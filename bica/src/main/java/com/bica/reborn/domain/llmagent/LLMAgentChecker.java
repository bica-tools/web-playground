package com.bica.reborn.domain.llmagent;

import com.bica.reborn.ast.SessionType;
import com.bica.reborn.globaltype.*;
import com.bica.reborn.lattice.LatticeChecker;
import com.bica.reborn.lattice.LatticeResult;
import com.bica.reborn.parser.Parser;
import com.bica.reborn.parser.PrettyPrinter;
import com.bica.reborn.statespace.StateSpace;
import com.bica.reborn.statespace.StateSpaceBuilder;

import java.util.*;

/**
 * LLM Agent Protocol Verification.
 *
 * <p>Models multi-agent LLM orchestration patterns as multiparty session
 * types (global types), projects onto individual agent roles, and verifies
 * that the resulting state spaces form lattices.
 *
 * <p>Ported from {@code reticulate/reticulate/llm_agents.py} (Step 82).
 */
public final class LLMAgentChecker {

    private LLMAgentChecker() {}

    // -----------------------------------------------------------------------
    // Data types
    // -----------------------------------------------------------------------

    /**
     * A multi-agent orchestration described by a global session type.
     */
    public record AgentOrchestration(
            String name,
            String description,
            List<String> agents,
            List<String[]> channels,
            GlobalType globalType) {

        public AgentOrchestration {
            Objects.requireNonNull(name);
            Objects.requireNonNull(globalType);
            agents = List.copyOf(agents);
            channels = List.copyOf(channels);
        }
    }

    /**
     * Result of projecting an orchestration onto a single role.
     */
    public record RoleProjection(
            String role,
            SessionType localType,
            String localTypeStr,
            boolean isWellDefined,
            int stateCount,
            boolean isLattice) {}

    /**
     * Complete verification result for a multi-agent orchestration.
     */
    public record OrchestrationResult(
            AgentOrchestration orchestration,
            String globalTypeStr,
            int globalStates,
            boolean globalIsLattice,
            LatticeResult latticeResult,
            List<RoleProjection> projections,
            boolean allProjectionsDefined,
            boolean allLocalLattices,
            boolean isVerified) {}

    // -----------------------------------------------------------------------
    // Helper: single-choice message
    // -----------------------------------------------------------------------

    private static GMessage msg(String sender, String receiver, String label, GlobalType cont) {
        return new GMessage(sender, receiver, List.of(new GMessage.Choice(label, cont)));
    }

    // -----------------------------------------------------------------------
    // Orchestration pattern constructors
    // -----------------------------------------------------------------------

    /** RAG (Retrieval-Augmented Generation) pipeline. */
    public static AgentOrchestration ragPipeline() {
        GlobalType gt = msg("user", "retriever", "query",
                msg("retriever", "ranker", "candidates",
                msg("ranker", "generator", "ranked",
                msg("generator", "validator", "draft",
                new GMessage("validator", "user", List.of(
                        new GMessage.Choice("APPROVED", new GEnd()),
                        new GMessage.Choice("REJECTED", new GEnd())
                ))))));

        return new AgentOrchestration(
                "RAG Pipeline",
                "Retrieval-Augmented Generation: user queries retriever, results ranked, "
                        + "generator produces draft, validator approves or rejects.",
                List.of("user", "retriever", "ranker", "generator", "validator"),
                List.of(new String[]{"user", "retriever"},
                        new String[]{"retriever", "ranker"},
                        new String[]{"ranker", "generator"},
                        new String[]{"generator", "validator"},
                        new String[]{"validator", "user"}),
                gt);
    }

    /** Agent-tool interaction loop with retry. */
    public static AgentOrchestration toolUseLoop() {
        GlobalType loopBody = msg("agent", "tool", "invoke",
                new GMessage("tool", "agent", List.of(
                        new GMessage.Choice("SUCCESS", new GEnd()),
                        new GMessage.Choice("ERROR", new GMessage("agent", "tool", List.of(
                                new GMessage.Choice("retry", new GVar("X")),
                                new GMessage.Choice("abort", new GEnd())
                        )))
                )));

        GlobalType gt = new GRec("X", loopBody);

        return new AgentOrchestration(
                "Tool Use Loop",
                "An agent invokes a tool in a loop. On success, done. "
                        + "On error, retry or abort.",
                List.of("agent", "tool"),
                List.of(new String[]{"agent", "tool"},
                        new String[]{"tool", "agent"}),
                gt);
    }

    /** Linear chain: agent1 -> agent2 -> ... -> agentN. */
    public static AgentOrchestration a2aChain(int numAgents) {
        if (numAgents < 2) throw new IllegalArgumentException("numAgents must be >= 2");

        List<String> agents = new ArrayList<>();
        for (int i = 1; i <= numAgents; i++) agents.add("agent" + i);

        List<String[]> channels = new ArrayList<>();
        for (int i = 0; i < agents.size() - 1; i++) {
            channels.add(new String[]{agents.get(i), agents.get(i + 1)});
            channels.add(new String[]{agents.get(i + 1), agents.get(i)});
        }

        // Build response chain backward
        GlobalType responses = buildResponses(agents, agents.size() - 2);
        // Build delegation chain forward
        GlobalType gt = buildDelegations(agents, 0, responses);

        return new AgentOrchestration(
                "A2A Chain (" + numAgents + " agents)",
                "A linear chain of " + numAgents + " agents. Each delegates to the next.",
                agents, channels, gt);
    }

    private static GlobalType buildResponses(List<String> agents, int idx) {
        if (idx < 0) return new GEnd();
        String sender = agents.get(idx + 1);
        String receiver = agents.get(idx);
        GlobalType cont = buildResponses(agents, idx - 1);
        return new GMessage(sender, receiver, List.of(
                new GMessage.Choice("DONE", cont),
                new GMessage.Choice("FAILED", cont)));
    }

    private static GlobalType buildDelegations(List<String> agents, int idx, GlobalType responses) {
        if (idx >= agents.size() - 1) return responses;
        return msg(agents.get(idx), agents.get(idx + 1), "delegate",
                buildDelegations(agents, idx + 1, responses));
    }

    /** One orchestrator broadcasts a task to N worker agents. */
    public static AgentOrchestration a2aBroadcast(int numWorkers) {
        if (numWorkers < 1) throw new IllegalArgumentException("numWorkers must be >= 1");

        List<String> agents = new ArrayList<>();
        agents.add("orchestrator");
        for (int i = 1; i <= numWorkers; i++) agents.add("worker" + i);

        List<String[]> channels = new ArrayList<>();
        for (int i = 1; i <= numWorkers; i++) {
            channels.add(new String[]{"orchestrator", "worker" + i});
            channels.add(new String[]{"worker" + i, "orchestrator"});
        }

        // Build worker branches
        List<GlobalType> branches = new ArrayList<>();
        for (int i = 1; i <= numWorkers; i++) {
            String w = "worker" + i;
            branches.add(msg("orchestrator", w, "assign",
                    new GMessage(w, "orchestrator", List.of(
                            new GMessage.Choice("DONE", new GEnd()),
                            new GMessage.Choice("FAILED", new GEnd())))));
        }

        GlobalType gt = branches.get(0);
        for (int i = 1; i < branches.size(); i++) {
            gt = new GParallel(gt, branches.get(i));
        }

        return new AgentOrchestration(
                "A2A Broadcast (" + numWorkers + " workers)",
                "An orchestrator broadcasts tasks to " + numWorkers + " workers in parallel.",
                agents, channels, gt);
    }

    // -----------------------------------------------------------------------
    // Verification
    // -----------------------------------------------------------------------

    /** Verify a multi-agent orchestration. */
    public static OrchestrationResult verifyOrchestration(AgentOrchestration orch) {
        // Global analysis
        StateSpace globalSs = GlobalTypeChecker.buildStateSpace(orch.globalType());
        LatticeResult globalLr = LatticeChecker.checkLattice(globalSs);
        String globalTypeStr = GlobalTypePrettyPrinter.pretty(orch.globalType());

        // Per-role projections
        Set<String> allRoles = GlobalTypeChecker.roles(orch.globalType());
        List<RoleProjection> projections = new ArrayList<>();
        boolean allDefined = true;
        boolean allLattices = true;

        for (String role : new TreeSet<>(allRoles)) {
            try {
                SessionType localType = Projection.project(orch.globalType(), role);
                String localStr = PrettyPrinter.pretty(localType);
                StateSpace localSs = StateSpaceBuilder.build(localType);
                LatticeResult localLr = LatticeChecker.checkLattice(localSs);
                projections.add(new RoleProjection(
                        role, localType, localStr, true,
                        localSs.states().size(), localLr.isLattice()));
                if (!localLr.isLattice()) allLattices = false;
            } catch (ProjectionError e) {
                allDefined = false;
                allLattices = false;
                projections.add(new RoleProjection(
                        role, null, "<projection failed>", false, 0, false));
            }
        }

        boolean isVerified = globalLr.isLattice() && allDefined && allLattices;

        return new OrchestrationResult(
                orch, globalTypeStr,
                globalSs.states().size(), globalLr.isLattice(), globalLr,
                projections, allDefined, allLattices, isVerified);
    }
}

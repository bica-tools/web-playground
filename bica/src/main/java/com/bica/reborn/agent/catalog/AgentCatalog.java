package com.bica.reborn.agent.catalog;

import com.bica.reborn.agent.AgentProtocol;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Predefined agent protocols from the research programme.
 *
 * <p>Contains session types for 16 agent roles used in the autonomous
 * research workflow. Each protocol is validated at class-load time.
 */
public final class AgentCatalog {

    private AgentCatalog() {}

    // -- Core Research Agents --

    public static final String RESEARCHER =
            "&{investigate: +{report: end}}";

    public static final String IMPLEMENTER =
            "rec X . &{implement: +{moduleReady: end, error: +{retry: X, abort: end}}}";

    public static final String TESTER =
            "rec X . &{writeTests: +{testsPass: end, testsFail: +{fix: X, abort: end}}}";

    public static final String WRITER =
            "&{writePaper: +{paperReady: end}}";

    public static final String PROVER =
            "&{writeProofs: +{proofsReady: end}}";

    public static final String EVALUATOR =
            "&{evaluate: +{accepted: end, needsFixes: +{listFixes: end}}}";

    public static final String REVIEWER =
            "&{review: +{fixes: end}}";

    public static final String SUPERVISOR =
            "&{scan: +{proposals: end}}";

    // -- Extended Agents --

    public static final String FORMALIZER =
            "&{formalize: +{formalized: end}}";

    public static final String BENCHMARKER =
            "&{benchmark: +{results: end}}";

    public static final String LITERATURE_SCOUT =
            "&{search: +{found: end, notFound: end}}";

    public static final String FACT_CHECKER =
            "&{check: +{verified: end, disputed: +{evidence: end}}}";

    public static final String PUBLICATION_PLANNER =
            "&{plan: +{schedule: end}}";

    public static final String VENUE_MATCHER =
            "&{match: +{venues: end}}";

    public static final String COPY_EDITOR =
            "&{edit: +{polished: end}}";

    public static final String DEPLOYER =
            "&{deploy: +{deployed: end, failed: +{rollback: end}}}";

    // -- Protocol Benchmarks --

    public static final String MCP_PROTOCOL =
            "&{initialize: rec X . &{callTool: +{RESULT: X, ERROR: X}, listTools: X, shutdown: end}}";

    public static final String A2A_PROTOCOL =
            "&{sendTask: rec X . +{WORKING: &{getStatus: X, cancel: end}, COMPLETED: &{getArtifact: end}, FAILED: end}}";

    /**
     * All predefined protocols as a name → session type map.
     */
    public static Map<String, String> all() {
        var map = new LinkedHashMap<String, String>();
        map.put("Researcher", RESEARCHER);
        map.put("Implementer", IMPLEMENTER);
        map.put("Tester", TESTER);
        map.put("Writer", WRITER);
        map.put("Prover", PROVER);
        map.put("Evaluator", EVALUATOR);
        map.put("Reviewer", REVIEWER);
        map.put("Supervisor", SUPERVISOR);
        map.put("Formalizer", FORMALIZER);
        map.put("Benchmarker", BENCHMARKER);
        map.put("LiteratureScout", LITERATURE_SCOUT);
        map.put("FactChecker", FACT_CHECKER);
        map.put("PublicationPlanner", PUBLICATION_PLANNER);
        map.put("VenueMatcher", VENUE_MATCHER);
        map.put("CopyEditor", COPY_EDITOR);
        map.put("Deployer", DEPLOYER);
        map.put("MCP", MCP_PROTOCOL);
        map.put("A2A", A2A_PROTOCOL);
        return Map.copyOf(map);
    }

    /**
     * Validate all catalog protocols. Returns the count of protocols that form lattices.
     */
    public static int validateAll() {
        int count = 0;
        for (var entry : all().entrySet()) {
            AgentProtocol p = AgentProtocol.of(entry.getValue());
            if (p.isLattice()) count++;
        }
        return count;
    }
}

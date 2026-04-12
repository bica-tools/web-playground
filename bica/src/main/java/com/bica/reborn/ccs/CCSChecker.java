package com.bica.reborn.ccs;

import com.bica.reborn.ast.*;
import com.bica.reborn.ast.Branch.Choice;
import com.bica.reborn.ccs.LTS.LTSTransition;
import com.bica.reborn.statespace.StateSpace;
import com.bica.reborn.statespace.StateSpaceBuilder;
import com.bica.reborn.statespace.TransitionKind;

import java.util.*;
import java.util.stream.Collectors;

/**
 * CCS (Calculus of Communicating Systems) checker for session types.
 *
 * <p>Provides:
 * <ul>
 *   <li>Translation from session type AST to CCS process</li>
 *   <li>LTS construction from CCS processes via structural operational semantics</li>
 *   <li>Strong bisimulation checking via partition refinement</li>
 *   <li>Pretty-printing of CCS processes</li>
 *   <li>Full analysis pipeline</li>
 * </ul>
 *
 * <p>Step 26 of the 1000 Steps Towards Session Types as Algebraic Reticulates.
 */
public final class CCSChecker {

    private CCSChecker() {}

    // -----------------------------------------------------------------------
    // Translation: Session Type AST -> CCS Process
    // -----------------------------------------------------------------------

    /**
     * Translate a session type AST to a CCS process.
     *
     * <p>Translation rules:
     * <ul>
     *   <li>{@code End} -> {@code Nil}</li>
     *   <li>{@code Var(X)} -> {@code CCSVar(X)}</li>
     *   <li>{@code Branch &{m1:S1,...}} -> {@code Sum(m1.[[S1]] + ...)}</li>
     *   <li>{@code Select +{l1:S1,...}} -> {@code Sum(tau_l1.[[S1]] + ...)} (internal choice)</li>
     *   <li>{@code Parallel(S1,S2)} -> {@code CCSPar([[S1]], [[S2]])}</li>
     *   <li>{@code Rec X . S} -> {@code CCSRec(X, [[S]])}</li>
     *   <li>{@code Sequence(L, R)} -> sequence L then R</li>
     * </ul>
     */
    public static CCSProcess sessionToCCS(SessionType ast) {
        if (ast instanceof End) {
            return new Nil();
        }
        if (ast instanceof Var v) {
            return new CCSVar(v.name());
        }
        if (ast instanceof Branch b) {
            List<CCSProcess> procs = new ArrayList<>();
            for (Choice c : b.choices()) {
                procs.add(new ActionPrefix(c.label(), sessionToCCS(c.body())));
            }
            if (procs.size() == 1) {
                return procs.get(0);
            }
            return new Sum(procs);
        }
        if (ast instanceof Select s) {
            List<CCSProcess> procs = new ArrayList<>();
            for (Choice c : s.choices()) {
                procs.add(new ActionPrefix("τ_" + c.label(), sessionToCCS(c.body())));
            }
            if (procs.size() == 1) {
                return procs.get(0);
            }
            return new Sum(procs);
        }
        if (ast instanceof Parallel p) {
            return new CCSPar(sessionToCCS(p.left()), sessionToCCS(p.right()));
        }
        if (ast instanceof Rec r) {
            return new CCSRec(r.var(), sessionToCCS(r.body()));
        }
        if (ast instanceof Sequence seq) {
            CCSProcess left = sessionToCCS(seq.left());
            CCSProcess right = sessionToCCS(seq.right());
            return sequenceWithSync(left, right);
        }
        throw new IllegalArgumentException("Unknown AST node: " + ast.getClass().getSimpleName());
    }

    /**
     * Replace all {@link Nil} leaves in {@code left} with
     * {@code ActionPrefix("tau_sync", right)}.
     */
    private static CCSProcess sequenceWithSync(CCSProcess left, CCSProcess right) {
        if (left instanceof Nil) {
            return new ActionPrefix("τ_sync", right);
        }
        if (left instanceof ActionPrefix ap) {
            return new ActionPrefix(ap.action(), sequenceWithSync(ap.cont(), right));
        }
        if (left instanceof Sum s) {
            List<CCSProcess> newProcs = new ArrayList<>();
            for (CCSProcess p : s.procs()) {
                newProcs.add(sequenceWithSync(p, right));
            }
            return new Sum(newProcs);
        }
        if (left instanceof CCSPar) {
            // Parallel: keep structure, LTS builder handles synchronisation
            return left;
        }
        if (left instanceof CCSRec r) {
            return new CCSRec(r.var(), sequenceWithSync(r.body(), right));
        }
        if (left instanceof Restrict r) {
            return new Restrict(sequenceWithSync(r.proc(), right), r.actions());
        }
        // CCSVar — cannot substitute further
        return left;
    }

    // -----------------------------------------------------------------------
    // CCS Process -> Labelled Transition System
    // -----------------------------------------------------------------------

    /**
     * Build a Labelled Transition System from a CCS process via SOS rules.
     *
     * @param proc      the CCS process to explore
     * @param maxStates safety bound on state count
     * @return the LTS
     */
    public static LTS ccsToLTS(CCSProcess proc, int maxStates) {
        return new LTSBuilder(maxStates).build(proc);
    }

    /** Overload with default max states of 500. */
    public static LTS ccsToLTS(CCSProcess proc) {
        return ccsToLTS(proc, 500);
    }

    private static final class LTSBuilder {

        private int nextId = 0;
        private final Map<Integer, String> stateLabels = new LinkedHashMap<>();
        private final List<LTSTransition> transitions = new ArrayList<>();
        private final Map<CCSProcess, Integer> procToId = new HashMap<>();
        private final int maxStates;

        LTSBuilder(int maxStates) {
            this.maxStates = maxStates;
        }

        private int fresh(String label) {
            int sid = nextId++;
            stateLabels.put(sid, label);
            return sid;
        }

        private int getOrCreate(CCSProcess proc) {
            proc = normalize(proc);
            Integer existing = procToId.get(proc);
            if (existing != null) {
                return existing;
            }
            int sid = fresh(shortLabel(proc));
            procToId.put(proc, sid);
            return sid;
        }

        LTS build(CCSProcess proc) {
            proc = normalize(proc);
            int initial = getOrCreate(proc);

            Deque<CCSProcess> worklist = new ArrayDeque<>();
            Set<CCSProcess> visited = new HashSet<>();
            worklist.add(proc);

            while (!worklist.isEmpty()) {
                CCSProcess current = worklist.poll();
                if (!visited.add(current)) {
                    continue;
                }
                if (nextId > maxStates) {
                    break;
                }

                int src = procToId.get(current);
                List<Map.Entry<String, CCSProcess>> steps = sosStep(current);

                for (var step : steps) {
                    String action = step.getKey();
                    CCSProcess target = normalize(step.getValue());
                    int tgtId = getOrCreate(target);
                    transitions.add(new LTSTransition(src, action, tgtId));
                    if (!visited.contains(target)) {
                        worklist.add(target);
                    }
                }
            }

            return new LTS(
                    Set.copyOf(stateLabels.keySet()),
                    List.copyOf(transitions),
                    initial,
                    Map.copyOf(stateLabels));
        }
    }

    // -----------------------------------------------------------------------
    // Structural Operational Semantics
    // -----------------------------------------------------------------------

    /**
     * Compute one-step transitions from a CCS process via SOS rules.
     *
     * @return list of (action, successor) pairs
     */
    static List<Map.Entry<String, CCSProcess>> sosStep(CCSProcess proc) {
        if (proc instanceof Nil || proc instanceof CCSVar) {
            return List.of();
        }
        if (proc instanceof ActionPrefix ap) {
            return List.of(Map.entry(ap.action(), ap.cont()));
        }
        if (proc instanceof Sum s) {
            var result = new ArrayList<Map.Entry<String, CCSProcess>>();
            for (CCSProcess p : s.procs()) {
                result.addAll(sosStep(p));
            }
            return result;
        }
        if (proc instanceof CCSPar par) {
            var result = new ArrayList<Map.Entry<String, CCSProcess>>();
            // Left steps independently
            for (var step : sosStep(par.left())) {
                result.add(Map.entry(step.getKey(),
                        new CCSPar(step.getValue(), par.right())));
            }
            // Right steps independently
            for (var step : sosStep(par.right())) {
                result.add(Map.entry(step.getKey(),
                        new CCSPar(par.left(), step.getValue())));
            }
            return result;
        }
        if (proc instanceof Restrict r) {
            var result = new ArrayList<Map.Entry<String, CCSProcess>>();
            for (var step : sosStep(r.proc())) {
                if (!r.actions().contains(step.getKey())) {
                    result.add(Map.entry(step.getKey(),
                            new Restrict(step.getValue(), r.actions())));
                }
            }
            return result;
        }
        if (proc instanceof CCSRec rec) {
            CCSProcess unfolded = unfoldRec(rec);
            return sosStep(unfolded);
        }
        return List.of();
    }

    // -----------------------------------------------------------------------
    // Normalization and substitution
    // -----------------------------------------------------------------------

    /** Normalize: unfold top-level CCSRec, flatten nested Sums. */
    static CCSProcess normalize(CCSProcess proc) {
        while (proc instanceof CCSRec rec) {
            proc = unfoldRec(rec);
        }
        if (proc instanceof Sum s) {
            List<CCSProcess> flat = flattenSum(s);
            if (flat.size() == 1) {
                return normalize(flat.get(0));
            }
            proc = new Sum(flat);
        }
        return proc;
    }

    /** Unfold {@code fix X . P} by substituting {@code fix X . P} for X in P. */
    static CCSProcess unfoldRec(CCSRec rec) {
        return subst(rec.body(), rec.var(), rec);
    }

    /** Substitute {@code replacement} for {@code var} in {@code proc}. */
    static CCSProcess subst(CCSProcess proc, String var, CCSProcess replacement) {
        if (proc instanceof Nil) {
            return proc;
        }
        if (proc instanceof CCSVar v) {
            return v.name().equals(var) ? replacement : proc;
        }
        if (proc instanceof ActionPrefix ap) {
            return new ActionPrefix(ap.action(), subst(ap.cont(), var, replacement));
        }
        if (proc instanceof Sum s) {
            return new Sum(s.procs().stream()
                    .map(p -> subst(p, var, replacement))
                    .toList());
        }
        if (proc instanceof CCSPar p) {
            return new CCSPar(subst(p.left(), var, replacement),
                    subst(p.right(), var, replacement));
        }
        if (proc instanceof Restrict r) {
            return new Restrict(subst(r.proc(), var, replacement), r.actions());
        }
        if (proc instanceof CCSRec r) {
            if (r.var().equals(var)) {
                return proc; // shadowed
            }
            return new CCSRec(r.var(), subst(r.body(), var, replacement));
        }
        return proc;
    }

    /** Flatten nested Sum nodes. */
    static List<CCSProcess> flattenSum(Sum s) {
        var result = new ArrayList<CCSProcess>();
        for (CCSProcess p : s.procs()) {
            if (p instanceof Sum inner) {
                result.addAll(flattenSum(inner));
            } else {
                result.add(p);
            }
        }
        return result;
    }

    // -----------------------------------------------------------------------
    // Strong bisimulation (partition refinement)
    // -----------------------------------------------------------------------

    /**
     * Check strong bisimulation between two LTS via partition refinement.
     *
     * <p>Combines both LTS into a single transition system, then iteratively
     * refines partitions until stable. The initial states are bisimilar iff
     * they end up in the same equivalence class.
     */
    public static boolean checkBisimulation(LTS lts1, LTS lts2) {
        if (lts1.states().isEmpty() || lts2.states().isEmpty()) {
            return lts1.states().isEmpty() && lts2.states().isEmpty();
        }

        // Offset lts2 state IDs
        int offset = lts1.states().stream().mapToInt(Integer::intValue).max().orElse(-1) + 1;

        // Combined transitions
        var combined = new ArrayList<LTSTransition>();
        combined.addAll(lts1.transitions());
        for (var t : lts2.transitions()) {
            combined.add(new LTSTransition(t.source() + offset, t.label(), t.target() + offset));
        }

        // All states
        Set<Integer> allStates = new HashSet<>(lts1.states());
        for (int s : lts2.states()) {
            allStates.add(s + offset);
        }

        // All actions
        Set<String> allActions = new HashSet<>();
        for (var t : combined) {
            allActions.add(t.label());
        }

        // Predecessor map: action -> target -> set of sources
        Map<String, Map<Integer, Set<Integer>>> pred = new HashMap<>();
        for (String a : allActions) {
            pred.put(a, new HashMap<>());
        }
        for (var t : combined) {
            pred.get(t.label()).computeIfAbsent(t.target(), k -> new HashSet<>()).add(t.source());
        }

        // Build outgoing set for fast terminal check
        Set<Integer> hasOutgoing = new HashSet<>();
        for (var t : combined) {
            hasOutgoing.add(t.source());
        }

        // Initial partition: terminal vs non-terminal
        Set<Integer> terminal = new HashSet<>();
        Set<Integer> nonTerminal = new HashSet<>();
        for (int s : allStates) {
            if (hasOutgoing.contains(s)) {
                nonTerminal.add(s);
            } else {
                terminal.add(s);
            }
        }

        List<Set<Integer>> partition = new ArrayList<>();
        if (!terminal.isEmpty()) partition.add(terminal);
        if (!nonTerminal.isEmpty()) partition.add(nonTerminal);
        if (partition.isEmpty()) partition.add(new HashSet<>(allStates));

        // Partition refinement
        boolean changed = true;
        while (changed) {
            changed = false;
            List<Set<Integer>> newPartition = new ArrayList<>();
            for (Set<Integer> block : partition) {
                if (block.size() <= 1) {
                    newPartition.add(block);
                    continue;
                }
                List<Set<Integer>> split = splitBlock(block, partition, allActions, pred);
                if (split.size() > 1) {
                    changed = true;
                    newPartition.addAll(split);
                } else {
                    newPartition.add(block);
                }
            }
            partition = newPartition;
        }

        // Check if initial states are in the same block
        int init1 = lts1.initial();
        int init2 = lts2.initial() + offset;
        for (Set<Integer> block : partition) {
            if (block.contains(init1) && block.contains(init2)) {
                return true;
            }
        }
        return false;
    }

    private static List<Set<Integer>> splitBlock(
            Set<Integer> block,
            List<Set<Integer>> partition,
            Set<String> actions,
            Map<String, Map<Integer, Set<Integer>>> pred) {

        for (String action : actions) {
            for (Set<Integer> splitter : partition) {
                Set<Integer> canReach = new HashSet<>();
                Map<Integer, Set<Integer>> actionPred = pred.get(action);
                for (int t : splitter) {
                    Set<Integer> sources = actionPred.get(t);
                    if (sources != null) {
                        for (int s : sources) {
                            if (block.contains(s)) {
                                canReach.add(s);
                            }
                        }
                    }
                }
                Set<Integer> cannotReach = new HashSet<>(block);
                cannotReach.removeAll(canReach);
                if (!canReach.isEmpty() && !cannotReach.isEmpty()) {
                    return List.of(canReach, cannotReach);
                }
            }
        }
        return List.of(block);
    }

    // -----------------------------------------------------------------------
    // Pretty-printer
    // -----------------------------------------------------------------------

    /**
     * Pretty-print a CCS process in standard notation.
     *
     * <ul>
     *   <li>{@code 0} for Nil</li>
     *   <li>{@code a . P} for ActionPrefix</li>
     *   <li>{@code P + Q} for Sum</li>
     *   <li>{@code P | Q} for CCSPar</li>
     *   <li>{@code P \ {a, b}} for Restrict</li>
     *   <li>{@code fix X . P} for CCSRec</li>
     *   <li>{@code X} for CCSVar</li>
     * </ul>
     */
    public static String prettyCCS(CCSProcess proc) {
        return prettyCCS(proc, 0);
    }

    /**
     * Precedence levels: 0=Sum, 1=Par, 2=Prefix, 3=Atom.
     */
    private static String prettyCCS(CCSProcess proc, int prec) {
        if (proc instanceof Nil) {
            return "0";
        }
        if (proc instanceof CCSVar v) {
            return v.name();
        }
        if (proc instanceof ActionPrefix ap) {
            String s = ap.action() + " . " + prettyCCS(ap.cont(), 2);
            return prec > 2 ? "(" + s + ")" : s;
        }
        if (proc instanceof Sum sum) {
            String s = sum.procs().stream()
                    .map(p -> prettyCCS(p, 1))
                    .collect(Collectors.joining(" + "));
            return prec > 0 ? "(" + s + ")" : s;
        }
        if (proc instanceof CCSPar par) {
            String s = prettyCCS(par.left(), 2) + " | " + prettyCCS(par.right(), 2);
            return prec > 1 ? "(" + s + ")" : s;
        }
        if (proc instanceof Restrict r) {
            String acts = r.actions().stream().sorted().collect(Collectors.joining(", "));
            return prettyCCS(r.proc(), 3) + " \\ {" + acts + "}";
        }
        if (proc instanceof CCSRec rec) {
            String s = "fix " + rec.var() + " . " + prettyCCS(rec.body(), 0);
            return prec > 0 ? "(" + s + ")" : s;
        }
        return "?";
    }

    // -----------------------------------------------------------------------
    // Bisimulation with session-type state space
    // -----------------------------------------------------------------------

    /**
     * Check if an LTS is strongly bisimilar to a session-type state space.
     * Converts the state space to an LTS (using tau_ prefix for selections)
     * and delegates to {@link #checkBisimulation(LTS, LTS)}.
     */
    public static boolean checkBisimulationWithStateSpace(LTS lts, StateSpace ss) {
        LTS ssLts = stateSpaceToLTS(ss);
        return checkBisimulation(lts, ssLts);
    }

    /** Convert a session-type {@link StateSpace} to an {@link LTS}. */
    static LTS stateSpaceToLTS(StateSpace ss) {
        var transitions = new ArrayList<LTSTransition>();
        for (var t : ss.transitions()) {
            String action = (t.kind() == TransitionKind.SELECTION)
                    ? "τ_" + t.label()
                    : t.label();
            transitions.add(new LTSTransition(t.source(), action, t.target()));
        }
        var labels = new HashMap<Integer, String>();
        for (int s : ss.states()) {
            labels.put(s, ss.labels().getOrDefault(s, String.valueOf(s)));
        }
        return new LTS(ss.states(), transitions, ss.top(), labels);
    }

    // -----------------------------------------------------------------------
    // Short labels for LTS display
    // -----------------------------------------------------------------------

    private static String shortLabel(CCSProcess proc) {
        if (proc instanceof Nil) return "0";
        if (proc instanceof ActionPrefix ap) return ap.action() + ".…";
        if (proc instanceof Sum s) {
            var parts = s.procs().stream().limit(3)
                    .map(CCSChecker::shortLabel).toList();
            String joined = String.join(" + ", parts);
            return s.procs().size() > 3 ? joined + " + …" : joined;
        }
        if (proc instanceof CCSPar p) return shortLabel(p.left()) + " | " + shortLabel(p.right());
        if (proc instanceof Restrict r) return shortLabel(r.proc()) + "\\L";
        if (proc instanceof CCSRec r) return "fix " + r.var();
        if (proc instanceof CCSVar v) return v.name();
        return "?";
    }

    // -----------------------------------------------------------------------
    // High-level analysis
    // -----------------------------------------------------------------------

    /**
     * Full CCS analysis pipeline for a session type AST.
     *
     * <ol>
     *   <li>Translate AST to CCS process</li>
     *   <li>Build LTS from CCS process</li>
     *   <li>Build state space from AST</li>
     *   <li>Check bisimulation between LTS and state space</li>
     * </ol>
     */
    public static CCSResult analyzeCCS(SessionType ast) {
        CCSProcess ccsProc = sessionToCCS(ast);
        LTS lts = ccsToLTS(ccsProc);
        StateSpace ss = StateSpaceBuilder.build(ast);
        boolean bisimilar = checkBisimulationWithStateSpace(lts, ss);

        return new CCSResult(
                ccsProc,
                lts,
                lts.states().size(),
                lts.transitions().size(),
                bisimilar);
    }
}

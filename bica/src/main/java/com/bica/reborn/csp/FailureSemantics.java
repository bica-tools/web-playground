package com.bica.reborn.csp;

import com.bica.reborn.lattice.LatticeChecker;
import com.bica.reborn.statespace.StateSpace;

import java.util.*;
import java.util.stream.Collectors;

/**
 * Failure semantics derived from lattice structure (Step 28 port).
 *
 * <p>In a session type lattice, the meet of two states determines their
 * shared refusals, and the join determines their shared acceptances.
 * Failure refinement corresponds to lattice embedding.
 */
public final class FailureSemantics {

    private FailureSemantics() {}

    /**
     * Full failure analysis result.
     */
    public record FailureAnalysis(
            Set<Failure> failurePairs,
            Map<Integer, Set<String>> refusalMap,
            Map<Integer, Set<String>> acceptanceMap,
            boolean deterministic,
            boolean latticeDetermines,
            String latticeDetails) {}

    /**
     * Failure refinement result.
     */
    public record RefinementResult(
            boolean refines,
            Failure witness,
            int ss1Failures,
            int ss2Failures) {}

    // =========================================================================
    // Refusals and acceptances
    // =========================================================================

    /**
     * Compute the refusal set at a given state.
     */
    public static Set<String> computeRefusals(StateSpace ss, int state) {
        return CSPChecker.computeRefusals(ss, state);
    }

    /**
     * Compute all refusals for all states.
     */
    public static Map<Integer, Set<String>> computeAllRefusals(StateSpace ss) {
        return CSPChecker.computeAllRefusals(ss);
    }

    // =========================================================================
    // Failure pairs
    // =========================================================================

    /**
     * Compute all (trace, refusal_set) failure pairs.
     */
    public static Set<Failure> failurePairs(StateSpace ss, int maxDepth) {
        return CSPChecker.failures(ss, maxDepth);
    }

    /**
     * Compute all failure pairs with default depth.
     */
    public static Set<Failure> failurePairs(StateSpace ss) {
        return failurePairs(ss, 20);
    }

    // =========================================================================
    // Failure refinement
    // =========================================================================

    /**
     * Check whether ss1 failure-refines ss2.
     */
    public static RefinementResult checkFailureRefinement(StateSpace ss1, StateSpace ss2, int maxDepth) {
        Set<Failure> fp1 = failurePairs(ss1, maxDepth);
        Set<Failure> fp2 = failurePairs(ss2, maxDepth);

        Set<String> common = new HashSet<>(CSPChecker.alphabet(ss1));
        common.retainAll(CSPChecker.alphabet(ss2));

        // Build lookup for ss2 failures by trace
        Map<List<String>, Set<Set<String>>> fp2ByTrace = new HashMap<>();
        for (Failure f : fp2) {
            Set<String> normRef = new HashSet<>(f.refusals());
            normRef.retainAll(common);
            fp2ByTrace.computeIfAbsent(f.trace(), k -> new HashSet<>()).add(normRef);
        }

        Failure witness = null;
        for (Failure f : fp1) {
            Set<String> refCommon = new HashSet<>(f.refusals());
            refCommon.retainAll(common);

            Set<Set<String>> ss2Refs = fp2ByTrace.get(f.trace());
            if (ss2Refs == null) continue; // ss2 can't execute this trace

            boolean found = ss2Refs.stream().anyMatch(r2 -> r2.containsAll(refCommon));
            if (!found) {
                witness = f;
                break;
            }
        }

        return new RefinementResult(witness == null, witness, fp1.size(), fp2.size());
    }

    /**
     * Check failure refinement with default depth.
     */
    public static RefinementResult checkFailureRefinement(StateSpace ss1, StateSpace ss2) {
        return checkFailureRefinement(ss1, ss2, 20);
    }

    // =========================================================================
    // Lattice determines failures
    // =========================================================================

    /**
     * Verify that the lattice ordering determines the failure structure.
     */
    public static boolean latticeDeterminesFailures(StateSpace ss) {
        return CSPChecker.latticeDeterminesFailures(ss);
    }

    // =========================================================================
    // Must / May testing
    // =========================================================================

    /**
     * After executing trace, what labels MUST be accepted?
     */
    public static Set<String> mustTesting(StateSpace ss, List<String> trace) {
        return CSPChecker.mustTesting(ss, trace);
    }

    /**
     * After executing trace, what labels MAY be accepted?
     */
    public static Set<String> mayTesting(StateSpace ss, List<String> trace) {
        return CSPChecker.mayTesting(ss, trace);
    }

    // =========================================================================
    // Full analysis
    // =========================================================================

    /**
     * Perform complete failure analysis on a state space.
     */
    public static FailureAnalysis analyzeFailures(StateSpace ss, int maxDepth) {
        Map<Integer, Set<String>> refusalMap = computeAllRefusals(ss);
        Map<Integer, Set<String>> acceptanceMap = new HashMap<>();
        for (int state : ss.states()) {
            acceptanceMap.put(state, ss.enabledLabels(state));
        }
        boolean deterministic = CSPChecker.isDeterministic(ss);
        boolean latDet = latticeDeterminesFailures(ss);
        Set<Failure> fp = failurePairs(ss, maxDepth);

        String details;
        if (latDet) {
            details = "Lattice meet/join fully determine failure structure.";
        } else {
            var lr = LatticeChecker.checkLattice(ss);
            if (!lr.isLattice()) {
                details = "State space is not a lattice; lattice-determines check not applicable.";
            } else {
                details = "Lattice meet/join do not fully determine failure structure.";
            }
        }

        return new FailureAnalysis(fp, refusalMap, acceptanceMap, deterministic, latDet, details);
    }

    /**
     * Perform complete failure analysis with default depth.
     */
    public static FailureAnalysis analyzeFailures(StateSpace ss) {
        return analyzeFailures(ss, 20);
    }
}

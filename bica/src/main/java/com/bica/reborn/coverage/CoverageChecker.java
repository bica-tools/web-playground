package com.bica.reborn.coverage;

import com.bica.reborn.statespace.StateSpace;
import com.bica.reborn.statespace.StateSpace.Transition;
import com.bica.reborn.testgen.PathEnumerator.EnumerationResult;
import com.bica.reborn.testgen.PathEnumerator.IncompletePrefix;
import com.bica.reborn.testgen.PathEnumerator.Step;
import com.bica.reborn.testgen.PathEnumerator.ValidPath;
import com.bica.reborn.testgen.PathEnumerator.ViolationPoint;

import java.util.*;

/**
 * Computes which transitions and states in a state space are exercised
 * by generated test paths.
 */
public final class CoverageChecker {

    private CoverageChecker() {}

    /**
     * Compute coverage from an enumeration result.
     */
    public static CoverageResult computeCoverage(
            StateSpace ss, EnumerationResult result) {
        return computeCoverage(ss,
                result.validPaths(),
                result.violations(),
                result.incompletePrefixes());
    }

    /**
     * Compute transition and state coverage over the state space.
     */
    public static CoverageResult computeCoverage(
            StateSpace ss,
            List<ValidPath> paths,
            List<ViolationPoint> violations,
            List<IncompletePrefix> incompletePrefixes) {

        Set<Transition> allTransitions = new HashSet<>(ss.transitions());

        if (allTransitions.isEmpty()) {
            return new CoverageResult(
                    Set.of(), Set.of(),
                    Set.copyOf(ss.states()), Set.of(),
                    1.0, 1.0);
        }

        Set<Transition> covered = new HashSet<>();

        // Valid paths
        if (paths != null) {
            for (ValidPath p : paths) {
                collectTransitions(ss.top(), p.steps(), allTransitions, covered);
            }
        }

        // Violations
        if (violations != null) {
            for (ViolationPoint v : violations) {
                collectTransitions(ss.top(), v.prefixPath(), allTransitions, covered);
            }
        }

        // Incomplete prefixes
        if (incompletePrefixes != null) {
            for (IncompletePrefix ip : incompletePrefixes) {
                collectTransitions(ss.top(), ip.steps(), allTransitions, covered);
            }
        }

        Set<Transition> uncovered = new HashSet<>(allTransitions);
        uncovered.removeAll(covered);

        Set<Integer> coveredStates = new HashSet<>();
        for (Transition t : covered) {
            coveredStates.add(t.source());
            coveredStates.add(t.target());
        }
        if (!covered.isEmpty()) {
            coveredStates.add(ss.top());
        }

        Set<Integer> uncoveredStates = new HashSet<>(ss.states());
        uncoveredStates.removeAll(coveredStates);

        double transCov = (double) covered.size() / allTransitions.size();
        double stateCov = (double) coveredStates.size() / ss.states().size();

        return new CoverageResult(
                covered, uncovered,
                coveredStates, uncoveredStates,
                transCov, stateCov);
    }

    private static void collectTransitions(
            int top,
            List<Step> steps,
            Set<Transition> allTransitions,
            Set<Transition> covered) {
        int current = top;
        for (Step step : steps) {
            for (Transition t : allTransitions) {
                if (t.source() == current && t.label().equals(step.label())
                        && t.target() == step.target()) {
                    covered.add(t);
                    break;
                }
            }
            current = step.target();
        }
    }
}

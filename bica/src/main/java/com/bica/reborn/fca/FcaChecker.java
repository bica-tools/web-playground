package com.bica.reborn.fca;

import com.bica.reborn.statespace.StateSpace;

import java.util.*;
import java.util.stream.Collectors;

/**
 * Formal Concept Analysis for session type lattices (Step 30aa).
 *
 * <p>Formal Concept Analysis (FCA) constructs a concept lattice from a
 * binary relation (formal context) between objects and attributes.
 * For session type state spaces:
 * <ul>
 *   <li><b>Objects</b> = states in the state space</li>
 *   <li><b>Attributes</b> = transition labels (methods) available in the protocol</li>
 *   <li><b>Incidence</b> = state s has attribute m iff m is enabled at s</li>
 * </ul>
 */
public final class FcaChecker {

    private FcaChecker() {}

    // =========================================================================
    // Context construction
    // =========================================================================

    /**
     * Extract the formal context from a session type state space.
     *
     * <p>Objects are states, attributes are transition labels.
     * State s has attribute m iff there is a transition (s, m, _) in ss.
     */
    public static FormalContext formalContext(StateSpace ss) {
        var objects = new ArrayList<>(ss.states());
        Collections.sort(objects);

        var allLabels = new TreeSet<String>();
        for (var t : ss.transitions()) {
            allLabels.add(t.label());
        }
        var attributes = new ArrayList<>(allLabels);

        var incidence = new LinkedHashMap<Integer, Set<String>>();
        for (int s : objects) {
            var labels = new HashSet<String>();
            for (var t : ss.transitions()) {
                if (t.source() == s) {
                    labels.add(t.label());
                }
            }
            incidence.put(s, labels);
        }

        return new FormalContext(objects, attributes, incidence);
    }

    // =========================================================================
    // Derivation operators
    // =========================================================================

    /**
     * Compute the extent of a set of attributes.
     * Returns the set of objects that have ALL the given attributes.
     */
    public static Set<Integer> extent(Set<String> attrs, FormalContext context) {
        if (attrs.isEmpty()) {
            return new HashSet<>(context.objects());
        }
        var result = new HashSet<Integer>();
        for (int obj : context.objects()) {
            if (context.incidence().get(obj).containsAll(attrs)) {
                result.add(obj);
            }
        }
        return result;
    }

    /**
     * Compute the intent of a set of objects.
     * Returns the set of attributes shared by ALL the given objects.
     */
    public static Set<String> intent(Set<Integer> objs, FormalContext context) {
        if (objs.isEmpty()) {
            return new HashSet<>(context.attributes());
        }
        Set<String> result = null;
        for (int obj : objs) {
            var objAttrs = context.incidence().getOrDefault(obj, Set.of());
            if (result == null) {
                result = new HashSet<>(objAttrs);
            } else {
                result.retainAll(objAttrs);
            }
        }
        return result != null ? result : new HashSet<>();
    }

    /**
     * Close a set of objects to a formal concept.
     * Computes (A'', A') where A'' = extent(intent(A)) and A' = intent(A).
     */
    public static FormalConcept conceptClosure(Set<Integer> objs, FormalContext context) {
        var b = intent(objs, context);
        var a = extent(b, context);
        return new FormalConcept(a, b);
    }

    /**
     * Close a set of attributes to a formal concept.
     * Computes (B', B'') where B' = extent(attrs) and B'' = intent(extent(attrs)).
     */
    public static FormalConcept attributeClosure(Set<String> attrs, FormalContext context) {
        var a = extent(attrs, context);
        var b = intent(a, context);
        return new FormalConcept(a, b);
    }

    // =========================================================================
    // Concept enumeration
    // =========================================================================

    /**
     * Enumerate all formal concepts using the basic closure algorithm.
     *
     * <p>Iterates over attribute subsets (up to 20 attributes) or object subsets
     * (fallback), computing closures and collecting unique concepts.
     */
    public static List<FormalConcept> allConcepts(FormalContext context) {
        var seen = new HashSet<FormalConcept>();
        var concepts = new ArrayList<FormalConcept>();

        int nAttrs = context.attributes().size();
        if (nAttrs <= 20) {
            for (long mask = 0; mask < (1L << nAttrs); mask++) {
                var attrs = new HashSet<String>();
                for (int i = 0; i < nAttrs; i++) {
                    if ((mask & (1L << i)) != 0) {
                        attrs.add(context.attributes().get(i));
                    }
                }
                var concept = attributeClosure(attrs, context);
                if (seen.add(concept)) {
                    concepts.add(concept);
                }
            }
        } else {
            int nObjs = context.objects().size();
            for (long mask = 0; mask < (1L << nObjs); mask++) {
                var objs = new HashSet<Integer>();
                for (int i = 0; i < nObjs; i++) {
                    if ((mask & (1L << i)) != 0) {
                        objs.add(context.objects().get(i));
                    }
                }
                var concept = conceptClosure(objs, context);
                if (seen.add(concept)) {
                    concepts.add(concept);
                }
            }
        }

        // Sort by extent size descending, then by sorted extent for consistency
        concepts.sort((a, b) -> {
            int cmp = Integer.compare(b.extent().size(), a.extent().size());
            if (cmp != 0) return cmp;
            var sortedA = new ArrayList<>(a.extent());
            var sortedB = new ArrayList<>(b.extent());
            Collections.sort(sortedA);
            Collections.sort(sortedB);
            for (int i = 0; i < Math.min(sortedA.size(), sortedB.size()); i++) {
                int c = Integer.compare(sortedA.get(i), sortedB.get(i));
                if (c != 0) return c;
            }
            return Integer.compare(sortedA.size(), sortedB.size());
        });

        return concepts;
    }

    // =========================================================================
    // Concept lattice construction
    // =========================================================================

    /**
     * Build the concept lattice from a formal context.
     * The ordering is by extent inclusion: (A1, B1) &le; (A2, B2) iff A1 &sube; A2.
     */
    public static ConceptLattice conceptLattice(FormalContext context) {
        var concepts = allConcepts(context);
        int n = concepts.size();

        var order = new ArrayList<int[]>();
        for (int i = 0; i < n; i++) {
            for (int j = 0; j < n; j++) {
                if (i != j && concepts.get(j).extent().containsAll(concepts.get(i).extent())) {
                    order.add(new int[]{i, j});
                }
            }
        }

        int topIdx = 0;
        int bottomIdx = 0;
        for (int i = 1; i < n; i++) {
            if (concepts.get(i).extent().size() > concepts.get(topIdx).extent().size()) {
                topIdx = i;
            }
            if (concepts.get(i).extent().size() < concepts.get(bottomIdx).extent().size()) {
                bottomIdx = i;
            }
        }

        return new ConceptLattice(concepts, order, topIdx, bottomIdx);
    }

    // =========================================================================
    // Context properties
    // =========================================================================

    /**
     * Check if the context is clarified.
     * A context is clarified iff no two objects have the same intent
     * and no two attributes have the same extent.
     */
    public static boolean isClarified(FormalContext context) {
        // Check objects: no two should have the same attribute set
        var seenIntents = new HashSet<Set<String>>();
        for (int obj : context.objects()) {
            var attrs = context.incidence().get(obj);
            if (!seenIntents.add(attrs)) {
                return false;
            }
        }

        // Check attributes: no two should have the same object set
        var seenExtents = new HashSet<Set<Integer>>();
        for (var attr : context.attributes()) {
            var objs = new HashSet<Integer>();
            for (int o : context.objects()) {
                if (context.incidence().get(o).contains(attr)) {
                    objs.add(o);
                }
            }
            if (!seenExtents.add(objs)) {
                return false;
            }
        }

        return true;
    }

    /**
     * Compute the density of the incidence relation.
     * Returns |I| / (|G| x |M|), the fraction of filled cells.
     */
    public static double contextDensity(FormalContext context) {
        int nObjs = context.objects().size();
        int nAttrs = context.attributes().size();
        if (nObjs == 0 || nAttrs == 0) {
            return 0.0;
        }
        int totalOnes = 0;
        for (var attrs : context.incidence().values()) {
            totalOnes += attrs.size();
        }
        return (double) totalOnes / ((double) nObjs * nAttrs);
    }

    // =========================================================================
    // Full analysis
    // =========================================================================

    /**
     * Perform complete FCA analysis on a session type state space.
     */
    public static FCAResult analyzeFca(StateSpace ss) {
        var ctx = formalContext(ss);
        var lattice = conceptLattice(ctx);
        int nConcepts = lattice.concepts().size();
        int nStates = ss.states().size();

        return new FCAResult(
                ctx,
                ctx.objects().size(),
                ctx.attributes().size(),
                nConcepts,
                lattice,
                contextDensity(ctx),
                isClarified(ctx),
                nStates,
                nStates > 0 ? (double) nConcepts / nStates : 0.0
        );
    }
}

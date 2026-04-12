package com.bica.reborn.char_poly;

import com.bica.reborn.algebraic.zeta.Zeta;
import com.bica.reborn.characteristic.CharacteristicChecker;
import com.bica.reborn.statespace.StateSpace;

import java.util.*;

/**
 * Characteristic polynomial via deletion-contraction (Step 32c).
 *
 * <p>Extends the Moebius-based characteristic polynomial from CharacteristicChecker
 * with the deletion-contraction algorithm. For a lattice L with covering
 * relation (Hasse diagram), the chromatic polynomial can be computed
 * recursively:
 *
 * <pre>    p(L, t) = p(L \ e, t) - p(L / e, t)</pre>
 *
 * <p>where L \ e is deletion (remove edge e) and L / e is contraction
 * (identify the endpoints of edge e).
 *
 * <p>Faithful Java port of Python {@code reticulate.char_poly}.
 */
public final class CharPolyChecker {

    private CharPolyChecker() {}

    // =========================================================================
    // Result types
    // =========================================================================

    /** Deletion-contraction characteristic polynomial analysis. */
    public record CharPolyDCResult(
            List<Integer> coefficientsDc,
            List<Integer> coefficientsMobius,
            boolean match,
            int degree,
            int evalAt0,
            int evalAt1,
            int evalAtNeg1,
            int chromaticBound,
            int numEdges,
            int numStates) {}

    // =========================================================================
    // Hasse diagram operations
    // =========================================================================

    /**
     * Extract the quotient Hasse diagram as (nodes, edges).
     * Returns nodes as sorted list and edges as (upper, lower) pairs.
     */
    public static List<int[]> hasseGraph(StateSpace ss) {
        Zeta.SccResult sccR = Zeta.computeSccs(ss);
        Map<Integer, Integer> sccMap = sccR.sccMap();
        Map<Integer, Set<Integer>> sccMembers = sccR.sccMembers();
        List<Zeta.Pair> covers = Zeta.coveringRelation(ss);

        List<Integer> reps = new ArrayList<>(sccMembers.keySet());
        Collections.sort(reps);

        Set<String> qEdgesSet = new HashSet<>();
        List<int[]> qEdges = new ArrayList<>();
        for (Zeta.Pair c : covers) {
            int rx = sccMap.get(c.x());
            int ry = sccMap.get(c.y());
            if (rx != ry) {
                String key = rx + "," + ry;
                if (qEdgesSet.add(key)) {
                    qEdges.add(new int[]{rx, ry});
                }
            }
        }
        qEdges.sort((a, b) -> a[0] != b[0] ? Integer.compare(a[0], b[0]) : Integer.compare(a[1], b[1]));

        // Return: index 0 = nodes as [rep1, rep2, ...], rest = edges
        // Actually return a flat structure: first element encodes nodes count
        // Better: return a pair-like structure
        List<int[]> result = new ArrayList<>();
        result.add(reps.stream().mapToInt(Integer::intValue).toArray()); // nodes
        for (int[] e : qEdges) result.add(e); // edges
        return result;
    }

    /** Internal: extract nodes from hasseGraph result. */
    static int[] nodes(List<int[]> hg) { return hg.get(0); }

    /** Internal: extract edges from hasseGraph result. */
    static List<int[]> edges(List<int[]> hg) { return hg.subList(1, hg.size()); }

    /**
     * Delete an edge from the graph (keep all nodes).
     */
    public static void graphDelete(
            List<Integer> nodes, List<int[]> edges, int[] edge,
            List<Integer> outNodes, List<int[]> outEdges) {
        outNodes.addAll(nodes);
        for (int[] e : edges) {
            if (e[0] != edge[0] || e[1] != edge[1]) {
                outEdges.add(e);
            }
        }
    }

    /**
     * Contract an edge: merge the two endpoints into one.
     */
    public static void graphContract(
            List<Integer> nodes, List<int[]> edges, int[] edge,
            List<Integer> outNodes, List<int[]> outEdges) {
        int u = edge[0], v = edge[1];
        for (int n : nodes) {
            if (n != v) outNodes.add(n);
        }
        Set<String> seen = new HashSet<>();
        for (int[] e : edges) {
            if (e[0] == edge[0] && e[1] == edge[1]) continue;
            int a = (e[0] == v) ? u : e[0];
            int b = (e[1] == v) ? u : e[1];
            if (a != b) {
                String key = a + "," + b;
                if (seen.add(key)) {
                    outEdges.add(new int[]{a, b});
                }
            }
        }
    }

    // =========================================================================
    // Deletion-contraction characteristic polynomial
    // =========================================================================

    /**
     * Compute characteristic polynomial via deletion-contraction on a graph.
     */
    static List<Integer> charPolyDcGraph(
            List<Integer> nodes, List<int[]> edges,
            Map<String, List<Integer>> memo) {
        String key = makeKey(nodes, edges);
        if (memo.containsKey(key)) return memo.get(key);

        int n = nodes.size();

        if (edges.isEmpty()) {
            // No edges: p(G, t) = t^n
            List<Integer> result = new ArrayList<>();
            result.add(1);
            for (int i = 0; i < n; i++) result.add(0);
            memo.put(key, result);
            return result;
        }

        if (n <= 1) {
            List<Integer> result = (n == 1) ? List.of(1, 0) : List.of(1);
            memo.put(key, result);
            return result;
        }

        // Pick first edge for deletion-contraction
        int[] e = edges.get(0);

        // Deletion
        List<Integer> delNodes = new ArrayList<>();
        List<int[]> delEdges = new ArrayList<>();
        graphDelete(nodes, edges, e, delNodes, delEdges);
        List<Integer> pDelete = charPolyDcGraph(delNodes, delEdges, memo);

        // Contraction
        List<Integer> conNodes = new ArrayList<>();
        List<int[]> conEdges = new ArrayList<>();
        graphContract(nodes, edges, e, conNodes, conEdges);
        List<Integer> pContract = charPolyDcGraph(conNodes, conEdges, memo);

        // p(G, t) = p(G\e, t) - p(G/e, t)
        List<Integer> result = polySubtract(pDelete, pContract);
        memo.put(key, result);
        return result;
    }

    private static String makeKey(List<Integer> nodes, List<int[]> edges) {
        StringBuilder sb = new StringBuilder();
        sb.append("N:");
        List<Integer> sorted = new ArrayList<>(nodes);
        Collections.sort(sorted);
        for (int n : sorted) sb.append(n).append(',');
        sb.append("E:");
        List<int[]> sortedEdges = new ArrayList<>(edges);
        sortedEdges.sort((a, b) -> a[0] != b[0] ? Integer.compare(a[0], b[0]) : Integer.compare(a[1], b[1]));
        for (int[] e : sortedEdges) sb.append(e[0]).append('-').append(e[1]).append(',');
        return sb.toString();
    }

    /**
     * Subtract polynomial b from polynomial a (highest degree first).
     */
    public static List<Integer> polySubtract(List<Integer> a, List<Integer> b) {
        int na = a.size(), nb = b.size();
        int maxLen = Math.max(na, nb);
        int[] pa = new int[maxLen];
        int[] pb = new int[maxLen];
        for (int i = 0; i < na; i++) pa[maxLen - na + i] = a.get(i);
        for (int i = 0; i < nb; i++) pb[maxLen - nb + i] = b.get(i);
        int[] result = new int[maxLen];
        for (int i = 0; i < maxLen; i++) result[i] = pa[i] - pb[i];
        // Remove leading zeros
        int start = 0;
        while (start < result.length - 1 && result[start] == 0) start++;
        List<Integer> out = new ArrayList<>();
        for (int i = start; i < result.length; i++) out.add(result[i]);
        return out;
    }

    /**
     * Add two polynomials (highest degree first).
     */
    public static List<Integer> polyAdd(List<Integer> a, List<Integer> b) {
        int na = a.size(), nb = b.size();
        int maxLen = Math.max(na, nb);
        int[] pa = new int[maxLen];
        int[] pb = new int[maxLen];
        for (int i = 0; i < na; i++) pa[maxLen - na + i] = a.get(i);
        for (int i = 0; i < nb; i++) pb[maxLen - nb + i] = b.get(i);
        int[] result = new int[maxLen];
        for (int i = 0; i < maxLen; i++) result[i] = pa[i] + pb[i];
        int start = 0;
        while (start < result.length - 1 && result[start] == 0) start++;
        List<Integer> out = new ArrayList<>();
        for (int i = start; i < result.length; i++) out.add(result[i]);
        return out;
    }

    // =========================================================================
    // Public API
    // =========================================================================

    /**
     * Compute the characteristic polynomial via deletion-contraction.
     */
    public static List<Integer> charPolynomial(StateSpace ss) {
        List<int[]> hg = hasseGraph(ss);
        int[] nodeArr = nodes(hg);
        List<int[]> edgeList = edges(hg);
        if (nodeArr.length == 0) return List.of(1);

        List<Integer> nodeList = new ArrayList<>();
        for (int n : nodeArr) nodeList.add(n);

        return charPolyDcGraph(nodeList, edgeList, new HashMap<>());
    }

    /**
     * Evaluate the deletion-contraction polynomial at t.
     */
    public static long evaluateCharPoly(StateSpace ss, int t) {
        return CharacteristicChecker.polyEvaluate(charPolynomial(ss), t);
    }

    /**
     * Compute an upper bound on the chromatic number of the Hasse diagram.
     */
    public static int chromaticNumberBound(StateSpace ss) {
        List<Integer> coeffs = charPolynomial(ss);
        List<int[]> hg = hasseGraph(ss);
        int n = nodes(hg).length;

        for (int k = 1; k <= n + 1; k++) {
            long val = CharacteristicChecker.polyEvaluate(coeffs, k);
            if (val > 0) return k;
        }
        return n + 1;
    }

    /**
     * Verify that deletion-contraction matches the Moebius computation.
     */
    public static boolean verifyDcVsMobius(StateSpace ss) {
        List<Integer> dcCoeffs = charPolynomial(ss);
        List<Integer> mobCoeffs = CharacteristicChecker.characteristicPolynomial(ss);
        return dcCoeffs.equals(mobCoeffs);
    }

    /**
     * Perform deletion of the edge at given index from the Hasse diagram.
     */
    public static List<int[]> deletion(StateSpace ss, int edgeIdx) {
        List<int[]> hg = hasseGraph(ss);
        int[] nodeArr = nodes(hg);
        List<int[]> edgeList = edges(hg);
        if (edgeIdx >= edgeList.size()) {
            List<int[]> result = new ArrayList<>();
            result.add(nodeArr);
            result.addAll(edgeList);
            return result;
        }
        List<Integer> nodeList = new ArrayList<>();
        for (int n : nodeArr) nodeList.add(n);
        List<Integer> outNodes = new ArrayList<>();
        List<int[]> outEdges = new ArrayList<>();
        graphDelete(nodeList, edgeList, edgeList.get(edgeIdx), outNodes, outEdges);
        List<int[]> result = new ArrayList<>();
        result.add(outNodes.stream().mapToInt(Integer::intValue).toArray());
        result.addAll(outEdges);
        return result;
    }

    /**
     * Perform contraction of the edge at given index from the Hasse diagram.
     */
    public static List<int[]> contraction(StateSpace ss, int edgeIdx) {
        List<int[]> hg = hasseGraph(ss);
        int[] nodeArr = nodes(hg);
        List<int[]> edgeList = edges(hg);
        if (edgeIdx >= edgeList.size()) {
            List<int[]> result = new ArrayList<>();
            result.add(nodeArr);
            result.addAll(edgeList);
            return result;
        }
        List<Integer> nodeList = new ArrayList<>();
        for (int n : nodeArr) nodeList.add(n);
        List<Integer> outNodes = new ArrayList<>();
        List<int[]> outEdges = new ArrayList<>();
        graphContract(nodeList, edgeList, edgeList.get(edgeIdx), outNodes, outEdges);
        List<int[]> result = new ArrayList<>();
        result.add(outNodes.stream().mapToInt(Integer::intValue).toArray());
        result.addAll(outEdges);
        return result;
    }

    // =========================================================================
    // Full analysis
    // =========================================================================

    /**
     * Complete deletion-contraction characteristic polynomial analysis.
     */
    public static CharPolyDCResult analyzeCharPoly(StateSpace ss) {
        List<Integer> dcCoeffs = charPolynomial(ss);
        List<Integer> mobCoeffs = CharacteristicChecker.characteristicPolynomial(ss);
        boolean match = verifyDcVsMobius(ss);

        int degree = dcCoeffs.size() - 1;
        int eval0 = (int) CharacteristicChecker.polyEvaluate(dcCoeffs, 0);
        int eval1 = (int) CharacteristicChecker.polyEvaluate(dcCoeffs, 1);
        int evalNeg1 = (int) CharacteristicChecker.polyEvaluate(dcCoeffs, -1);
        int chrom = chromaticNumberBound(ss);

        List<int[]> hg = hasseGraph(ss);
        int numEdges = edges(hg).size();
        int numStates = nodes(hg).length;

        return new CharPolyDCResult(
                dcCoeffs, mobCoeffs, match, degree,
                eval0, eval1, evalNeg1, chrom,
                numEdges, numStates);
    }
}

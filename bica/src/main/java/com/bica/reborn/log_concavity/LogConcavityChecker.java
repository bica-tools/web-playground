package com.bica.reborn.log_concavity;

import com.bica.reborn.algebraic.zeta.Zeta;
import com.bica.reborn.characteristic.CharacteristicChecker;
import com.bica.reborn.statespace.StateSpace;

import java.util.*;

/**
 * Log-concavity verification for session type lattices (Step 32e).
 *
 * <p>Verifies the log-concavity conjecture for session type lattices,
 * connecting to the Heron-Rota-Welsh conjecture and the Adiprasito-Huh-Katz
 * theorem (2018 Fields Medal work of June Huh).
 *
 * <p>A sequence a_0, a_1, ..., a_n is log-concave iff
 * a_k^2 &ge; a_{k-1} * a_{k+1} for all 1 &le; k &le; n-1.
 * Log-concavity implies unimodality (single peak).
 *
 * <p>Faithful Java port of Python {@code reticulate.log_concavity}.
 */
public final class LogConcavityChecker {

    private LogConcavityChecker() {}

    // =========================================================================
    // Result type
    // =========================================================================

    /**
     * Complete log-concavity analysis result.
     *
     * @param whitneySecondLc  true iff W_k (rank counts) is log-concave
     * @param whitneyFirstLc   true iff |w_k| (char poly coefficients) is log-concave
     * @param fVectorLc        true iff the f-vector is log-concave
     * @param isUnimodal       true iff W_k is unimodal
     * @param isUltraLc        true iff the normalised sequence is log-concave
     * @param hrwSatisfied     true iff the HRW conjecture conditions hold
     * @param ahkBoundHolds    true iff the AHK inequality is satisfied
     * @param whitneySecond    Whitney numbers of the second kind
     * @param whitneyFirst     Whitney numbers of the first kind
     * @param height           height of the lattice
     * @param numStates        number of quotient states
     * @param lcViolationsSecond violations in Whitney second kind
     * @param lcViolationsFirst  violations in Whitney first kind
     */
    public record LogConcavityResult(
            boolean whitneySecondLc,
            boolean whitneyFirstLc,
            boolean fVectorLc,
            boolean isUnimodal,
            boolean isUltraLc,
            boolean hrwSatisfied,
            boolean ahkBoundHolds,
            Map<Integer, Integer> whitneySecond,
            Map<Integer, Integer> whitneyFirst,
            int height,
            int numStates,
            List<int[]> lcViolationsSecond,
            List<int[]> lcViolationsFirst) {}

    // =========================================================================
    // Helpers
    // =========================================================================

    /**
     * Convert rank-to-count map to a list [d[0], d[1], ..., d[max]].
     */
    static List<Integer> sequenceFromDict(Map<Integer, Integer> d) {
        if (d.isEmpty()) return List.of();
        int maxK = d.keySet().stream().mapToInt(Integer::intValue).max().orElse(0);
        List<Integer> result = new ArrayList<>(maxK + 1);
        for (int k = 0; k <= maxK; k++) {
            result.add(d.getOrDefault(k, 0));
        }
        return result;
    }

    /**
     * Binomial coefficient C(n, k).
     */
    static long comb(int n, int k) {
        if (k < 0 || k > n) return 0;
        if (k == 0 || k == n) return 1;
        long result = 1;
        int minK = Math.min(k, n - k);
        for (int i = 0; i < minK; i++) {
            result = result * (n - i) / (i + 1);
        }
        return result;
    }

    /**
     * Find all positions where log-concavity is violated.
     * Returns list of [k, abs(a_{k-1}), abs(a_k), abs(a_{k+1})].
     */
    static List<int[]> findViolations(List<Integer> seq) {
        List<Long> absSeq = new ArrayList<>(seq.size());
        for (int v : seq) absSeq.add((long) Math.abs(v));
        List<int[]> violations = new ArrayList<>();
        for (int k = 1; k < absSeq.size() - 1; k++) {
            long ak = absSeq.get(k);
            long akPrev = absSeq.get(k - 1);
            long akNext = absSeq.get(k + 1);
            if (ak * ak < akPrev * akNext) {
                violations.add(new int[]{k, (int) akPrev, (int) ak, (int) akNext});
            }
        }
        return violations;
    }

    // =========================================================================
    // Unimodality
    // =========================================================================

    /**
     * Check if a sequence is unimodal (increases then decreases).
     */
    static boolean checkUnimodal(List<Integer> seq) {
        if (seq.size() <= 2) return true;
        boolean increasing = true;
        for (int k = 1; k < seq.size(); k++) {
            if (seq.get(k) < seq.get(k - 1)) {
                increasing = false;
            } else if (seq.get(k) > seq.get(k - 1) && !increasing) {
                return false;
            }
        }
        return true;
    }

    /**
     * Check if Whitney numbers of the second kind are unimodal.
     */
    public static boolean checkUnimodality(StateSpace ss) {
        Map<Integer, Integer> w = CharacteristicChecker.whitneyNumbersSecond(ss);
        List<Integer> seq = sequenceFromDict(w);
        return checkUnimodal(seq);
    }

    // =========================================================================
    // Ultra-log-concavity
    // =========================================================================

    /**
     * Check ultra-log-concavity: a_k / C(n, k) is log-concave.
     */
    static boolean checkUltraLogConcave(List<Integer> seq) {
        int n = seq.size() - 1;
        if (n <= 1) return true;

        List<Double> normalised = new ArrayList<>(n + 1);
        for (int k = 0; k <= n; k++) {
            long c = comb(n, k);
            if (c > 0) {
                normalised.add(Math.abs(seq.get(k)) / (double) c);
            } else {
                normalised.add(0.0);
            }
        }

        for (int k = 1; k < normalised.size() - 1; k++) {
            double ak = normalised.get(k);
            if (ak > 0) {
                if (ak * ak < normalised.get(k - 1) * normalised.get(k + 1) - 1e-12) {
                    return false;
                }
            }
        }
        return true;
    }

    // =========================================================================
    // Heron-Rota-Welsh conjecture check
    // =========================================================================

    /**
     * Check the Heron-Rota-Welsh conjecture conditions.
     *
     * <p>The HRW conjecture states that the absolute values of the coefficients
     * of the characteristic polynomial of a geometric lattice form a log-concave
     * sequence. We check:
     * <ol>
     *   <li>The characteristic polynomial has alternating signs.</li>
     *   <li>The absolute values of coefficients are log-concave.</li>
     * </ol>
     *
     * @return true iff both conditions hold
     */
    public static boolean heronRotaWelshCheck(StateSpace ss) {
        List<Integer> coeffs = CharacteristicChecker.characteristicPolynomial(ss);
        if (coeffs.size() <= 2) return true;

        // Check alternating signs
        boolean alternating = true;
        for (int i = 0; i < coeffs.size(); i++) {
            int expectedSign = (i % 2 == 0) ? 1 : -1;
            if (coeffs.get(i) != 0) {
                int actualSign = coeffs.get(i) > 0 ? 1 : -1;
                if (actualSign != expectedSign) {
                    alternating = false;
                    break;
                }
            }
        }

        // Check log-concavity of absolute values
        List<Integer> absCoeffs = new ArrayList<>(coeffs.size());
        for (int c : coeffs) absCoeffs.add(Math.abs(c));
        boolean lc = CharacteristicChecker.checkLogConcave(absCoeffs);

        return alternating && lc;
    }

    // =========================================================================
    // Adiprasito-Huh-Katz bound
    // =========================================================================

    /**
     * Check the Adiprasito-Huh-Katz inequality for session type lattices.
     *
     * <p>Checks |w_k|^2 &ge; |w_{k-1}| * |w_{k+1}| for all valid k.
     *
     * @return true iff the basic AHK inequality holds
     */
    public static boolean adiprasitoHuhKatzBound(StateSpace ss) {
        Map<Integer, Integer> w = CharacteristicChecker.whitneyNumbersFirst(ss);
        List<Integer> seq = sequenceFromDict(w);
        List<Integer> absSeq = new ArrayList<>(seq.size());
        for (int v : seq) absSeq.add(Math.abs(v));

        if (absSeq.size() <= 2) return true;
        return CharacteristicChecker.checkLogConcave(absSeq);
    }

    // =========================================================================
    // f-vector log-concavity
    // =========================================================================

    /**
     * Check log-concavity of the f-vector (rank level counts).
     */
    static boolean checkFVectorLc(StateSpace ss) {
        Map<Integer, Integer> w = CharacteristicChecker.whitneyNumbersSecond(ss);
        List<Integer> seq = sequenceFromDict(w);
        if (seq.size() <= 2) return true;
        return CharacteristicChecker.checkLogConcave(seq);
    }

    // =========================================================================
    // Quick checks
    // =========================================================================

    /**
     * Quick check: are Whitney numbers of the second kind log-concave?
     */
    public static boolean isLogConcaveWhitneySecond(StateSpace ss) {
        List<Integer> seq = sequenceFromDict(CharacteristicChecker.whitneyNumbersSecond(ss));
        return seq.size() >= 3 ? CharacteristicChecker.checkLogConcave(seq) : true;
    }

    /**
     * Quick check: are absolute Whitney numbers of the first kind log-concave?
     */
    public static boolean isLogConcaveWhitneyFirst(StateSpace ss) {
        List<Integer> seq = sequenceFromDict(CharacteristicChecker.whitneyNumbersFirst(ss));
        List<Integer> absSeq = new ArrayList<>(seq.size());
        for (int v : seq) absSeq.add(Math.abs(v));
        return absSeq.size() >= 3 ? CharacteristicChecker.checkLogConcave(absSeq) : true;
    }

    // =========================================================================
    // Log-concavity ratios
    // =========================================================================

    /**
     * Compute log-concavity ratios a_k^2 / (a_{k-1} * a_{k+1}).
     * Ratios &ge; 1 indicate log-concavity at that position.
     *
     * @return ratios for k = 1, ..., n-1
     */
    public static List<Double> logConcavityRatio(List<Integer> seq) {
        List<Long> absSeq = new ArrayList<>(seq.size());
        for (int v : seq) absSeq.add((long) Math.abs(v));
        List<Double> ratios = new ArrayList<>();
        for (int k = 1; k < absSeq.size() - 1; k++) {
            long ak = absSeq.get(k);
            long akPrev = absSeq.get(k - 1);
            long akNext = absSeq.get(k + 1);
            long denom = akPrev * akNext;
            if (denom > 0) {
                ratios.add((double) (ak * ak) / denom);
            } else if (ak > 0) {
                ratios.add(Double.POSITIVE_INFINITY);
            } else {
                ratios.add(1.0); // 0^2 >= 0 * x
            }
        }
        return ratios;
    }

    // =========================================================================
    // Preservation under product
    // =========================================================================

    /**
     * Check if log-concavity is preserved under parallel composition.
     *
     * @return true iff both components and the product have log-concave Whitney second kind
     */
    public static boolean preservationUnderProduct(
            StateSpace ssLeft, StateSpace ssRight, StateSpace ssProduct) {
        List<Integer> wLeft = sequenceFromDict(CharacteristicChecker.whitneyNumbersSecond(ssLeft));
        List<Integer> wRight = sequenceFromDict(CharacteristicChecker.whitneyNumbersSecond(ssRight));
        List<Integer> wProd = sequenceFromDict(CharacteristicChecker.whitneyNumbersSecond(ssProduct));

        boolean lcLeft = wLeft.size() >= 3 ? CharacteristicChecker.checkLogConcave(wLeft) : true;
        boolean lcRight = wRight.size() >= 3 ? CharacteristicChecker.checkLogConcave(wRight) : true;
        boolean lcProd = wProd.size() >= 3 ? CharacteristicChecker.checkLogConcave(wProd) : true;

        return lcLeft && lcRight && lcProd;
    }

    // =========================================================================
    // Full analysis
    // =========================================================================

    /**
     * Verify log-concavity of all relevant sequences for a session type lattice.
     */
    public static LogConcavityResult verifyLogConcavity(StateSpace ss) {
        Map<Integer, Integer> wSecond = CharacteristicChecker.whitneyNumbersSecond(ss);
        Map<Integer, Integer> wFirst = CharacteristicChecker.whitneyNumbersFirst(ss);

        List<Integer> seqSecond = sequenceFromDict(wSecond);
        List<Integer> seqFirst = sequenceFromDict(wFirst);

        // Basic log-concavity
        boolean lcSecond = seqSecond.size() >= 3
                ? CharacteristicChecker.checkLogConcave(seqSecond) : true;
        boolean lcFirst;
        if (seqFirst.size() >= 3) {
            List<Integer> absFirst = new ArrayList<>(seqFirst.size());
            for (int v : seqFirst) absFirst.add(Math.abs(v));
            lcFirst = CharacteristicChecker.checkLogConcave(absFirst);
        } else {
            lcFirst = true;
        }

        // f-vector log-concavity
        boolean fLc = checkFVectorLc(ss);

        // Unimodality
        boolean unimodal = checkUnimodal(seqSecond);

        // Ultra-log-concavity
        boolean ultra = checkUltraLogConcave(seqSecond);

        // HRW conjecture
        boolean hrw = heronRotaWelshCheck(ss);

        // AHK bound
        boolean ahk = adiprasitoHuhKatzBound(ss);

        // Violations
        List<int[]> violationsSecond = findViolations(seqSecond);
        List<Integer> absFirstSeq = new ArrayList<>(seqFirst.size());
        for (int v : seqFirst) absFirstSeq.add(Math.abs(v));
        List<int[]> violationsFirst = findViolations(absFirstSeq);

        // Quotient state count and height
        Zeta.SccResult sccR = Zeta.computeSccs(ss);
        int nQuotient = new HashSet<>(sccR.sccMap().values()).size();
        int height = Zeta.computeHeight(ss);

        return new LogConcavityResult(
                lcSecond, lcFirst, fLc,
                unimodal, ultra, hrw, ahk,
                wSecond, wFirst,
                height, nQuotient,
                violationsSecond, violationsFirst);
    }
}

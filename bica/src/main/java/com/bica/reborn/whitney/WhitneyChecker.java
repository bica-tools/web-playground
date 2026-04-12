package com.bica.reborn.whitney;

import com.bica.reborn.algebraic.zeta.Zeta;
import com.bica.reborn.characteristic.CharacteristicChecker;
import com.bica.reborn.statespace.StateSpace;

import java.util.*;

/**
 * Whitney number analysis for session type lattices (Step 30g).
 *
 * <p>Whitney numbers describe the rank-level structure of a lattice:
 * <ul>
 *   <li><b>W_k (second kind)</b>: number of elements at rank k</li>
 *   <li><b>w_k (first kind)</b>: Sum_{rho(x)=k} mu(top,x) -- signed rank-level counts</li>
 *   <li><b>Rank profile</b>: the sequence [W_0, W_1, ..., W_h]</li>
 *   <li><b>Unimodality</b>: W_0 <= W_1 <= ... <= W_m >= ... >= W_h</li>
 *   <li><b>Sperner property</b>: max antichain size = max W_k</li>
 *   <li><b>Log-concavity</b>: W_k^2 >= W_{k-1}*W_{k+1}</li>
 *   <li><b>Rank symmetry</b>: W_k = W_{h-k}</li>
 *   <li><b>Composition</b>: product lattice rank profile is convolution</li>
 * </ul>
 *
 * <p>Faithful Java port of Python {@code reticulate.whitney}.
 */
public final class WhitneyChecker {

    private WhitneyChecker() {}

    // =========================================================================
    // Result types
    // =========================================================================

    /** Complete Whitney number analysis. */
    public record WhitneyResult(
            int height,
            Map<Integer, Integer> whitneyFirst,
            Map<Integer, Integer> whitneySecond,
            List<Integer> rankProfile,
            int maxRankLevelK,
            int maxRankLevelW,
            boolean isUnimodal,
            boolean isLogConcaveSecond,
            boolean isLogConcaveFirst,
            boolean isRankSymmetric,
            int spernerWidth,
            boolean isSperner,
            int totalElements) {}

    // =========================================================================
    // Core computations
    // =========================================================================

    /**
     * Compute [W_0, W_1, ..., W_h] -- elements per corank level.
     */
    public static List<Integer> rankProfile(StateSpace ss) {
        Map<Integer, Integer> W = CharacteristicChecker.whitneyNumbersSecond(ss);
        if (W.isEmpty()) return List.of(1);
        int h = W.keySet().stream().mapToInt(Integer::intValue).max().orElse(0);
        List<Integer> profile = new ArrayList<>();
        for (int k = 0; k <= h; k++) {
            profile.add(W.getOrDefault(k, 0));
        }
        return profile;
    }

    /**
     * Find the rank level with most elements: (k, W_k).
     * Returns an array [k, W_k].
     */
    public static int[] maxRankLevel(StateSpace ss) {
        Map<Integer, Integer> W = CharacteristicChecker.whitneyNumbersSecond(ss);
        if (W.isEmpty()) return new int[]{0, 1};
        int kMax = -1;
        int wMax = -1;
        for (var entry : W.entrySet()) {
            if (entry.getValue() > wMax) {
                wMax = entry.getValue();
                kMax = entry.getKey();
            }
        }
        return new int[]{kMax, wMax};
    }

    // =========================================================================
    // Unimodality and log-concavity
    // =========================================================================

    /**
     * Check if a sequence is unimodal (increases then decreases).
     */
    public static boolean isUnimodal(List<Integer> profile) {
        int n = profile.size();
        if (n <= 2) return true;
        boolean increasing = true;
        for (int i = 1; i < n; i++) {
            if (profile.get(i) < profile.get(i - 1)) {
                increasing = false;
            } else if (profile.get(i) > profile.get(i - 1) && !increasing) {
                return false;
            }
        }
        return true;
    }

    /**
     * Check if W_k = W_{h-k} for all k (rank symmetry).
     */
    public static boolean isRankSymmetric(StateSpace ss) {
        List<Integer> prof = rankProfile(ss);
        int h = prof.size() - 1;
        for (int k = 0; k <= h; k++) {
            if (!prof.get(k).equals(prof.get(h - k))) return false;
        }
        return true;
    }

    // =========================================================================
    // Sperner property
    // =========================================================================

    /**
     * Compute the Sperner width (maximum antichain size).
     */
    public static int spernerWidth(StateSpace ss) {
        return Zeta.computeWidth(ss);
    }

    /**
     * Check if the lattice has the Sperner property.
     */
    public static boolean isSperner(StateSpace ss) {
        List<Integer> prof = rankProfile(ss);
        int maxW = prof.stream().mapToInt(Integer::intValue).max().orElse(0);
        int actualWidth = spernerWidth(ss);
        return actualWidth == maxW;
    }

    // =========================================================================
    // Convolution
    // =========================================================================

    /**
     * Convolve two rank profiles: (p1 * p2)[k] = Sum_{i+j=k} p1[i]*p2[j].
     */
    public static List<Integer> convolveProfiles(List<Integer> p1, List<Integer> p2) {
        if (p1.isEmpty() || p2.isEmpty()) return List.of();
        int n = p1.size() + p2.size() - 1;
        int[] result = new int[n];
        for (int i = 0; i < p1.size(); i++) {
            for (int j = 0; j < p2.size(); j++) {
                result[i + j] += p1.get(i) * p2.get(j);
            }
        }
        List<Integer> out = new ArrayList<>(n);
        for (int v : result) out.add(v);
        return out;
    }

    /**
     * Verify rank_profile(L1 x L2) = rank_profile(L1) * rank_profile(L2).
     */
    public static boolean verifyProfileConvolution(
            StateSpace ssLeft, StateSpace ssRight, StateSpace ssProduct) {
        List<Integer> pLeft = rankProfile(ssLeft);
        List<Integer> pRight = rankProfile(ssRight);
        List<Integer> pProduct = rankProfile(ssProduct);
        List<Integer> pExpected = convolveProfiles(pLeft, pRight);
        return pProduct.equals(pExpected);
    }

    // =========================================================================
    // Full analysis
    // =========================================================================

    /**
     * Complete Whitney number analysis.
     */
    public static WhitneyResult analyzeWhitney(StateSpace ss) {
        Map<Integer, Integer> w1 = CharacteristicChecker.whitneyNumbersFirst(ss);
        Map<Integer, Integer> w2 = CharacteristicChecker.whitneyNumbersSecond(ss);
        List<Integer> prof = rankProfile(ss);
        int h = prof.size() - 1;

        int[] mrl = maxRankLevel(ss);

        boolean unimodal = isUnimodal(prof);
        boolean lcSecond = CharacteristicChecker.checkLogConcave(prof);

        // Log-concavity of |w_k|
        List<Integer> w1Seq = new ArrayList<>();
        for (int k = 0; k <= h; k++) {
            w1Seq.add(w1.getOrDefault(k, 0));
        }
        boolean lcFirst = CharacteristicChecker.checkLogConcave(w1Seq);

        boolean sym = isRankSymmetric(ss);
        int sw = spernerWidth(ss);
        int maxProf = prof.stream().mapToInt(Integer::intValue).max().orElse(0);
        boolean sp = (sw == maxProf);

        Zeta.SccResult sccR = Zeta.computeSccs(ss);
        int total = new HashSet<>(sccR.sccMap().values()).size();

        return new WhitneyResult(
                h, w1, w2, prof,
                mrl[0], mrl[1],
                unimodal, lcSecond, lcFirst,
                sym, sw, sp, total);
    }
}

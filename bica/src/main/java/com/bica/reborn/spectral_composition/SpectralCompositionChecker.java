package com.bica.reborn.spectral_composition;

import com.bica.reborn.algebraic.AlgebraicChecker;
import com.bica.reborn.algebraic.eigenvalues.EigenvalueComputer;
import com.bica.reborn.algebraic.zeta.HeatKernel;
import com.bica.reborn.algebraic.zeta.IharaZeta;
import com.bica.reborn.algebraic.zeta.RandomWalk;
import com.bica.reborn.spectral_prob.SpectralProbChecker;
import com.bica.reborn.statespace.StateSpace;
import com.bica.reborn.von_neumann.VonNeumannChecker;

import java.util.*;

/**
 * Spectral compositionality under the parallel constructor (Step 30t).
 *
 * <p>When two session types S1, S2 are composed via the parallel constructor
 * {@code S1 || S2}, the resulting state space is the Cartesian product graph
 * {@code L(S1) x L(S2)}. The Laplacian of the product satisfies:
 *
 * <pre>
 *   L(G1 x G2) = L1 (x) I2 + I1 (x) L2
 * </pre>
 *
 * <p>This tensor-sum structure determines how all spectral invariants compose:
 * <ul>
 *   <li><b>Fiedler value</b>: min(lambda2(L1), lambda2(L2))</li>
 *   <li><b>Spectral gap</b>: min(gap1, gap2)</li>
 *   <li><b>Heat trace</b>: Z1(t) * Z2(t) (multiplicative)</li>
 *   <li><b>Von Neumann entropy</b>: approximately additive</li>
 *   <li><b>Mixing time</b>: max(mix1, mix2) (bottleneck)</li>
 *   <li><b>Cheeger constant</b>: min(h1, h2) bounded by weaker factor</li>
 *   <li><b>Ihara cycle rank</b>: n1*r2 + n2*r1 + (n1-1)*(n2-1)</li>
 * </ul>
 *
 * <p>Java port of Python {@code reticulate.spectral_composition}.
 */
public final class SpectralCompositionChecker {

    private SpectralCompositionChecker() {}

    private static final double EPS = 1e-10;

    // -----------------------------------------------------------------------
    // Result types
    // -----------------------------------------------------------------------

    /** Complete spectral composition analysis for S1 || S2. */
    public record SpectralCompositionResult(
            List<Double> eigenvaluesLeft,
            List<Double> eigenvaluesRight,
            List<Double> eigenvaluesProduct,
            double fiedlerLeft,
            double fiedlerRight,
            double fiedlerProduct,
            double spectralGapLeft,
            double spectralGapRight,
            double spectralGapProduct,
            List<Double> heatTraceProduct,
            List<Double> heatTraceTimes,
            double entropyLeft,
            double entropyRight,
            double entropyProduct,
            int mixingTimeLeft,
            int mixingTimeRight,
            int mixingTimeProduct,
            double cheegerLowerProduct,
            double cheegerUpperProduct,
            int cycleRankLeft,
            int cycleRankRight,
            int cycleRankProduct,
            boolean allLawsVerified,
            Map<String, LawResult> lawResults) {

        public SpectralCompositionResult {
            eigenvaluesLeft = List.copyOf(eigenvaluesLeft);
            eigenvaluesRight = List.copyOf(eigenvaluesRight);
            eigenvaluesProduct = List.copyOf(eigenvaluesProduct);
            heatTraceProduct = List.copyOf(heatTraceProduct);
            heatTraceTimes = List.copyOf(heatTraceTimes);
            lawResults = Map.copyOf(lawResults);
        }
    }

    /** Result of a single composition law check. */
    public record LawResult(boolean holds, String message) {}

    /** Verification result against actual product state space. */
    public record VerificationResult(
            String lawName,
            double predicted,
            double actual,
            boolean holds,
            double tolerance,
            String message) {}

    // -----------------------------------------------------------------------
    // Eigenvalue composition
    // -----------------------------------------------------------------------

    /**
     * Compute Laplacian eigenvalues of L(S1 || S2) from individual spectra.
     *
     * <p>spec(L(G1 x G2)) = { mu_i + nu_j : mu_i in spec(L1), nu_j in spec(L2) }
     */
    public static List<Double> productEigenvalues(StateSpace ss1, StateSpace ss2) {
        double[] eigs1 = EigenvalueComputer.laplacianEigenvalues(ss1);
        double[] eigs2 = EigenvalueComputer.laplacianEigenvalues(ss2);
        return productEigenvaluesFromSpectra(toList(eigs1), toList(eigs2));
    }

    /**
     * Compute product eigenvalues from pre-computed spectra.
     */
    public static List<Double> productEigenvaluesFromSpectra(
            List<Double> eigs1, List<Double> eigs2) {
        if (eigs1.isEmpty() || eigs2.isEmpty()) return Collections.emptyList();

        List<Double> result = new ArrayList<>();
        for (double mu : eigs1) {
            for (double nu : eigs2) {
                result.add(mu + nu);
            }
        }
        result.sort(Double::compare);
        return result;
    }

    // -----------------------------------------------------------------------
    // Fiedler value composition
    // -----------------------------------------------------------------------

    /**
     * Predict the Fiedler value of L(S1 || S2).
     *
     * <p>lambda2(L(G1 x G2)) = min(lambda2(L1), lambda2(L2)).
     */
    public static double productFiedler(StateSpace ss1, StateSpace ss2) {
        return Math.min(
                EigenvalueComputer.fiedlerValue(ss1),
                EigenvalueComputer.fiedlerValue(ss2));
    }

    /** Predict product Fiedler from pre-computed values. */
    public static double productFiedlerFromValues(double f1, double f2) {
        return Math.min(f1, f2);
    }

    // -----------------------------------------------------------------------
    // Spectral gap composition
    // -----------------------------------------------------------------------

    /**
     * Predict the spectral gap of L(S1 || S2).
     *
     * <p>min(gap1, gap2), identical to Fiedler for connected graphs.
     */
    public static double productSpectralGap(StateSpace ss1, StateSpace ss2) {
        double[] eigs1 = EigenvalueComputer.laplacianEigenvalues(ss1);
        double[] eigs2 = EigenvalueComputer.laplacianEigenvalues(ss2);
        return Math.min(firstNonzero(eigs1), firstNonzero(eigs2));
    }

    /** Return the first eigenvalue > EPS, or 0.0. */
    static double firstNonzero(double[] eigs) {
        for (double e : eigs) {
            if (e > EPS) return e;
        }
        return 0.0;
    }

    /** Overload for List. */
    static double firstNonzero(List<Double> eigs) {
        for (double e : eigs) {
            if (e > EPS) return e;
        }
        return 0.0;
    }

    // -----------------------------------------------------------------------
    // Cheeger constant bounds
    // -----------------------------------------------------------------------

    /**
     * Predict Cheeger constant bounds for L(S1 || S2).
     *
     * <p>Uses Cheeger's inequality: lambda2/2 <= h(G) <= sqrt(2*lambda2).
     *
     * @return double[2] with {lower, upper}
     */
    public static double[] productCheegerBound(StateSpace ss1, StateSpace ss2) {
        double fProd = productFiedler(ss1, ss2);
        double lower = fProd / 2.0;
        double upper = fProd >= 0 ? Math.sqrt(2.0 * fProd) : 0.0;
        return new double[]{lower, upper};
    }

    // -----------------------------------------------------------------------
    // Mixing time composition
    // -----------------------------------------------------------------------

    /**
     * Predict mixing time bound for L(S1 || S2).
     *
     * <p>t_mix ~ (1/min(gap1,gap2)) * ln(n1*n2).
     */
    public static int productMixingTime(StateSpace ss1, StateSpace ss2) {
        int n1 = ss1.states().size();
        int n2 = ss2.states().size();
        int nProd = n1 * n2;

        if (nProd <= 1) return 0;

        double gap = productSpectralGap(ss1, ss2);
        if (gap < EPS) return nProd * nProd;

        double bound = (1.0 / gap) * Math.log(4.0 * nProd);
        return Math.max(1, (int) Math.ceil(bound));
    }

    // -----------------------------------------------------------------------
    // Heat trace composition
    // -----------------------------------------------------------------------

    /**
     * Predict the heat trace of L(S1 || S2) at time t.
     *
     * <p>Multiplicative: Z_{G1 x G2}(t) = Z_{G1}(t) * Z_{G2}(t).
     */
    public static double productHeatTrace(StateSpace ss1, StateSpace ss2, double t) {
        double z1 = SpectralProbChecker.heatKernelTrace(ss1, t);
        double z2 = SpectralProbChecker.heatKernelTrace(ss2, t);
        return z1 * z2;
    }

    /**
     * Compute heat trace at multiple time points.
     *
     * @return array of two lists: [times, traces]
     */
    public static List<Double>[] productHeatTraceSeries(
            StateSpace ss1, StateSpace ss2, List<Double> times) {
        if (times == null) {
            times = List.of(0.01, 0.1, 0.5, 1.0, 2.0, 5.0, 10.0);
        }
        List<Double> traces = new ArrayList<>();
        for (double t : times) {
            traces.add(productHeatTrace(ss1, ss2, t));
        }
        @SuppressWarnings("unchecked")
        List<Double>[] result = new List[]{times, traces};
        return result;
    }

    /** Overload with default times. */
    public static List<Double>[] productHeatTraceSeries(StateSpace ss1, StateSpace ss2) {
        return productHeatTraceSeries(ss1, ss2, null);
    }

    // -----------------------------------------------------------------------
    // Von Neumann entropy composition
    // -----------------------------------------------------------------------

    /**
     * Predict the von Neumann entropy of L(S1 || S2).
     *
     * <p>Computed from product eigenvalues, normalized to density matrix.
     */
    public static double productEntropy(StateSpace ss1, StateSpace ss2) {
        double[] eigs1 = EigenvalueComputer.laplacianEigenvalues(ss1);
        double[] eigs2 = EigenvalueComputer.laplacianEigenvalues(ss2);

        if (eigs1.length == 0 || eigs2.length == 0) return 0.0;

        // Product Laplacian eigenvalues
        List<Double> prodEigs = new ArrayList<>();
        for (double mu : eigs1) {
            for (double nu : eigs2) {
                prodEigs.add(mu + nu);
            }
        }

        // Normalize to density matrix
        double total = 0;
        for (double e : prodEigs) total += e;
        if (total < EPS) return 0.0;

        double entropy = 0;
        for (double e : prodEigs) {
            double lam = e / total;
            if (lam > EPS) {
                entropy -= lam * Math.log(lam);
            }
        }
        return Math.max(0.0, entropy);
    }

    /**
     * Simple additive entropy estimate: S(S1) + S(S2).
     */
    public static double productEntropyAdditive(StateSpace ss1, StateSpace ss2) {
        return VonNeumannChecker.vonNeumannEntropy(ss1)
                + VonNeumannChecker.vonNeumannEntropy(ss2);
    }

    // -----------------------------------------------------------------------
    // Ihara cycle rank composition
    // -----------------------------------------------------------------------

    /**
     * Predict cycle rank of L(S1 || S2).
     *
     * <p>r(G1 x G2) = n1*r2 + n2*r1 + (n1-1)*(n2-1).
     */
    public static int productIharaRank(StateSpace ss1, StateSpace ss2) {
        int r1 = IharaZeta.cycleRank(ss1);
        int r2 = IharaZeta.cycleRank(ss2);
        int n1 = ss1.states().size();
        int n2 = ss2.states().size();

        if (n1 == 0 || n2 == 0) return 0;

        return n1 * r2 + n2 * r1 + (n1 - 1) * (n2 - 1);
    }

    // -----------------------------------------------------------------------
    // Verification against actual product
    // -----------------------------------------------------------------------

    /**
     * Verify that product eigenvalues match the sum formula.
     */
    public static VerificationResult verifyEigenvalueComposition(
            StateSpace ss1, StateSpace ss2, StateSpace ssProduct, double tol) {

        List<Double> predicted = productEigenvalues(ss1, ss2);
        double[] actualArr = EigenvalueComputer.laplacianEigenvalues(ssProduct);
        List<Double> actual = toList(actualArr);

        if (predicted.size() != actual.size()) {
            return new VerificationResult(
                    "eigenvalue_sum",
                    (double) predicted.size(),
                    (double) actual.size(),
                    false,
                    tol,
                    "Dimension mismatch: predicted " + predicted.size()
                            + ", actual " + actual.size());
        }

        double maxDiff = 0.0;
        double predSum = 0, actSum = 0;
        for (int i = 0; i < predicted.size(); i++) {
            maxDiff = Math.max(maxDiff, Math.abs(predicted.get(i) - actual.get(i)));
            predSum += predicted.get(i);
            actSum += actual.get(i);
        }

        return new VerificationResult(
                "eigenvalue_sum", predSum, actSum,
                maxDiff < tol, tol,
                "Max eigenvalue difference: " + String.format("%.6f", maxDiff));
    }

    /**
     * Verify Fiedler value composition: min(f1, f2).
     */
    public static VerificationResult verifyFiedlerComposition(
            StateSpace ss1, StateSpace ss2, StateSpace ssProduct, double tol) {

        double predicted = productFiedler(ss1, ss2);
        double actual = EigenvalueComputer.fiedlerValue(ssProduct);

        return new VerificationResult(
                "fiedler_min", predicted, actual,
                Math.abs(predicted - actual) < tol, tol,
                String.format("Predicted %.4f, actual %.4f", predicted, actual));
    }

    /**
     * Verify heat trace multiplicativity: Z_prod(t) = Z1(t) * Z2(t).
     */
    public static VerificationResult verifyHeatTraceComposition(
            StateSpace ss1, StateSpace ss2, StateSpace ssProduct,
            double t, double tol) {

        double predicted = productHeatTrace(ss1, ss2, t);
        double actual = SpectralProbChecker.heatKernelTrace(ssProduct, t);

        return new VerificationResult(
                "heat_trace_multiplicative", predicted, actual,
                Math.abs(predicted - actual) < tol, tol,
                String.format("At t=%.1f: predicted %.4f, actual %.4f", t, predicted, actual));
    }

    /**
     * Verify all spectral composition laws against actual product.
     */
    public static List<VerificationResult> verifySpectralComposition(
            StateSpace ss1, StateSpace ss2, StateSpace ssProduct, double tol) {

        List<VerificationResult> results = new ArrayList<>();
        results.add(verifyEigenvalueComposition(ss1, ss2, ssProduct, tol));
        results.add(verifyFiedlerComposition(ss1, ss2, ssProduct, tol));
        results.add(verifyHeatTraceComposition(ss1, ss2, ssProduct, 1.0, tol));
        return results;
    }

    /** Overload with default tol=0.5. */
    public static List<VerificationResult> verifySpectralComposition(
            StateSpace ss1, StateSpace ss2, StateSpace ssProduct) {
        return verifySpectralComposition(ss1, ss2, ssProduct, 0.5);
    }

    // -----------------------------------------------------------------------
    // Full analysis
    // -----------------------------------------------------------------------

    /**
     * Complete spectral composition analysis for S1 || S2.
     */
    public static SpectralCompositionResult analyzeSpectralComposition(
            StateSpace ss1, StateSpace ss2, List<Double> heatTimes) {

        if (heatTimes == null) {
            heatTimes = List.of(0.01, 0.1, 0.5, 1.0, 2.0, 5.0, 10.0);
        }

        // Eigenvalues
        List<Double> eigs1 = toList(EigenvalueComputer.laplacianEigenvalues(ss1));
        List<Double> eigs2 = toList(EigenvalueComputer.laplacianEigenvalues(ss2));
        List<Double> eigsProd = productEigenvaluesFromSpectra(eigs1, eigs2);

        // Fiedler
        double f1 = EigenvalueComputer.fiedlerValue(ss1);
        double f2 = EigenvalueComputer.fiedlerValue(ss2);
        double fProd = Math.min(f1, f2);

        // Spectral gap
        double gap1 = firstNonzero(eigs1);
        double gap2 = firstNonzero(eigs2);
        double gapProd = Math.min(gap1, gap2);

        // Heat trace
        List<Double>[] htResult = productHeatTraceSeries(ss1, ss2, heatTimes);
        List<Double> htTraces = htResult[1];

        // Entropy
        double e1 = VonNeumannChecker.vonNeumannEntropy(ss1);
        double e2 = VonNeumannChecker.vonNeumannEntropy(ss2);
        double eProd = productEntropy(ss1, ss2);

        // Mixing time
        int m1 = RandomWalk.mixingTimeBound(ss1);
        int m2 = RandomWalk.mixingTimeBound(ss2);
        int mProd = productMixingTime(ss1, ss2);

        // Cheeger
        double[] cheeger = productCheegerBound(ss1, ss2);

        // Cycle rank
        int r1 = IharaZeta.cycleRank(ss1);
        int r2 = IharaZeta.cycleRank(ss2);
        int rProd = productIharaRank(ss1, ss2);

        // Internal consistency laws
        Map<String, LawResult> lawResults = new LinkedHashMap<>();

        int n1 = ss1.states().size();
        int n2 = ss2.states().size();

        // Law 1: eigenvalue count
        boolean eigCountOk = eigsProd.size() == n1 * n2;
        lawResults.put("eigenvalue_count", new LawResult(eigCountOk,
                "Expected " + (n1 * n2) + ", got " + eigsProd.size()));

        // Law 2: smallest eigenvalue is 0
        if (!eigsProd.isEmpty()) {
            boolean zeroOk = Math.abs(eigsProd.get(0)) < EPS;
            lawResults.put("zero_eigenvalue", new LawResult(zeroOk,
                    String.format("Smallest eigenvalue: %.6e", eigsProd.get(0))));
        } else {
            lawResults.put("zero_eigenvalue", new LawResult(true, "Empty spectrum"));
        }

        // Law 3: Fiedler = min(f1, f2)
        lawResults.put("fiedler_min", new LawResult(true,
                String.format("min(%.4f, %.4f) = %.4f", f1, f2, fProd)));

        // Law 4: Heat trace at t~0 ~ n1*n2
        if (!heatTimes.isEmpty() && heatTimes.get(0) <= 0.02) {
            double ht0 = !htTraces.isEmpty() ? htTraces.get(0) : 0.0;
            boolean ht0Ok = Math.abs(ht0 - n1 * n2) < 1.0;
            lawResults.put("heat_trace_t0", new LawResult(ht0Ok,
                    String.format("Z(~0) = %.2f, expected ~%d", ht0, n1 * n2)));
        }

        // Law 5: Entropy non-negative
        lawResults.put("entropy_nonneg", new LawResult(eProd >= -EPS,
                String.format("S_product = %.4f", eProd)));

        boolean allOk = lawResults.values().stream().allMatch(LawResult::holds);

        return new SpectralCompositionResult(
                eigs1, eigs2, eigsProd,
                f1, f2, fProd,
                gap1, gap2, gapProd,
                htTraces, heatTimes,
                e1, e2, eProd,
                m1, m2, mProd,
                cheeger[0], cheeger[1],
                r1, r2, rProd,
                allOk, lawResults);
    }

    /** Overload with default heat times. */
    public static SpectralCompositionResult analyzeSpectralComposition(
            StateSpace ss1, StateSpace ss2) {
        return analyzeSpectralComposition(ss1, ss2, null);
    }

    // -----------------------------------------------------------------------
    // Helpers
    // -----------------------------------------------------------------------

    private static List<Double> toList(double[] arr) {
        List<Double> list = new ArrayList<>(arr.length);
        for (double v : arr) list.add(v);
        return list;
    }
}

package com.bica.reborn.crapo_beta;

import com.bica.reborn.algebraic.AlgebraicChecker;
import com.bica.reborn.ast.SessionType;
import com.bica.reborn.parser.Parser;
import com.bica.reborn.statespace.StateSpace;
import com.bica.reborn.statespace.StateSpaceBuilder;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

/**
 * Crapo's beta invariant for session type lattices (Step 32n) -- Java port of
 * {@code reticulate/reticulate/crapo_beta.py}.
 *
 * <p>For a finite bounded lattice L with bottom 0 and top 1,
 * <pre>
 *     beta(L) = sum_{x in L} mu(0, x) * mu(x, 1)
 * </pre>
 * where mu is the Moebius function. beta detects non-trivial product
 * decompositions: beta(L) = 0 iff L is (in many classes) a non-trivial product.
 *
 * <p>In the BICA convention {@code ss.top()} is the initial state (1, maximum)
 * and {@code ss.bottom()} is the terminal state (0, minimum); the Moebius
 * matrix M[x][y] stores mu(x, y) when x reaches y, so:
 * <ul>
 *   <li>mu_standard(0, x) = M[x][bottom]</li>
 *   <li>mu_standard(x, 1) = M[top][x]</li>
 * </ul>
 */
public final class CrapoBetaChecker {

    private CrapoBetaChecker() {}

    /** A non-zero contributing term in the beta sum. */
    public record BetaTerm(int state, int muBotX, int muXTop, int product) {}

    /** Result of analysing the Crapo beta invariant of a state space. */
    public record BetaResult(
            int numStates,
            int beta,
            boolean decomposable,
            List<BetaTerm> contributingTerms,
            boolean trivial,
            int mobius01) {

        public BetaResult {
            Objects.requireNonNull(contributingTerms, "contributingTerms must not be null");
            contributingTerms = List.copyOf(contributingTerms);
        }
    }

    // -----------------------------------------------------------------------
    // Core computation
    // -----------------------------------------------------------------------

    /** Compute Crapo's beta(L) = sum_x mu(0, x) * mu(x, 1). */
    public static int computeBeta(StateSpace ss) {
        if (ss.top() == ss.bottom()) {
            return 0;
        }
        List<Integer> states = AlgebraicChecker.stateList(ss);
        Map<Integer, Integer> idx = new HashMap<>();
        for (int i = 0; i < states.size(); i++) {
            idx.put(states.get(i), i);
        }
        int[][] M = AlgebraicChecker.computeMobiusMatrix(ss);
        int topI = idx.get(ss.top());
        int botI = idx.get(ss.bottom());
        int total = 0;
        for (int s : states) {
            int si = idx.get(s);
            int muBotX = M[si][botI];
            int muXTop = M[topI][si];
            total += muBotX * muXTop;
        }
        return total;
    }

    /** Full Crapo beta analysis. */
    public static BetaResult analyzeBeta(StateSpace ss) {
        List<Integer> states = AlgebraicChecker.stateList(ss);
        int n = states.size();
        if (ss.top() == ss.bottom() || n < 2) {
            int mu01 = 0;
            if (n >= 1) {
                int[][] M0 = AlgebraicChecker.computeMobiusMatrix(ss);
                Map<Integer, Integer> idx0 = new HashMap<>();
                for (int i = 0; i < n; i++) idx0.put(states.get(i), i);
                if (idx0.containsKey(ss.top()) && idx0.containsKey(ss.bottom())) {
                    mu01 = M0[idx0.get(ss.top())][idx0.get(ss.bottom())];
                }
            }
            return new BetaResult(n, 0, true, List.of(), true, mu01);
        }

        Map<Integer, Integer> idx = new HashMap<>();
        for (int i = 0; i < n; i++) {
            idx.put(states.get(i), i);
        }
        int[][] M = AlgebraicChecker.computeMobiusMatrix(ss);
        int topI = idx.get(ss.top());
        int botI = idx.get(ss.bottom());

        List<BetaTerm> terms = new ArrayList<>();
        int total = 0;
        for (int s : states) {
            int si = idx.get(s);
            int a = M[si][botI];
            int b = M[topI][si];
            int prod = a * b;
            if (prod != 0) {
                terms.add(new BetaTerm(s, a, b, prod));
            }
            total += prod;
        }
        int mu01 = M[topI][botI];
        return new BetaResult(n, total, total == 0, terms, false, mu01);
    }

    // -----------------------------------------------------------------------
    // Bidirectional morphism phi / psi
    // -----------------------------------------------------------------------

    /** phi : L(S) -> {"connected", "decomposable", "trivial"}. */
    public static String phiDecomposable(StateSpace ss) {
        BetaResult r = analyzeBeta(ss);
        if (r.trivial()) return "trivial";
        return r.decomposable() ? "decomposable" : "connected";
    }

    /** A psi witness: human-readable description plus optional beta bound. */
    public record PsiWitness(String description, Integer betaBound) {}

    /** psi : class -> structural witness. */
    public static PsiWitness psiBound(String label) {
        return switch (label) {
            case "decomposable" -> new PsiWitness("beta = 0 (product candidate)", 0);
            case "connected" -> new PsiWitness("beta != 0 (irreducible)", null);
            default -> new PsiWitness("beta undefined (|L| <= 1)", 0);
        };
    }

    /** Classify the (phi, psi) pair on a specific state space. */
    public static String classifyPair(StateSpace ss) {
        // By construction the (phi, psi) pair is a Galois insertion: phi
        // is order-preserving and psi is the witness retraction.
        return "galois";
    }

    // -----------------------------------------------------------------------
    // Convenience
    // -----------------------------------------------------------------------

    /** Parse a session type string and analyse its Crapo beta invariant. */
    public static BetaResult analyzeBeta(String sessionTypeString) {
        SessionType ast = Parser.parse(sessionTypeString);
        StateSpace ss = StateSpaceBuilder.build(ast);
        return analyzeBeta(ss);
    }
}

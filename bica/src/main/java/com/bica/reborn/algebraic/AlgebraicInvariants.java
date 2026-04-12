package com.bica.reborn.algebraic;

import java.util.List;
import java.util.Objects;

/**
 * Complete set of algebraic invariants for a session type lattice.
 *
 * <p>Includes matrix-based invariants (zeta, Mobius), spectral invariants
 * (eigenvalues, Fiedler value), and structural counts (join/meet irreducibles).
 *
 * @param numStates           number of states in the state space
 * @param mobiusValue         mu(top, bottom) -- Euler characteristic
 * @param rotaPolynomial      coefficients of chi_P(t), highest degree first
 * @param eigenvalues         sorted eigenvalues of the undirected Hasse adjacency matrix
 * @param spectralRadius      largest absolute eigenvalue
 * @param fiedlerValue        second-smallest Laplacian eigenvalue (algebraic connectivity)
 * @param tropicalDiameter    longest shortest path over all reachable pairs
 * @param tropicalEigenvalue  maximum cycle mean (0 if acyclic)
 * @param vonNeumannEntropy   spectral entropy of the Laplacian
 * @param numJoinIrreducibles number of join-irreducible elements
 * @param numMeetIrreducibles number of meet-irreducible elements
 */
public record AlgebraicInvariants(
        int numStates,
        int mobiusValue,
        List<Integer> rotaPolynomial,
        List<Double> eigenvalues,
        double spectralRadius,
        double fiedlerValue,
        int tropicalDiameter,
        double tropicalEigenvalue,
        double vonNeumannEntropy,
        int numJoinIrreducibles,
        int numMeetIrreducibles) {

    public AlgebraicInvariants {
        Objects.requireNonNull(rotaPolynomial, "rotaPolynomial must not be null");
        Objects.requireNonNull(eigenvalues, "eigenvalues must not be null");
        rotaPolynomial = List.copyOf(rotaPolynomial);
        eigenvalues = List.copyOf(eigenvalues);
    }
}

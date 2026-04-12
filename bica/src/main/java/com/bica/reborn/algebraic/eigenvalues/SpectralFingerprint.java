package com.bica.reborn.algebraic.eigenvalues;

import java.util.List;
import java.util.Objects;

/**
 * Compact spectral summary of a session type lattice.
 *
 * <p>Captures the key spectral invariants in a single immutable record
 * that can be used for protocol classification and comparison.
 *
 * @param numStates              number of states in the state space
 * @param adjacencyEigenvalues   sorted eigenvalues of the Hasse adjacency matrix
 * @param laplacianEigenvalues   sorted eigenvalues of the Laplacian L = D - A
 * @param spectralRadius         max |lambda_i| from adjacency spectrum
 * @param spectralGap            lambda_max - lambda_second from adjacency spectrum
 * @param fiedlerValue           second-smallest Laplacian eigenvalue (algebraic connectivity)
 * @param energy                 graph energy = sum |lambda_i|
 * @param numEdges               number of edges in the Hasse diagram
 * @param avgDegree              average degree of the Hasse diagram
 */
public record SpectralFingerprint(
        int numStates,
        List<Double> adjacencyEigenvalues,
        List<Double> laplacianEigenvalues,
        double spectralRadius,
        double spectralGap,
        double fiedlerValue,
        double energy,
        int numEdges,
        double avgDegree) {

    public SpectralFingerprint {
        Objects.requireNonNull(adjacencyEigenvalues, "adjacencyEigenvalues must not be null");
        Objects.requireNonNull(laplacianEigenvalues, "laplacianEigenvalues must not be null");
        adjacencyEigenvalues = List.copyOf(adjacencyEigenvalues);
        laplacianEigenvalues = List.copyOf(laplacianEigenvalues);
    }

    /** True if the Hasse diagram is connected (Fiedler value > 0). */
    public boolean isConnected() {
        return fiedlerValue > 1e-10;
    }
}

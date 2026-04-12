package com.bica.reborn.spectral;

/**
 * Feature vector extracted from a session type lattice.
 *
 * @param spectralRadius    largest absolute eigenvalue of the Hasse adjacency matrix
 * @param fiedlerValue      second-smallest Laplacian eigenvalue (algebraic connectivity)
 * @param vonNeumannEntropy spectral entropy of the Laplacian
 * @param width             maximum antichain size (Dilworth width)
 * @param height            length of longest chain (number of edges, top to bottom)
 * @param numStates         number of states in the state space
 * @param numTransitions    number of transitions in the state space
 */
public record SpectralFeatures(
        double spectralRadius,
        double fiedlerValue,
        double vonNeumannEntropy,
        int width,
        int height,
        int numStates,
        int numTransitions) {

    /** Return the feature vector as an array of doubles. */
    public double[] asVector() {
        return new double[]{
                spectralRadius, fiedlerValue, vonNeumannEntropy,
                width, height, numStates, numTransitions
        };
    }
}

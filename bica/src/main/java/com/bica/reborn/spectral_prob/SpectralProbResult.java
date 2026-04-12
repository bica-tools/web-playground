package com.bica.reborn.spectral_prob;

import java.util.Map;

/**
 * Spectral-probabilistic analysis results.
 *
 * @param numStates              number of states
 * @param cheegerConstant        edge expansion h(G)
 * @param cheegerLower           lambda_2 / 2 (lower bound from Cheeger inequality)
 * @param cheegerUpper           sqrt(2 * lambda_2) (upper bound from Cheeger inequality)
 * @param mixingTimeBound        upper bound on mixing time from spectral gap
 * @param heatTrace              trace of heat kernel at t=1: sum exp(-lambda_i)
 * @param vonNeumannEntropy      S = -sum (lambda_i / sum lambda_j) log(lambda_i / sum lambda_j)
 * @param normalizedEntropy      S / log(n-1) (0 to 1 scale)
 * @param stationaryDistribution stationary distribution of random walk (degree-proportional)
 */
public record SpectralProbResult(
        int numStates,
        double cheegerConstant,
        double cheegerLower,
        double cheegerUpper,
        double mixingTimeBound,
        double heatTrace,
        double vonNeumannEntropy,
        double normalizedEntropy,
        Map<Integer, Double> stationaryDistribution) {

    public SpectralProbResult {
        stationaryDistribution = Map.copyOf(stationaryDistribution);
    }
}

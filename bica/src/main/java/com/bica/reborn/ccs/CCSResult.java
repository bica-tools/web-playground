package com.bica.reborn.ccs;

/**
 * Result of CCS analysis for a session type.
 *
 * @param ccsProcess              the CCS process term
 * @param lts                     the labelled transition system derived from the CCS process
 * @param numStates               number of LTS states
 * @param numTransitions          number of LTS transitions
 * @param bisimilarToStateSpace   whether the LTS is bisimilar to the session type's state space
 */
public record CCSResult(
        CCSProcess ccsProcess,
        LTS lts,
        int numStates,
        int numTransitions,
        boolean bisimilarToStateSpace) {
}

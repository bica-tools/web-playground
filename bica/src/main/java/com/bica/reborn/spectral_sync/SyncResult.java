package com.bica.reborn.spectral_sync;

import java.util.List;

/**
 * Spectral synchronization analysis result.
 *
 * @param numStates      number of states
 * @param numRoles       number of distinct roles (from transition labels)
 * @param roleCoupling   role x role interaction count matrix
 * @param roles          list of role names
 * @param syncSpectrum   eigenvalues of the role coupling matrix
 * @param maxCoupling    maximum coupling between any two distinct roles
 * @param isolationScore minimum row sum (least connected role)
 */
public record SyncResult(
        int numStates,
        int numRoles,
        List<List<Integer>> roleCoupling,
        List<String> roles,
        List<Double> syncSpectrum,
        int maxCoupling,
        int isolationScore) {

    public SyncResult {
        roleCoupling = roleCoupling.stream()
                .map(List::copyOf)
                .toList();
        roles = List.copyOf(roles);
        syncSpectrum = List.copyOf(syncSpectrum);
    }
}

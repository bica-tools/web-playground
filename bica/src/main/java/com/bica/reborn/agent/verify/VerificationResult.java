package com.bica.reborn.agent.verify;

import com.bica.reborn.lattice.DistributivityResult;
import com.bica.reborn.lattice.LatticeResult;
import com.bica.reborn.termination.TerminationResult;

/**
 * Comprehensive verification outcome for an agent's protocol.
 *
 * @param agentName       agent name
 * @param isLattice       state space forms a lattice
 * @param termination     termination check result
 * @param distributivity  distributivity classification
 * @param stateCount      number of states in the protocol
 * @param verified        all critical checks passed
 */
public record VerificationResult(
        String agentName,
        boolean isLattice,
        TerminationResult termination,
        DistributivityResult distributivity,
        int stateCount,
        boolean verified) {

    /** Human-readable summary for error messages. */
    public String summary() {
        if (verified) return "OK";
        var sb = new StringBuilder();
        if (!isLattice) sb.append("not a lattice; ");
        if (termination != null && !termination.isTerminating()) sb.append("non-terminating; ");
        return sb.toString().stripTrailing();
    }
}

package com.bica.reborn.agent.verify;

import com.bica.reborn.agent.Agent;
import com.bica.reborn.agent.AgentProtocol;
import com.bica.reborn.lattice.DistributivityChecker;
import com.bica.reborn.lattice.DistributivityResult;
import com.bica.reborn.termination.TerminationChecker;
import com.bica.reborn.termination.TerminationResult;

/**
 * Pre-deployment verification pipeline for agents.
 *
 * <p>Runs the full BICA analysis pipeline on an agent's protocol:
 * lattice property, termination, distributivity classification.
 */
public final class AgentVerifier {

    private AgentVerifier() {}

    /**
     * Run the full verification pipeline on an agent.
     */
    public static VerificationResult verify(Agent agent) {
        AgentProtocol p = agent.protocol();

        boolean isLattice = p.isLattice();
        TerminationResult termination = TerminationChecker.checkTermination(p.ast());
        DistributivityResult distributivity = DistributivityChecker.checkDistributive(p.stateSpace());

        boolean verified = isLattice && termination.isTerminating();

        return new VerificationResult(
                agent.name(), isLattice, termination, distributivity,
                p.stateCount(), verified);
    }
}

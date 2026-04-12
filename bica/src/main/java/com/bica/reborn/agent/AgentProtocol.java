package com.bica.reborn.agent;

import com.bica.reborn.ast.SessionType;
import com.bica.reborn.lattice.LatticeChecker;
import com.bica.reborn.lattice.LatticeResult;
import com.bica.reborn.parser.Parser;
import com.bica.reborn.statespace.StateSpace;
import com.bica.reborn.statespace.StateSpaceBuilder;

import java.util.Objects;
import java.util.Set;

/**
 * A validated agent protocol: session type string, parsed AST, built state space,
 * and lattice verification result.
 *
 * <p>Every agent must have an {@code AgentProtocol}. Construction via {@link #of(String)}
 * runs the full BICA pipeline (parse → build → check lattice) and fails if the
 * session type does not form a lattice.
 *
 * <p>This is the bridge between the session type analysis library and the agent
 * framework: it wraps {@link Parser}, {@link StateSpaceBuilder}, and
 * {@link LatticeChecker} into a single validated object.
 *
 * @param typeString    the session type in BICA syntax
 * @param ast           parsed AST
 * @param stateSpace    the labeled transition system
 * @param latticeResult lattice verification result
 */
public record AgentProtocol(
        String typeString,
        SessionType ast,
        StateSpace stateSpace,
        LatticeResult latticeResult) {

    public AgentProtocol {
        Objects.requireNonNull(typeString, "typeString must not be null");
        Objects.requireNonNull(ast, "ast must not be null");
        Objects.requireNonNull(stateSpace, "stateSpace must not be null");
        Objects.requireNonNull(latticeResult, "latticeResult must not be null");
    }

    /**
     * Parse, build state space, and verify lattice property.
     *
     * @throws AgentException if the session type does not form a lattice
     */
    public static AgentProtocol of(String sessionType) {
        Objects.requireNonNull(sessionType, "sessionType must not be null");
        SessionType ast = Parser.parse(sessionType);
        StateSpace ss = StateSpaceBuilder.build(ast);
        LatticeResult lr = LatticeChecker.checkLattice(ss);
        if (!lr.isLattice()) {
            throw new AgentException("Protocol does not form a lattice: " + sessionType
                    + " (counterexample: " + lr.counterexample() + ")");
        }
        return new AgentProtocol(sessionType, ast, ss, lr);
    }

    /**
     * Parse and build without lattice enforcement. For testing or
     * experimental protocols that may not form lattices.
     */
    public static AgentProtocol unchecked(String sessionType) {
        Objects.requireNonNull(sessionType, "sessionType must not be null");
        SessionType ast = Parser.parse(sessionType);
        StateSpace ss = StateSpaceBuilder.build(ast);
        LatticeResult lr = LatticeChecker.checkLattice(ss);
        return new AgentProtocol(sessionType, ast, ss, lr);
    }

    /** True iff the state space forms a lattice. */
    public boolean isLattice() {
        return latticeResult.isLattice();
    }

    /** Number of states in the protocol's state space. */
    public int stateCount() {
        return stateSpace.states().size();
    }

    /** Methods enabled at the initial (top) state. */
    public Set<String> initialMethods() {
        return stateSpace.enabledMethods(stateSpace.top());
    }

    /** Selection labels enabled at the initial state. */
    public Set<String> initialSelections() {
        return stateSpace.enabledSelections(stateSpace.top());
    }
}

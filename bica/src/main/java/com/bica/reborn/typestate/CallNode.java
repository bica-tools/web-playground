package com.bica.reborn.typestate;

import java.util.List;
import java.util.Map;
import java.util.Objects;

/**
 * Tree-structured representation of method calls on a tracked variable.
 *
 * <p>A flat sequence of calls cannot represent selection-dependent control flow:
 * after calling a method that returns an enum (internal choice), the client
 * switches on the return value and follows a different path per branch.
 *
 * <p>{@code CallNode} captures this as a tree:
 * <ul>
 *   <li>{@link Call} — a single method invocation</li>
 *   <li>{@link SelectionSwitch} — a switch on a selection return value,
 *       with one call list per branch</li>
 * </ul>
 */
public sealed interface CallNode {

    /**
     * A single method call on the tracked variable.
     *
     * @param call the method call
     */
    record Call(MethodCall call) implements CallNode {
        public Call {
            Objects.requireNonNull(call, "call must not be null");
        }
    }

    /**
     * A switch statement on a selection return value.
     *
     * <p>The {@code variable} is the local variable holding the enum result
     * (e.g., {@code Status r = f.open()}). The {@code branches} map each
     * enum constant (selection label) to the calls made in that case arm.
     *
     * @param variable  the variable being switched on
     * @param branches  label → call nodes for each case arm
     */
    record SelectionSwitch(String variable, Map<String, List<CallNode>> branches) implements CallNode {
        public SelectionSwitch {
            Objects.requireNonNull(variable, "variable must not be null");
            Objects.requireNonNull(branches, "branches must not be null");
            branches = Map.copyOf(branches);
        }
    }
}

package com.bica.reborn.reticular;

import com.bica.reborn.ast.*;
import com.bica.reborn.ast.Branch.Choice;
import com.bica.reborn.parser.PrettyPrinter;
import com.bica.reborn.statespace.StateSpace;
import com.bica.reborn.statespace.TransitionKind;

import java.util.*;
import java.util.stream.Collectors;

/**
 * Reticular form: characterisation and reconstruction (Step 9).
 *
 * <p>A finite bounded lattice is a <i>reticulate</i> if it arises as L(S) for
 * some session type S. This class provides:
 * <ol>
 *   <li>{@link #reconstruct(StateSpace)} — recover a session type AST from a state space
 *       (inverse of StateSpaceBuilder.build)</li>
 *   <li>{@link #isReticulate(StateSpace)} — check whether a state space has reticular form</li>
 *   <li>{@link #checkReticularForm(StateSpace)} — detailed analysis with result object</li>
 * </ol>
 *
 * <p>Reticular form requires that every non-bottom state is either:
 * <ul>
 *   <li>a <b>branch</b> node: all outgoing transitions are method calls</li>
 *   <li>a <b>select</b> node: all outgoing transitions are selections</li>
 *   <li>a <b>product</b> node: mixed transitions (from parallel composition)</li>
 *   <li>an <b>end</b> node: no outgoing transitions</li>
 * </ul>
 */
public final class ReticularChecker {

    private ReticularChecker() {}

    // =========================================================================
    // State classification
    // =========================================================================

    /**
     * Classify a state as branch, select, end, or product.
     */
    public static StateClassification classifyState(StateSpace ss, int state) {
        Set<String> methods = ss.enabledMethods(state);
        Set<String> selections = ss.enabledSelections(state);

        List<String> labels = new ArrayList<>();
        labels.addAll(new TreeSet<>(methods));
        labels.addAll(new TreeSet<>(selections));

        if (methods.isEmpty() && selections.isEmpty()) {
            return new StateClassification(state, "end", List.of());
        } else if (!methods.isEmpty() && selections.isEmpty()) {
            return new StateClassification(state, "branch", labels);
        } else if (methods.isEmpty()) {
            return new StateClassification(state, "select", labels);
        } else {
            // Mixed: both method and selection transitions (product from parallel)
            return new StateClassification(state, "product", labels);
        }
    }

    /**
     * Classify all states in a state space.
     */
    public static List<StateClassification> classifyAllStates(StateSpace ss) {
        return ss.states().stream()
                .sorted()
                .map(s -> classifyState(ss, s))
                .toList();
    }

    // =========================================================================
    // Reconstruction: StateSpace → SessionType
    // =========================================================================

    /**
     * Reconstruct a session type AST from a state space.
     *
     * <p>This is the inverse of {@code StateSpaceBuilder.build()}.
     * Traverses the state space from top to bottom, building the AST:
     * <ul>
     *   <li>States with only method transitions → Branch</li>
     *   <li>States with only selection transitions → Select</li>
     *   <li>States with no transitions → End</li>
     *   <li>Back-edges (to already-visited states) → Rec/Var</li>
     * </ul>
     */
    public static SessionType reconstruct(StateSpace ss) {
        return new Reconstructor(ss).reconstruct();
    }

    private static final class Reconstructor {
        private final StateSpace ss;
        private final Map<Integer, String> varNames = new HashMap<>();
        private int varCounter = 0;
        private final Set<Integer> inProgress = new HashSet<>();
        private final Map<Integer, SessionType> cache = new HashMap<>();

        Reconstructor(StateSpace ss) {
            this.ss = ss;
        }

        SessionType reconstruct() {
            if (ss.top() == ss.bottom() && !ss.transitionsFrom(ss.top()).isEmpty()) {
                return visit(ss.top(), true);
            }
            return visit(ss.top(), false);
        }

        private String freshVar() {
            String name = String.valueOf((char) ('X' + varCounter % 3));
            if (varCounter >= 3) {
                name += varCounter / 3;
            }
            varCounter++;
            return name;
        }

        private SessionType visit(int state, boolean isEntry) {
            // Bottom state → End
            if (state == ss.bottom() && !isEntry && !inProgress.contains(state)) {
                return new End();
            }

            // Already computed → cached
            if (cache.containsKey(state)) {
                return cache.get(state);
            }

            // Cycle detected → recursive reference
            if (inProgress.contains(state)) {
                varNames.computeIfAbsent(state, k -> freshVar());
                return new Var(varNames.get(state));
            }

            inProgress.add(state);

            // Collect method and selection transitions with targets
            List<StateSpace.Transition> fromHere = ss.transitionsFrom(state);
            List<StateSpace.Transition> methods = fromHere.stream()
                    .filter(t -> t.kind() == TransitionKind.METHOD)
                    .sorted(Comparator.comparing(StateSpace.Transition::label))
                    .toList();
            List<StateSpace.Transition> selections = fromHere.stream()
                    .filter(t -> t.kind() == TransitionKind.SELECTION)
                    .sorted(Comparator.comparing(StateSpace.Transition::label))
                    .toList();

            SessionType result;
            if (methods.isEmpty() && selections.isEmpty()) {
                result = new End();
            } else if (!methods.isEmpty() && selections.isEmpty()) {
                List<Choice> choices = methods.stream()
                        .map(t -> new Choice(t.label(), visit(t.target(), false)))
                        .toList();
                result = new Branch(choices);
            } else if (methods.isEmpty()) {
                List<Choice> choices = selections.stream()
                        .map(t -> new Choice(t.label(), visit(t.target(), false)))
                        .toList();
                result = new Select(choices);
            } else {
                // Mixed — prefer branch for reconstruction
                List<StateSpace.Transition> all = new ArrayList<>(methods);
                all.addAll(selections);
                all.sort(Comparator.comparing(StateSpace.Transition::label));
                List<Choice> choices = all.stream()
                        .map(t -> new Choice(t.label(), visit(t.target(), false)))
                        .toList();
                result = new Branch(choices);
            }

            inProgress.remove(state);

            // Wrap in Rec if this state has a recursive variable
            if (varNames.containsKey(state)) {
                result = new Rec(varNames.get(state), result);
            }

            cache.put(state, result);
            return result;
        }
    }

    // =========================================================================
    // Reticular form check
    // =========================================================================

    /**
     * Check whether a state space has reticular form.
     */
    public static boolean isReticulate(StateSpace ss) {
        return checkReticularForm(ss).isReticulate();
    }

    /**
     * Check reticular form with detailed analysis.
     */
    public static ReticularFormResult checkReticularForm(StateSpace ss) {
        List<StateClassification> classifications = classifyAllStates(ss);

        try {
            SessionType reconstructed = reconstruct(ss);
            String reconstructedStr = PrettyPrinter.pretty(reconstructed);
            return new ReticularFormResult(true, classifications, null, reconstructedStr);
        } catch (Exception e) {
            return new ReticularFormResult(false, classifications,
                    "reconstruction failed: " + e.getMessage(), null);
        }
    }
}

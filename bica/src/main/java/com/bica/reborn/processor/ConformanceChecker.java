package com.bica.reborn.processor;

import com.bica.reborn.ast.*;
import com.bica.reborn.ast.Branch.Choice;
import com.bica.reborn.statespace.StateSpace;
import com.bica.reborn.statespace.TransitionKind;

import javax.lang.model.element.ElementKind;
import javax.lang.model.element.ExecutableElement;
import javax.lang.model.element.TypeElement;
import javax.lang.model.type.DeclaredType;
import javax.lang.model.type.TypeKind;
import javax.lang.model.type.TypeMirror;
import java.util.*;

/**
 * Checks that a class's method return types conform to the session type's
 * selection labels.
 *
 * <p>When a session type has the pattern {@code m . +{OP1: S1, ..., OPn: Sn}},
 * the method {@code m()} must return a type whose values cover all selection
 * labels {@code {OP1, ..., OPn}}. Supported return types:
 * <ul>
 *   <li><b>Enum</b>: constants must include all selection labels</li>
 *   <li><b>boolean</b>: maps to labels {@code {TRUE, FALSE}}</li>
 * </ul>
 */
public final class ConformanceChecker {

    private ConformanceChecker() {} // utility class

    /**
     * Extracts a mapping from method names to the selection labels that follow them,
     * using the state space (may produce false positives with parallel composition).
     *
     * @param ss the state space to analyze
     * @return method name → set of selection labels
     */
    public static Map<String, Set<String>> extractSelectionMap(StateSpace ss) {
        var result = new LinkedHashMap<String, Set<String>>();

        for (var t : ss.transitions()) {
            if (t.kind() != TransitionKind.METHOD) continue;

            int target = t.target();
            Set<String> selections = ss.enabledSelections(target);
            Set<String> methods = ss.enabledMethods(target);

            // Target state has only selection transitions (no method transitions)
            if (!selections.isEmpty() && methods.isEmpty()) {
                result.merge(t.label(), new LinkedHashSet<>(selections), (old, add) -> {
                    old.addAll(add);
                    return old;
                });
            }
        }

        return result;
    }

    /**
     * Extracts a mapping from method names to selection labels from the AST.
     *
     * <p>Walks the AST looking for the pattern: {@code Branch(m, Select(...))}
     * — a method whose body is directly a selection. This is immune to false
     * positives from product state spaces in parallel composition.
     *
     * @param ast the session type AST
     * @return method name → set of selection labels
     */
    public static Map<String, Set<String>> extractSelectionMapFromAST(SessionType ast) {
        var result = new LinkedHashMap<String, Set<String>>();
        walkAST(ast, result);
        return result;
    }

    private static void walkAST(SessionType type, Map<String, Set<String>> acc) {
        switch (type) {
            case Branch b -> {
                for (Choice c : b.choices()) {
                    if (c.body() instanceof Select sel) {
                        var labels = new LinkedHashSet<String>();
                        for (Choice sc : sel.choices()) labels.add(sc.label());
                        acc.merge(c.label(), labels, (old, add) -> {
                            old.addAll(add);
                            return old;
                        });
                    }
                    walkAST(c.body(), acc);
                }
            }
            case Select s -> {
                for (Choice c : s.choices()) walkAST(c.body(), acc);
            }
            case Parallel p -> {
                walkAST(p.left(), acc);
                walkAST(p.right(), acc);
            }
            case Rec r -> walkAST(r.body(), acc);
            case Sequence seq -> {
                walkAST(seq.left(), acc);
                walkAST(seq.right(), acc);
            }
            case Chain ch -> {
                // T2b: Chain(m, Select(...)) is the T2b analogue of
                // Branch(Choice(m, Select(...))) — record the method-to-selection
                // mapping for conformance checking on return types.
                if (ch.cont() instanceof Select sel) {
                    var labels = new LinkedHashSet<String>();
                    for (Choice sc : sel.choices()) labels.add(sc.label());
                    acc.merge(ch.method(), labels, (old, add) -> {
                        old.addAll(add);
                        return old;
                    });
                }
                walkAST(ch.cont(), acc);
            }
            case End ignored -> {}
            case Var ignored -> {}
        }
    }

    /**
     * Checks that the class's method return types conform to the session type's
     * selection labels. Uses AST-based extraction when available (preferred),
     * falls back to state-space-based extraction.
     *
     * @param ss          the state space built from the session type
     * @param typeElement the class being checked
     * @param ast         the session type AST (optional, pass null for state-space-only)
     * @return list of conformance errors (empty if conformant)
     */
    public static List<ConformanceError> check(StateSpace ss, TypeElement typeElement,
                                                 SessionType ast) {
        Map<String, Set<String>> selectionMap = ast != null
                ? extractSelectionMapFromAST(ast)
                : extractSelectionMap(ss);
        if (selectionMap.isEmpty()) return List.of();

        var errors = new ArrayList<ConformanceError>();

        for (var entry : selectionMap.entrySet()) {
            String methodName = entry.getKey();
            Set<String> expectedLabels = entry.getValue();

            // Find the method in the class
            ExecutableElement method = findMethod(typeElement, methodName);
            if (method == null) {
                errors.add(new ConformanceError(methodName, expectedLabels,
                        "Method '" + methodName + "' declared in session type but not found in class"));
                continue;
            }

            TypeMirror returnType = method.getReturnType();

            if (returnType.getKind() == TypeKind.BOOLEAN) {
                Set<String> boolLabels = Set.of("TRUE", "FALSE");
                if (!boolLabels.equals(expectedLabels)) {
                    errors.add(new ConformanceError(methodName, expectedLabels,
                            "Method '" + methodName + "' returns boolean (covers {TRUE, FALSE}) "
                                    + "but session type expects selection labels " + sorted(expectedLabels)));
                }
            } else if (returnType.getKind() == TypeKind.DECLARED) {
                DeclaredType declaredType = (DeclaredType) returnType;
                TypeElement returnElement = (TypeElement) declaredType.asElement();

                if (returnElement.getKind() != ElementKind.ENUM) {
                    errors.add(new ConformanceError(methodName, expectedLabels,
                            "Method '" + methodName + "' must return an enum or boolean "
                                    + "to select from " + sorted(expectedLabels)));
                    continue;
                }

                // Collect enum constant names
                Set<String> enumConstants = new LinkedHashSet<>();
                for (var enclosed : returnElement.getEnclosedElements()) {
                    if (enclosed.getKind() == ElementKind.ENUM_CONSTANT) {
                        enumConstants.add(enclosed.getSimpleName().toString());
                    }
                }

                // Check coverage
                Set<String> missing = new LinkedHashSet<>(expectedLabels);
                missing.removeAll(enumConstants);
                if (!missing.isEmpty()) {
                    errors.add(new ConformanceError(methodName, expectedLabels,
                            "Method '" + methodName + "' returns enum "
                                    + returnElement.getSimpleName()
                                    + " which is missing selection labels " + sorted(missing)
                                    + " (has " + sorted(enumConstants) + ")"));
                }
            } else {
                errors.add(new ConformanceError(methodName, expectedLabels,
                        "Method '" + methodName + "' must return an enum or boolean "
                                + "to select from " + sorted(expectedLabels)));
            }
        }

        return errors;
    }

    private static ExecutableElement findMethod(TypeElement typeElement, String name) {
        for (var enclosed : typeElement.getEnclosedElements()) {
            if (enclosed.getKind() == ElementKind.METHOD
                    && enclosed.getSimpleName().toString().equals(name)) {
                return (ExecutableElement) enclosed;
            }
        }
        return null;
    }

    private static List<String> sorted(Set<String> set) {
        var list = new ArrayList<>(set);
        Collections.sort(list);
        return list;
    }
}

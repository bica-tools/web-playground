package com.bica.reborn.processor;

import com.bica.reborn.statespace.StateSpace;
import com.bica.reborn.typestate.CallNode;
import com.bica.reborn.typestate.MethodCallExtractor;
import com.bica.reborn.typestate.TypestateChecker;
import com.bica.reborn.typestate.TypestateError;
import com.bica.reborn.typestate.TypestateResult;

import com.sun.source.util.Trees;

import javax.annotation.processing.ProcessingEnvironment;
import javax.annotation.processing.RoundEnvironment;
import javax.lang.model.element.Element;
import javax.lang.model.element.ExecutableElement;
import javax.lang.model.element.TypeElement;
import javax.tools.Diagnostic;
import java.util.*;

/**
 * Isolated Phase B typestate checking logic.
 *
 * <p>This class is loaded reflectively by {@link SessionProcessor} to avoid
 * a compile-time dependency on {@code com.sun.source.util.Trees} in the main
 * processor class. On Eclipse's ECJ compiler (which lacks Trees), this class
 * is never loaded, allowing Phase A to still work.
 */
public final class TypestatePhaseB {

    private TypestatePhaseB() {}

    /**
     * Run typestate checking for all root elements.
     * Called reflectively from SessionProcessor.
     */
    public static void run(ProcessingEnvironment processingEnv,
                           RoundEnvironment roundEnv,
                           Map<String, StateSpace> sessionStateSpaces) {
        Trees trees;
        try {
            trees = Trees.instance(processingEnv);
        } catch (Exception e) {
            return;
        }

        Set<String> sessionTypeNames = sessionStateSpaces.keySet();

        for (Element root : roundEnv.getRootElements()) {
            var extractions = MethodCallExtractor.extract(
                    trees, processingEnv, root, sessionTypeNames);

            for (var entry : extractions.entrySet()) {
                var method = entry.getKey();
                var extraction = entry.getValue();

                for (var varEntry : extraction.trackedVars().entrySet()) {
                    String varName = varEntry.getKey();
                    String typeName = varEntry.getValue();
                    StateSpace ss = sessionStateSpaces.get(typeName);
                    if (ss == null) continue;

                    Set<String> protocolLabels = ss.enabledLabels(ss.top());
                    var allLabels = new HashSet<String>();
                    for (int state : ss.states()) {
                        allLabels.addAll(ss.enabledLabels(state));
                    }

                    List<CallNode> varNodes = filterNodes(extraction.nodes(), varName, allLabels);
                    if (!varNodes.isEmpty()) {
                        TypestateResult result = TypestateChecker.checkCallTree(
                                ss, varNodes, varName, ss.top(), true);
                        for (TypestateError err : result.errors()) {
                            processingEnv.getMessager().printMessage(
                                    Diagnostic.Kind.ERROR,
                                    String.format("Typestate violation in %s.%s(): %s",
                                            ((TypeElement) method.getEnclosingElement()).getQualifiedName(),
                                            method.getSimpleName(), err.message()),
                                    method);
                        }
                    } else {
                        var varCalls = extraction.calls().stream()
                                .filter(c -> c.variable().equals(varName))
                                .filter(c -> allLabels.contains(c.method()))
                                .toList();

                        if (!varCalls.isEmpty()) {
                            TypestateResult result = TypestateChecker.check(
                                    ss, varCalls,
                                    Map.of(varName, ss.top()), true);
                            for (TypestateError err : result.errors()) {
                                processingEnv.getMessager().printMessage(
                                        Diagnostic.Kind.ERROR,
                                        String.format("Typestate violation in %s.%s(): %s",
                                                ((TypeElement) method.getEnclosingElement()).getQualifiedName(),
                                                method.getSimpleName(), err.message()),
                                        method);
                            }
                        }
                    }
                }
            }
        }
    }

    private static List<CallNode> filterNodes(List<CallNode> nodes, String varName,
                                               Set<String> protocolLabels) {
        var result = new ArrayList<CallNode>();
        for (CallNode node : nodes) {
            switch (node) {
                case CallNode.Call c -> {
                    if (c.call().variable().equals(varName)
                            && protocolLabels.contains(c.call().method())) {
                        result.add(c);
                    }
                }
                case CallNode.SelectionSwitch sw -> {
                    var filteredBranches = new LinkedHashMap<String, List<CallNode>>();
                    for (var branch : sw.branches().entrySet()) {
                        filteredBranches.put(branch.getKey(),
                                filterNodes(branch.getValue(), varName, protocolLabels));
                    }
                    boolean hasContent = filteredBranches.values().stream()
                            .anyMatch(b -> !b.isEmpty());
                    if (hasContent) {
                        result.add(new CallNode.SelectionSwitch(sw.variable(), filteredBranches));
                    }
                }
            }
        }
        return result;
    }
}

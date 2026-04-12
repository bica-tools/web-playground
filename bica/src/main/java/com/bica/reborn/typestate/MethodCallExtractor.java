package com.bica.reborn.typestate;

import com.sun.source.tree.*;
import com.sun.source.util.TreePath;
import com.sun.source.util.TreePathScanner;
import com.sun.source.util.Trees;

import javax.annotation.processing.ProcessingEnvironment;
import javax.lang.model.element.Element;
import javax.lang.model.element.ExecutableElement;
import javax.lang.model.element.TypeElement;
import java.util.*;

/**
 * Extracts method calls on {@code @Session}-annotated variables from method bodies.
 *
 * <p>Uses the {@code com.sun.source.util.TreePathScanner} to walk ASTs.
 * This is the only class in the typestate package that depends on the compiler tree API.
 *
 * <p>Extracted information per method body:
 * <ul>
 *   <li>Local variables of {@code @Session}-annotated types created via {@code new}</li>
 *   <li>Method calls on those tracked variables, in source order</li>
 *   <li>Tree-structured call nodes that capture switch-on-enum branching</li>
 * </ul>
 */
public final class MethodCallExtractor {

    private MethodCallExtractor() {}

    /**
     * Result of extraction for a single method body.
     *
     * @param calls          ordered method calls on tracked variables (flat view)
     * @param nodes          tree-structured call nodes (captures switch branches)
     * @param trackedVars    variable names → the @Session type they were assigned from
     */
    public record ExtractionResult(
            List<MethodCall> calls,
            List<CallNode> nodes,
            Map<String, String> trackedVars) {

        public ExtractionResult {
            calls = List.copyOf(calls);
            nodes = List.copyOf(nodes);
            trackedVars = Map.copyOf(trackedVars);
        }

        /** Flatten nodes to a list of MethodCalls (backward-compatible view). */
        public List<MethodCall> flatCalls() {
            return calls;
        }
    }

    /**
     * Extract method calls from all method bodies in the given compilation unit
     * that operate on variables of the given {@code @Session}-annotated types.
     *
     * @param trees          the Trees API instance
     * @param processingEnv  the processing environment
     * @param rootElement    a root element to scan (typically from roundEnv.getRootElements())
     * @param sessionTypes   set of fully-qualified names of @Session-annotated classes
     * @return map from method element → extraction result (only methods with tracked variables)
     */
    public static Map<ExecutableElement, ExtractionResult> extract(
            Trees trees,
            ProcessingEnvironment processingEnv,
            Element rootElement,
            Set<String> sessionTypes) {

        var results = new LinkedHashMap<ExecutableElement, ExtractionResult>();

        if (!(rootElement instanceof TypeElement typeElement)) {
            return results;
        }

        TreePath typePath;
        try {
            typePath = trees.getPath(typeElement);
        } catch (Exception e) {
            // Tree not available (e.g., compiled class) — skip
            return results;
        }
        if (typePath == null) {
            return results;
        }

        // Scan all methods in this type
        for (var enclosed : typeElement.getEnclosedElements()) {
            if (enclosed instanceof ExecutableElement method) {
                TreePath methodPath;
                try {
                    methodPath = trees.getPath(method);
                } catch (Exception e) {
                    continue;
                }
                if (methodPath == null) continue;

                var scanner = new MethodBodyScanner(trees, sessionTypes);
                scanner.scan(methodPath, null);

                if (!scanner.trackedVars.isEmpty()) {
                    results.put(method, new ExtractionResult(
                            scanner.calls, scanner.topNodes, scanner.trackedVars));
                }
            }
        }

        return results;
    }

    /**
     * Scanner that walks a single method body to find tracked variable declarations,
     * method calls on those variables, and switch statements on selection return values.
     */
    private static final class MethodBodyScanner extends TreePathScanner<Void, Void> {

        private final Trees trees;
        private final Set<String> sessionTypes;

        /** Variable name → @Session type name. */
        final Map<String, String> trackedVars = new LinkedHashMap<>();

        /** Ordered method calls on tracked variables (flat list). */
        final List<MethodCall> calls = new ArrayList<>();

        /** Tree-structured call nodes (top-level list). */
        final List<CallNode> topNodes = new ArrayList<>();

        /** Stack of node lists — top is the current context for adding nodes. */
        private final Deque<List<CallNode>> nodeStack = new ArrayDeque<>();

        /** Return variable → tracked object variable name (e.g., "r" → "atm"). */
        private final Map<String, String> returnVarToTracked = new LinkedHashMap<>();

        MethodBodyScanner(Trees trees, Set<String> sessionTypes) {
            this.trees = trees;
            this.sessionTypes = sessionTypes;
            nodeStack.push(topNodes);
        }

        @Override
        public Void visitVariable(VariableTree node, Void unused) {
            ExpressionTree init = node.getInitializer();

            // Check if initialized with `new SessionType()`
            if (init instanceof NewClassTree newClass) {
                String typeName = newClass.getIdentifier().toString();
                if (sessionTypes.contains(typeName) || isSessionType(typeName)) {
                    trackedVars.put(node.getName().toString(), typeName);
                }
            }

            // Check if the variable type itself is a @Session type
            Tree typeTree = node.getType();
            if (typeTree != null) {
                String typeName = typeTree.toString();
                if (sessionTypes.contains(typeName) || isSessionType(typeName)) {
                    if (!(init instanceof NewClassTree) && init != null) {
                        trackedVars.put(node.getName().toString(), typeName);
                    }
                }
            }

            // Track return variables: `EnumType r = tracked.method()`
            if (init instanceof MethodInvocationTree invocation) {
                ExpressionTree select = invocation.getMethodSelect();
                if (select instanceof MemberSelectTree ms) {
                    String receiver = ms.getExpression().toString();
                    if (trackedVars.containsKey(receiver)) {
                        returnVarToTracked.put(node.getName().toString(), receiver);
                    }
                }
            }

            return super.visitVariable(node, unused);
        }

        @Override
        public Void visitMethodInvocation(MethodInvocationTree node, Void unused) {
            ExpressionTree select = node.getMethodSelect();
            if (select instanceof MemberSelectTree memberSelect) {
                ExpressionTree receiver = memberSelect.getExpression();
                String varName = receiver.toString();
                String methodName = memberSelect.getIdentifier().toString();

                if (trackedVars.containsKey(varName)) {
                    long pos = trees.getSourcePositions() != null
                            ? trees.getSourcePositions().getStartPosition(
                                    getCurrentPath().getCompilationUnit(), node)
                            : 0;
                    var mc = new MethodCall(varName, methodName, pos);
                    calls.add(mc);
                    nodeStack.peek().add(new CallNode.Call(mc));
                }
            }
            return super.visitMethodInvocation(node, unused);
        }

        @Override
        public Void visitSwitch(SwitchTree node, Void unused) {
            // Check if the switch selector is a tracked return variable
            String selectorVar = getSelectorName(node.getExpression());
            if (selectorVar != null && returnVarToTracked.containsKey(selectorVar)) {
                // Build a SelectionSwitch node
                var branches = new LinkedHashMap<String, List<CallNode>>();

                for (CaseTree caseTree : node.getCases()) {
                    String label = getCaseLabel(caseTree);
                    if (label == null) continue; // default case or unparseable

                    var branchNodes = new ArrayList<CallNode>();
                    nodeStack.push(branchNodes);

                    // Scan the case body
                    if (caseTree.getBody() != null) {
                        scan(caseTree.getBody(), null);
                    } else if (caseTree.getStatements() != null) {
                        for (StatementTree stmt : caseTree.getStatements()) {
                            scan(stmt, null);
                        }
                    }

                    nodeStack.pop();
                    branches.put(label, branchNodes);
                }

                nodeStack.peek().add(new CallNode.SelectionSwitch(selectorVar, branches));
                return null; // Don't call super — we handled children manually
            }

            return super.visitSwitch(node, unused);
        }

        private String getSelectorName(ExpressionTree expr) {
            if (expr instanceof IdentifierTree id) {
                return id.getName().toString();
            }
            // Handle parenthesized expressions
            if (expr instanceof ParenthesizedTree paren) {
                return getSelectorName(paren.getExpression());
            }
            return null;
        }

        private String getCaseLabel(CaseTree caseTree) {
            // Try getExpressions() first (works for traditional and arrow-style)
            try {
                @SuppressWarnings("deprecation")
                var expressions = caseTree.getExpressions();
                if (expressions != null && !expressions.isEmpty()) {
                    ExpressionTree expr = expressions.get(0);
                    if (expr instanceof IdentifierTree id) {
                        return id.getName().toString();
                    }
                    return expr.toString();
                }
            } catch (Exception ignored) {
                // Fall through to other approaches
            }

            // Try getLabels() (Java 21+)
            try {
                var labels = caseTree.getLabels();
                if (labels != null) {
                    for (var label : labels) {
                        String s = label.toString();
                        if (!s.equals("default")) {
                            return s;
                        }
                    }
                }
            } catch (Exception ignored) {
                // Fall through
            }

            return null;
        }

        private boolean isSessionType(String name) {
            for (String st : sessionTypes) {
                if (st.equals(name) || st.endsWith("." + name)) {
                    return true;
                }
            }
            return false;
        }
    }
}

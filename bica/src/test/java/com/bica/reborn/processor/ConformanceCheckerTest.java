package com.bica.reborn.processor;

import com.bica.reborn.ast.SessionType;
import com.bica.reborn.parser.Parser;
import com.bica.reborn.statespace.StateSpace;
import com.bica.reborn.statespace.StateSpaceBuilder;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import javax.tools.*;
import java.io.StringWriter;
import java.net.URI;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Tests for {@link ConformanceChecker}.
 */
class ConformanceCheckerTest {

    // -- test infrastructure (same as SessionProcessorTest) --------------------

    private static class InMemorySource extends SimpleJavaFileObject {
        private final String code;

        InMemorySource(String className, String code) {
            super(URI.create("string:///" + className.replace('.', '/') + Kind.SOURCE.extension),
                    Kind.SOURCE);
            this.code = code;
        }

        @Override
        public CharSequence getCharContent(boolean ignoreEncodingErrors) {
            return code;
        }
    }

    private record CompileResult(boolean success, String diagnostics) {}

    private static CompileResult compile(String className, String code) {
        JavaCompiler compiler = ToolProvider.getSystemJavaCompiler();
        assertNotNull(compiler, "No system Java compiler available");

        var diagnostics = new DiagnosticCollector<JavaFileObject>();
        var source = new InMemorySource(className, code);

        var task = compiler.getTask(
                new StringWriter(), null, diagnostics,
                List.of("-proc:only"), null, List.of(source));

        task.setProcessors(List.of(new SessionProcessor()));
        boolean success = task.call();

        var sb = new StringBuilder();
        for (var d : diagnostics.getDiagnostics()) {
            sb.append(d.getKind()).append(": ")
                    .append(d.getMessage(Locale.ROOT)).append("\n");
        }

        return new CompileResult(success, sb.toString());
    }

    private static final String IMPORTS =
            "import com.bica.reborn.annotation.*;\n";

    private static StateSpace buildStateSpace(String typeString) {
        SessionType ast = Parser.parse(typeString);
        return StateSpaceBuilder.build(ast);
    }

    // =========================================================================
    // Unit tests for extractSelectionMap
    // =========================================================================

    @Nested
    class SelectionMapTests {

        @Test
        void simpleSelection() {
            StateSpace ss = buildStateSpace("m . +{A: end, B: end}");
            Map<String, Set<String>> map = ConformanceChecker.extractSelectionMap(ss);

            assertEquals(1, map.size());
            assertTrue(map.containsKey("m"));
            assertEquals(Set.of("A", "B"), map.get("m"));
        }

        @Test
        void noSelection() {
            StateSpace ss = buildStateSpace("a . b . end");
            Map<String, Set<String>> map = ConformanceChecker.extractSelectionMap(ss);

            assertTrue(map.isEmpty());
        }

        @Test
        void multipleSelections() {
            StateSpace ss = buildStateSpace(
                    "&{enterPIN: +{AUTH: &{withdraw: +{OK: end, INSUFFICIENT: end}}, REJECTED: end}}");
            Map<String, Set<String>> map = ConformanceChecker.extractSelectionMap(ss);

            assertEquals(2, map.size());
            assertEquals(Set.of("AUTH", "REJECTED"), map.get("enterPIN"));
            assertEquals(Set.of("OK", "INSUFFICIENT"), map.get("withdraw"));
        }

        @Test
        void branchThenSelect() {
            StateSpace ss = buildStateSpace("&{open: +{OK: end, ERR: end}}");
            Map<String, Set<String>> map = ConformanceChecker.extractSelectionMap(ss);

            assertEquals(1, map.size());
            assertEquals(Set.of("OK", "ERR"), map.get("open"));
        }

        @Test
        void recursiveSelection() {
            StateSpace ss = buildStateSpace("rec X . m . +{A: X, B: end}");
            Map<String, Set<String>> map = ConformanceChecker.extractSelectionMap(ss);

            assertEquals(1, map.size());
            assertEquals(Set.of("A", "B"), map.get("m"));
        }
    }

    // =========================================================================
    // Integration tests via compiler
    // =========================================================================

    @Nested
    class IntegrationTests {

        @Test
        void voidMethodWithSelection_fails() {
            var r = compile("TestVoid", IMPORTS
                    + "@Session(\"m . +{A: end, B: end}\")\n"
                    + "class TestVoid {\n"
                    + "    void m() {}\n"
                    + "}\n");
            assertFalse(r.success(), "Should fail: void method with selection");
            assertTrue(r.diagnostics().contains("Conformance"),
                    "Should mention conformance: " + r.diagnostics());
            assertTrue(r.diagnostics().contains("enum or boolean"),
                    "Should mention expected types: " + r.diagnostics());
        }

        @Test
        void enumMethodWithSelection_passes() {
            var r = compile("TestEnum", IMPORTS
                    + "@Session(\"m . +{A: end, B: end}\")\n"
                    + "class TestEnum {\n"
                    + "    enum MResult { A, B }\n"
                    + "    MResult m() { return MResult.A; }\n"
                    + "}\n");
            assertTrue(r.success(), "Should succeed: " + r.diagnostics());
        }

        @Test
        void booleanMethodWithSelection_passes() {
            var r = compile("TestBool", IMPORTS
                    + "@Session(\"m . +{TRUE: end, FALSE: end}\")\n"
                    + "class TestBool {\n"
                    + "    boolean m() { return true; }\n"
                    + "}\n");
            assertTrue(r.success(), "Should succeed: " + r.diagnostics());
        }

        @Test
        void enumMissingLabel_fails() {
            var r = compile("TestMissing", IMPORTS
                    + "@Session(\"m . +{A: end, B: end}\")\n"
                    + "class TestMissing {\n"
                    + "    enum MResult { A }\n"
                    + "    MResult m() { return MResult.A; }\n"
                    + "}\n");
            assertFalse(r.success(), "Should fail: enum missing label B");
            assertTrue(r.diagnostics().contains("Conformance"),
                    "Should mention conformance: " + r.diagnostics());
            assertTrue(r.diagnostics().contains("B"),
                    "Should mention missing label B: " + r.diagnostics());
        }

        @Test
        void wrongReturnType_fails() {
            var r = compile("TestWrong", IMPORTS
                    + "@Session(\"m . +{A: end, B: end}\")\n"
                    + "class TestWrong {\n"
                    + "    int m() { return 0; }\n"
                    + "}\n");
            assertFalse(r.success(), "Should fail: int return type with selection");
            assertTrue(r.diagnostics().contains("Conformance"),
                    "Should mention conformance: " + r.diagnostics());
            assertTrue(r.diagnostics().contains("enum or boolean"),
                    "Should mention expected types: " + r.diagnostics());
        }

        @Test
        void booleanWithNonBoolLabels_fails() {
            var r = compile("TestBoolWrong", IMPORTS
                    + "@Session(\"m . +{A: end, B: end}\")\n"
                    + "class TestBoolWrong {\n"
                    + "    boolean m() { return true; }\n"
                    + "}\n");
            assertFalse(r.success(), "Should fail: boolean with non-TRUE/FALSE labels");
            assertTrue(r.diagnostics().contains("Conformance"),
                    "Should mention conformance: " + r.diagnostics());
        }

        @Test
        void methodNotFound_fails() {
            var r = compile("TestNoMethod", IMPORTS
                    + "@Session(\"m . +{A: end, B: end}\")\n"
                    + "class TestNoMethod {\n"
                    + "}\n");
            assertFalse(r.success(), "Should fail: method not found");
            assertTrue(r.diagnostics().contains("Conformance"),
                    "Should mention conformance: " + r.diagnostics());
            assertTrue(r.diagnostics().contains("not found"),
                    "Should mention not found: " + r.diagnostics());
        }

        @Test
        void enumWithExtraConstants_passes() {
            // Enum has more constants than needed — that's fine
            var r = compile("TestExtra", IMPORTS
                    + "@Session(\"m . +{A: end, B: end}\")\n"
                    + "class TestExtra {\n"
                    + "    enum MResult { A, B, C }\n"
                    + "    MResult m() { return MResult.A; }\n"
                    + "}\n");
            assertTrue(r.success(), "Should succeed with extra enum constants: " + r.diagnostics());
        }

        @Test
        void multipleSelectionsAllConform() {
            var r = compile("TestMulti", IMPORTS
                    + "@Session(\"&{enterPIN: +{AUTH: &{withdraw: +{OK: end, FAIL: end}}, REJECTED: end}}\")\n"
                    + "class TestMulti {\n"
                    + "    enum PinResult { AUTH, REJECTED }\n"
                    + "    PinResult enterPIN() { return PinResult.AUTH; }\n"
                    + "    enum WithdrawResult { OK, FAIL }\n"
                    + "    WithdrawResult withdraw() { return WithdrawResult.OK; }\n"
                    + "}\n");
            assertTrue(r.success(), "Should succeed: " + r.diagnostics());
        }
    }
}

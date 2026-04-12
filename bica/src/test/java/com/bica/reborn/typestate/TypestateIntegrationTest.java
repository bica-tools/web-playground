package com.bica.reborn.typestate;

import com.bica.reborn.processor.SessionProcessor;

import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import javax.tools.*;
import java.io.StringWriter;
import java.net.URI;
import java.util.List;
import java.util.Locale;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Integration tests for typestate checking at compile time.
 *
 * <p>Uses the Java compiler API to compile in-memory source files with
 * {@code @Session}-annotated classes and client code, verifying that
 * the annotation processor correctly detects protocol violations.
 */
class TypestateIntegrationTest {

    // -- test infrastructure --------------------------------------------------

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

    private static CompileResult compile(String... classNameAndCodePairs) {
        JavaCompiler compiler = ToolProvider.getSystemJavaCompiler();
        assertNotNull(compiler, "No system Java compiler available");

        var diagnostics = new DiagnosticCollector<JavaFileObject>();
        var sources = new java.util.ArrayList<JavaFileObject>();

        for (int i = 0; i < classNameAndCodePairs.length; i += 2) {
            sources.add(new InMemorySource(classNameAndCodePairs[i], classNameAndCodePairs[i + 1]));
        }

        var task = compiler.getTask(
                new StringWriter(),
                null,
                diagnostics,
                List.of("-proc:only"),
                null,
                sources);

        task.setProcessors(List.of(new SessionProcessor()));
        boolean success = task.call();

        var sb = new StringBuilder();
        for (var d : diagnostics.getDiagnostics()) {
            sb.append(d.getKind()).append(": ")
                    .append(d.getMessage(Locale.ROOT)).append("\n");
        }

        return new CompileResult(success, sb.toString());
    }

    private static final String IMPORTS = "import com.bica.reborn.annotation.*;\n";

    // =========================================================================
    // Correct protocol usage — should compile successfully
    // =========================================================================

    @Nested
    class ValidUsage {

        @Test
        void correctLinearSequence() {
            var r = compile(
                    "FileHandle", IMPORTS
                            + "@Session(\"open . read . close . end\")\n"
                            + "class FileHandle {\n"
                            + "    void open() {}\n"
                            + "    void read() {}\n"
                            + "    void close() {}\n"
                            + "}\n",
                    "Client", IMPORTS
                            + "class Client {\n"
                            + "    void use() {\n"
                            + "        FileHandle f = new FileHandle();\n"
                            + "        f.open();\n"
                            + "        f.read();\n"
                            + "        f.close();\n"
                            + "    }\n"
                            + "}\n");
            assertTrue(r.success(), "Correct sequence should pass: " + r.diagnostics());
        }

        @Test
        void noSessionUsage() {
            // Client that doesn't use any @Session objects should pass
            var r = compile(
                    "Proto", IMPORTS
                            + "@Session(\"done . end\")\n"
                            + "class Proto { void done() {} }\n",
                    "Bystander",
                            "class Bystander {\n"
                            + "    void doStuff() {\n"
                            + "        System.out.println(\"hello\");\n"
                            + "    }\n"
                            + "}\n");
            assertTrue(r.success(), "No session usage should pass: " + r.diagnostics());
        }

        @Test
        void branchChoice() {
            var r = compile(
                    "Chooser", IMPORTS
                            + "@Session(\"&{read: end, write: end}\")\n"
                            + "class Chooser {\n"
                            + "    void read() {}\n"
                            + "    void write() {}\n"
                            + "}\n",
                    "Reader",
                            "class Reader {\n"
                            + "    void go() {\n"
                            + "        Chooser c = new Chooser();\n"
                            + "        c.read();\n"
                            + "    }\n"
                            + "}\n");
            assertTrue(r.success(), "Branch choice should pass: " + r.diagnostics());
        }

        @Test
        void recursiveLoopThenDone() {
            var r = compile(
                    "Iterator", IMPORTS
                            + "@Session(\"rec X . &{next: X, done: end}\")\n"
                            + "class Iterator {\n"
                            + "    void next() {}\n"
                            + "    void done() {}\n"
                            + "}\n",
                    "User",
                            "class User {\n"
                            + "    void go() {\n"
                            + "        Iterator it = new Iterator();\n"
                            + "        it.next();\n"
                            + "        it.next();\n"
                            + "        it.done();\n"
                            + "    }\n"
                            + "}\n");
            assertTrue(r.success(), "Recursive loop then done should pass: " + r.diagnostics());
        }

        @Test
        void twoIndependentVariables() {
            var r = compile(
                    "Conn", IMPORTS
                            + "@Session(\"open . close . end\")\n"
                            + "class Conn {\n"
                            + "    void open() {}\n"
                            + "    void close() {}\n"
                            + "}\n",
                    "TwoConns",
                            "class TwoConns {\n"
                            + "    void go() {\n"
                            + "        Conn a = new Conn();\n"
                            + "        Conn b = new Conn();\n"
                            + "        a.open();\n"
                            + "        b.open();\n"
                            + "        a.close();\n"
                            + "        b.close();\n"
                            + "    }\n"
                            + "}\n");
            assertTrue(r.success(), "Two independent variables should pass: " + r.diagnostics());
        }
    }

    // =========================================================================
    // Protocol violations — should fail
    // =========================================================================

    @Nested
    class Violations {

        @Test
        void wrongOrder() {
            var r = compile(
                    "FileHandle2", IMPORTS
                            + "@Session(\"open . read . close . end\")\n"
                            + "class FileHandle2 {\n"
                            + "    void open() {}\n"
                            + "    void read() {}\n"
                            + "    void close() {}\n"
                            + "}\n",
                    "BadClient",
                            "class BadClient {\n"
                            + "    void use() {\n"
                            + "        FileHandle2 f = new FileHandle2();\n"
                            + "        f.read();\n"
                            + "    }\n"
                            + "}\n");
            assertFalse(r.success(), "Wrong order should fail");
            assertTrue(r.diagnostics().contains("Typestate violation"),
                    "Should mention typestate: " + r.diagnostics());
            assertTrue(r.diagnostics().contains("read"),
                    "Should mention 'read': " + r.diagnostics());
        }

        @Test
        void notClosed() {
            var r = compile(
                    "FileHandle3", IMPORTS
                            + "@Session(\"open . close . end\")\n"
                            + "class FileHandle3 {\n"
                            + "    void open() {}\n"
                            + "    void close() {}\n"
                            + "}\n",
                    "LeakyClient",
                            "class LeakyClient {\n"
                            + "    void use() {\n"
                            + "        FileHandle3 f = new FileHandle3();\n"
                            + "        f.open();\n"
                            + "    }\n"
                            + "}\n");
            assertFalse(r.success(), "Not closed should fail");
            assertTrue(r.diagnostics().contains("terminal"),
                    "Should mention terminal state: " + r.diagnostics());
        }

        @Test
        void callAfterClose() {
            var r = compile(
                    "FileHandle4", IMPORTS
                            + "@Session(\"open . close . end\")\n"
                            + "class FileHandle4 {\n"
                            + "    void open() {}\n"
                            + "    void close() {}\n"
                            + "}\n",
                    "DoubleClient",
                            "class DoubleClient {\n"
                            + "    void use() {\n"
                            + "        FileHandle4 f = new FileHandle4();\n"
                            + "        f.open();\n"
                            + "        f.close();\n"
                            + "        f.open();\n"
                            + "    }\n"
                            + "}\n");
            assertFalse(r.success(), "Call after close should fail");
            assertTrue(r.diagnostics().contains("not enabled"),
                    "Should mention not enabled: " + r.diagnostics());
        }

        @Test
        void oneValidOneInvalid() {
            var r = compile(
                    "Conn2", IMPORTS
                            + "@Session(\"open . close . end\")\n"
                            + "class Conn2 {\n"
                            + "    void open() {}\n"
                            + "    void close() {}\n"
                            + "}\n",
                    "MixedClient",
                            "class MixedClient {\n"
                            + "    void go() {\n"
                            + "        Conn2 a = new Conn2();\n"
                            + "        Conn2 b = new Conn2();\n"
                            + "        a.open();\n"
                            + "        b.close();\n"
                            + "        a.close();\n"
                            + "    }\n"
                            + "}\n");
            assertFalse(r.success(), "One invalid variable should fail");
            assertTrue(r.diagnostics().contains("Typestate violation"),
                    "Should report typestate error: " + r.diagnostics());
        }
    }

    // =========================================================================
    // Edge cases
    // =========================================================================

    @Nested
    class EdgeCases {

        @Test
        void endSessionNoMethods() {
            var r = compile(
                    "Done", IMPORTS
                            + "@Session(\"end\")\n"
                            + "class Done {}\n",
                    "EndClient",
                            "class EndClient {\n"
                            + "    void use() {\n"
                            + "        Done d = new Done();\n"
                            + "    }\n"
                            + "}\n");
            // Creating an 'end' session object requires no calls
            assertTrue(r.success(), "End session should pass: " + r.diagnostics());
        }

        @Test
        void sessionClassWithSelfUsage() {
            // Methods within the @Session class itself — not client usage
            var r = compile(
                    "SelfUser", IMPORTS
                            + "@Session(\"open . close . end\")\n"
                            + "class SelfUser {\n"
                            + "    void open() {}\n"
                            + "    void close() {}\n"
                            + "}\n");
            assertTrue(r.success(), "Session class without client usage should pass: " + r.diagnostics());
        }

        @Test
        void selectionProtocol() {
            var r = compile(
                    "Select", IMPORTS
                            + "@Session(\"+{OK: done . end, ERR: end}\")\n"
                            + "class Select {\n"
                            + "    void done() {}\n"
                            + "}\n",
                    "SelectUser",
                            "class SelectUser {\n"
                            + "    void go() {\n"
                            + "        Select s = new Select();\n"
                            + "        s.OK();\n"
                            + "        s.done();\n"
                            + "    }\n"
                            + "}\n");
            assertTrue(r.success(), "Selection protocol should pass: " + r.diagnostics());
        }
    }
}

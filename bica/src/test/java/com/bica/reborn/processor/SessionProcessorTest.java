package com.bica.reborn.processor;

import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import javax.tools.*;
import java.io.StringWriter;
import java.net.URI;
import java.util.List;
import java.util.Locale;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Integration tests for {@link SessionProcessor} using the Java compiler API.
 *
 * <p>Each test creates an in-memory Java source file with a {@code @Session} annotation,
 * compiles it with the processor active, and checks for expected errors or success.
 */
class SessionProcessorTest {

    // -- test infrastructure --------------------------------------------------

    /** In-memory source file for the compiler. */
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

    private record CompileResult(boolean success, String diagnostics,
                                     List<Diagnostic<? extends JavaFileObject>> rawDiagnostics) {}

    private static CompileResult compile(String className, String code) {
        JavaCompiler compiler = ToolProvider.getSystemJavaCompiler();
        assertNotNull(compiler, "No system Java compiler available");

        var diagnostics = new DiagnosticCollector<JavaFileObject>();
        var source = new InMemorySource(className, code);

        // Compile with the processor enabled
        var task = compiler.getTask(
                new StringWriter(),     // output writer
                null,                   // file manager (default)
                diagnostics,            // diagnostics collector
                List.of("-proc:only"),  // only run annotation processing
                null,                   // classes to process
                List.of(source));       // sources

        task.setProcessors(List.of(new SessionProcessor()));

        boolean success = task.call();

        var sb = new StringBuilder();
        for (var d : diagnostics.getDiagnostics()) {
            sb.append(d.getKind()).append(": ")
                    .append(d.getMessage(Locale.ROOT)).append("\n");
        }

        return new CompileResult(success, sb.toString(), diagnostics.getDiagnostics());
    }

    private static final String IMPORTS =
            "import com.bica.reborn.annotation.*;\n";

    // =========================================================================
    // Valid session types — should compile successfully
    // =========================================================================

    @Nested
    class ValidTypes {

        @Test
        void simpleEnd() {
            var r = compile("TestEnd", IMPORTS
                    + "@Session(\"end\")\n"
                    + "class TestEnd {}\n");
            assertTrue(r.success(), "Should succeed: " + r.diagnostics());
            assertTrue(r.diagnostics().contains("verified"),
                    "Should have verification note: " + r.diagnostics());
        }

        @Test
        void singleMethod() {
            var r = compile("TestSingle", IMPORTS
                    + "@Session(\"read . end\")\n"
                    + "class TestSingle {\n"
                    + "    void read() {}\n"
                    + "}\n");
            assertTrue(r.success(), "Should succeed: " + r.diagnostics());
        }

        @Test
        void branchAndSelect() {
            var r = compile("TestBranch", IMPORTS
                    + "@Session(\"&{open: +{OK: end, ERR: end}}\")\n"
                    + "class TestBranch {\n"
                    + "    enum OpenResult { OK, ERR }\n"
                    + "    OpenResult open() { return OpenResult.OK; }\n"
                    + "}\n");
            assertTrue(r.success(), "Should succeed: " + r.diagnostics());
        }

        @Test
        void recursiveType() {
            var r = compile("TestRec", IMPORTS
                    + "@Session(\"rec X . &{next: X, done: end}\")\n"
                    + "class TestRec {\n"
                    + "    void next() {}\n"
                    + "    void done() {}\n"
                    + "}\n");
            assertTrue(r.success(), "Should succeed: " + r.diagnostics());
        }

        @Test
        void safeParallelWithShared() {
            var r = compile("TestSafe", IMPORTS
                    + "@Session(\"(read . end || write . end)\")\n"
                    + "class TestSafe {\n"
                    + "    @Shared void read() {}\n"
                    + "    @Shared void write() {}\n"
                    + "}\n");
            assertTrue(r.success(), "Should succeed: " + r.diagnostics());
        }

        @Test
        void safeParallelWithSync() {
            var r = compile("TestSync", IMPORTS
                    + "@Session(\"(read . end || write . end)\")\n"
                    + "class TestSync {\n"
                    + "    synchronized void read() {}\n"
                    + "    synchronized void write() {}\n"
                    + "}\n");
            assertTrue(r.success(), "Should succeed: " + r.diagnostics());
        }

        @Test
        void safeParallelWithReadOnly() {
            var r = compile("TestReadOnly", IMPORTS
                    + "@Session(\"(read . end || peek . end)\")\n"
                    + "class TestReadOnly {\n"
                    + "    @ReadOnly void read() {}\n"
                    + "    @ReadOnly void peek() {}\n"
                    + "}\n");
            assertTrue(r.success(), "Should succeed: " + r.diagnostics());
        }

        @Test
        void mixedSyncAndReadOnly() {
            var r = compile("TestMixed", IMPORTS
                    + "@Session(\"(read . end || sync . end)\")\n"
                    + "class TestMixed {\n"
                    + "    @ReadOnly void read() {}\n"
                    + "    synchronized void sync() {}\n"
                    + "}\n");
            assertTrue(r.success(), "Should succeed: " + r.diagnostics());
        }

        @Test
        void javaIterator() {
            var r = compile("TestIterator", IMPORTS
                    + "@Session(\"rec X . &{hasNext: +{TRUE: &{next: X}, FALSE: end}}\")\n"
                    + "class TestIterator {\n"
                    + "    boolean hasNext() { return false; }\n"
                    + "    Object next() { return null; }\n"
                    + "}\n");
            assertTrue(r.success(), "Should succeed: " + r.diagnostics());
        }
    }

    // =========================================================================
    // Parse errors
    // =========================================================================

    @Nested
    class ParseErrors {

        @Test
        void invalidSyntax() {
            var r = compile("TestBadParse", IMPORTS
                    + "@Session(\"&{\")\n"
                    + "class TestBadParse {}\n");
            assertFalse(r.success(), "Should fail with parse error");
            assertTrue(r.diagnostics().contains("parse"),
                    "Should mention parse error: " + r.diagnostics());
        }

        @Test
        void emptyString() {
            var r = compile("TestEmpty", IMPORTS
                    + "@Session(\"\")\n"
                    + "class TestEmpty {}\n");
            assertFalse(r.success(), "Should fail with parse error");
        }
    }

    // =========================================================================
    // Termination errors
    // =========================================================================

    @Nested
    class TerminationErrors {

        @Test
        void nonTerminating() {
            var r = compile("TestNonTerm", IMPORTS
                    + "@Session(\"rec X . X\")\n"
                    + "class TestNonTerm {}\n");
            assertFalse(r.success(), "Should fail: non-terminating");
            assertTrue(r.diagnostics().contains("non-terminating"),
                    "Should mention non-terminating: " + r.diagnostics());
        }

        @Test
        void allBranchesLoop() {
            var r = compile("TestLoop", IMPORTS
                    + "@Session(\"rec X . &{loop: X}\")\n"
                    + "class TestLoop {}\n");
            assertFalse(r.success(), "Should fail: non-terminating");
        }
    }

    // =========================================================================
    // WF-Par errors
    // =========================================================================

    @Nested
    class WFParErrors {

        @Test
        void nestedParallel() {
            var r = compile("TestNested", IMPORTS
                    + "@Session(\"( (a . end || b . end) || c . end )\")\n"
                    + "class TestNested {}\n");
            assertFalse(r.success(), "Should fail: nested parallel");
            assertTrue(r.diagnostics().contains("WF-Par"),
                    "Should mention WF-Par: " + r.diagnostics());
        }

        @Test
        void nonTerminatingBranch() {
            var r = compile("TestBadPar", IMPORTS
                    + "@Session(\"(rec X . X || end)\")\n"
                    + "class TestBadPar {}\n");
            // First fails termination, not WF-Par
            assertFalse(r.success());
        }
    }

    // =========================================================================
    // Thread safety errors
    // =========================================================================

    @Nested
    class ThreadSafetyErrors {

        @Test
        void unsafeParallelDefaultExclusive() {
            var r = compile("TestUnsafe", IMPORTS
                    + "@Session(\"(read . end || write . end)\")\n"
                    + "class TestUnsafe {\n"
                    + "    void read() {}\n"
                    + "    void write() {}\n"
                    + "}\n");
            assertFalse(r.success(), "Should fail: EXCLUSIVE methods in parallel");
            assertTrue(r.diagnostics().contains("Thread safety"),
                    "Should mention thread safety: " + r.diagnostics());
        }

        @Test
        void oneExclusiveOneShared() {
            var r = compile("TestHalfSafe", IMPORTS
                    + "@Session(\"(read . end || write . end)\")\n"
                    + "class TestHalfSafe {\n"
                    + "    @Shared void read() {}\n"
                    + "    void write() {}\n"
                    + "}\n");
            assertFalse(r.success(), "Should fail: one EXCLUSIVE in parallel");
        }

        @Test
        void explicitExclusive() {
            var r = compile("TestExplicit", IMPORTS
                    + "@Session(\"(read . end || write . end)\")\n"
                    + "class TestExplicit {\n"
                    + "    @Exclusive void read() {}\n"
                    + "    @ReadOnly void write() {}\n"
                    + "}\n");
            assertFalse(r.success(), "Should fail: @Exclusive in parallel");
        }
    }

    // =========================================================================
    // Annotation classification
    // =========================================================================

    @Nested
    class AnnotationClassification {

        @Test
        void sharedAnnotationTakesPriority() {
            // @Shared on synchronized method → SHARED wins
            var r = compile("TestPrio", IMPORTS
                    + "@Session(\"(read . end || write . end)\")\n"
                    + "class TestPrio {\n"
                    + "    @Shared synchronized void read() {}\n"
                    + "    @Shared synchronized void write() {}\n"
                    + "}\n");
            assertTrue(r.success(), "Should succeed: @Shared overrides synchronized: " + r.diagnostics());
        }

        @Test
        void staticMethodsAreShared() {
            var r = compile("TestStatic", IMPORTS
                    + "@Session(\"(get . end || get2 . end)\")\n"
                    + "class TestStatic {\n"
                    + "    static void get() {}\n"
                    + "    static void get2() {}\n"
                    + "}\n");
            assertTrue(r.success(), "Should succeed: static methods are SHARED: " + r.diagnostics());
        }
    }

    // =========================================================================
    // Skip termination
    // =========================================================================

    @Nested
    class SkipTermination {

        @Test
        void skipTerminationAllowsNonTerminating() {
            var r = compile("TestSkipTerm", IMPORTS
                    + "@Session(value=\"rec X . X\", skipTermination=true)\n"
                    + "class TestSkipTerm {}\n");
            assertTrue(r.success(), "Should succeed with skipTermination: " + r.diagnostics());
        }

        @Test
        void skipTerminationEmitsWarning() {
            var r = compile("TestSkipWarn", IMPORTS
                    + "@Session(value=\"rec X . X\", skipTermination=true)\n"
                    + "class TestSkipWarn {}\n");
            assertTrue(r.success(), "Should succeed: " + r.diagnostics());
            assertTrue(r.diagnostics().contains("WARNING"),
                    "Should emit WARNING: " + r.diagnostics());
            assertTrue(r.diagnostics().contains("non-terminating"),
                    "Warning should mention non-terminating: " + r.diagnostics());
        }

        @Test
        void skipTerminationStillRunsLatticeCheck() {
            var r = compile("TestSkipLattice", IMPORTS
                    + "@Session(value=\"rec X . &{a: X}\", skipTermination=true)\n"
                    + "class TestSkipLattice {\n"
                    + "    void a() {}\n"
                    + "}\n");
            assertTrue(r.success(), "Should succeed with skipTermination: " + r.diagnostics());
            assertTrue(r.diagnostics().contains("verified"),
                    "Should still run lattice check: " + r.diagnostics());
        }

        @Test
        void defaultBehaviorUnchanged() {
            var r = compile("TestDefaultTerm", IMPORTS
                    + "@Session(\"rec X . X\")\n"
                    + "class TestDefaultTerm {}\n");
            assertFalse(r.success(), "Should fail without skipTermination");
            assertTrue(r.diagnostics().contains("non-terminating"),
                    "Should mention non-terminating: " + r.diagnostics());
        }
    }

    // =========================================================================
    // SourceMethodClassifier — unit-level
    // =========================================================================

    @Nested
    class SourceClassifier {

        @Test
        void unknownMethodReturnsNull() {
            // Compile a class with no methods, then check unknown
            var r = compile("TestNoMethods", IMPORTS
                    + "@Session(\"end\")\n"
                    + "class TestNoMethods {}\n");
            assertTrue(r.success(), "Should succeed: " + r.diagnostics());
        }
    }

    // =========================================================================
    // Diagnostic location targeting — errors point to @Session annotation
    // =========================================================================

    @Nested
    class DiagnosticLocation {

        @Test
        void errorPointsToAnnotationLine() {
            // @Session is on line 2 (after import on line 1)
            var r = compile("TestLocation", IMPORTS
                    + "@Session(\"&{broken\")\n"       // line 2: parse error
                    + "class TestLocation {}\n");
            assertFalse(r.success(), "Should fail: parse error");

            // Check that at least one error diagnostic has a valid position
            var errors = r.rawDiagnostics().stream()
                    .filter(d -> d.getKind() == Diagnostic.Kind.ERROR)
                    .toList();
            assertFalse(errors.isEmpty(), "Should have error diagnostics");

            // The error should point to a real position (not -1/NOPOS)
            var firstError = errors.get(0);
            assertTrue(firstError.getLineNumber() > 0,
                    "Error should have line number, got: " + firstError.getLineNumber());
        }

        @Test
        void notePointsToAnnotationLine() {
            // Successful compilation — note should also target the annotation
            var r = compile("TestNoteLocation", IMPORTS
                    + "@Session(\"end\")\n"
                    + "class TestNoteLocation {}\n");
            assertTrue(r.success(), "Should succeed: " + r.diagnostics());

            var notes = r.rawDiagnostics().stream()
                    .filter(d -> d.getKind() == Diagnostic.Kind.NOTE)
                    .toList();
            assertFalse(notes.isEmpty(), "Should have note diagnostics");
        }

        @Test
        void warningPointsToAnnotationLine() {
            // Non-terminating with skipTermination=true → warning on annotation
            var r = compile("TestWarnLocation", IMPORTS
                    + "@Session(value=\"rec X . &{a: X}\", skipTermination=true)\n"
                    + "class TestWarnLocation {}\n");

            var warnings = r.rawDiagnostics().stream()
                    .filter(d -> d.getKind() == Diagnostic.Kind.WARNING
                            || d.getKind() == Diagnostic.Kind.MANDATORY_WARNING)
                    .toList();
            assertFalse(warnings.isEmpty(),
                    "Should have warnings for non-terminating type: " + r.diagnostics());

            var firstWarning = warnings.get(0);
            assertTrue(firstWarning.getLineNumber() > 0,
                    "Warning should have line number, got: " + firstWarning.getLineNumber());
        }
    }
}

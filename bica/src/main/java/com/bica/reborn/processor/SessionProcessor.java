package com.bica.reborn.processor;

import com.bica.reborn.annotation.Session;
import com.bica.reborn.concurrency.ThreadSafetyChecker;
import com.bica.reborn.concurrency.ThreadSafetyResult;
import com.bica.reborn.lattice.LatticeChecker;
import com.bica.reborn.lattice.LatticeResult;
import com.bica.reborn.parser.ParseError;
import com.bica.reborn.parser.Parser;
import com.bica.reborn.ast.SessionType;
import com.bica.reborn.statespace.StateSpace;
import com.bica.reborn.statespace.StateSpaceBuilder;
import com.bica.reborn.termination.TerminationChecker;
import com.bica.reborn.termination.TerminationResult;
import com.bica.reborn.termination.WFParallelResult;

import com.bica.reborn.cli.ProtocolRegistry;

import javax.annotation.processing.AbstractProcessor;
import javax.annotation.processing.RoundEnvironment;
import javax.annotation.processing.SupportedAnnotationTypes;
import javax.annotation.processing.SupportedSourceVersion;
import javax.lang.model.SourceVersion;
import javax.lang.model.element.AnnotationMirror;
import javax.lang.model.element.Element;
import javax.lang.model.element.TypeElement;
import javax.tools.Diagnostic;
import java.lang.reflect.Method;
import java.util.*;

/**
 * Annotation processor for {@link Session} annotations.
 *
 * <p>For each {@code @Session("...")} class, runs the full BICA pipeline:
 * <ol>
 *   <li>Parse the session type string.</li>
 *   <li>Check termination of recursive types.</li>
 *   <li>Check WF-Par well-formedness of parallel compositions.</li>
 *   <li>Build the state space.</li>
 *   <li>Check lattice properties.</li>
 *   <li>Check thread safety of concurrent method pairs.</li>
 *   <li>Typestate checking: verify client code follows the protocol.</li>
 * </ol>
 *
 * <p>Errors are emitted as compiler errors via {@code Messager}.
 * Successful checks emit informational notes.
 */
@SupportedAnnotationTypes("*")
@SupportedSourceVersion(SourceVersion.RELEASE_21)
public class SessionProcessor extends AbstractProcessor {

    @Override
    public boolean process(Set<? extends TypeElement> annotations, RoundEnvironment roundEnv) {
        // Phase A: validate all @Session classes and collect their state spaces
        var sessionStateSpaces = new LinkedHashMap<String, StateSpace>();

        for (Element element : roundEnv.getElementsAnnotatedWith(Session.class)) {
            if (element instanceof TypeElement typeElement) {
                StateSpace ss = processType(typeElement);
                if (ss != null) {
                    // Store by both simple and qualified name
                    sessionStateSpaces.put(typeElement.getQualifiedName().toString(), ss);
                    sessionStateSpaces.put(typeElement.getSimpleName().toString(), ss);
                }
            }
        }

        // Phase A2: add built-in protocols from the registry (no @Session needed)
        loadRegistryProtocols(sessionStateSpaces);

        // Phase B: typestate checking — scan ALL classes for client code
        if (!sessionStateSpaces.isEmpty()) {
            checkTypestates(roundEnv, sessionStateSpaces);
        }

        return true; // claim the annotation
    }

    /**
     * Phase B: typestate checking via reflection to avoid compile-time dependency
     * on {@code com.sun.source.util.Trees}. This allows the processor to load on
     * Eclipse's ECJ compiler (which lacks the Trees API), while still performing
     * full typestate checking on javac.
     *
     * <p>Delegates to {@code TypestatePhaseB.run()} which contains the actual
     * Trees-dependent logic in a separate class that is only loaded when needed.
     */
    private void checkTypestates(RoundEnvironment roundEnv,
                                  Map<String, StateSpace> sessionStateSpaces) {
        try {
            // Reflectively check if Trees API is available
            Class.forName("com.sun.source.util.Trees");
            // Delegate to TypestatePhaseB (loaded only here, so ECJ never touches it)
            Class<?> phaseBClass = Class.forName("com.bica.reborn.processor.TypestatePhaseB");
            Method runMethod = phaseBClass.getMethod("run",
                    javax.annotation.processing.ProcessingEnvironment.class,
                    RoundEnvironment.class, Map.class);
            runMethod.invoke(null, processingEnv, roundEnv, sessionStateSpaces);
        } catch (ClassNotFoundException e) {
            // com.sun.source.util.Trees not available (ECJ) — skip typestate checking
        } catch (Exception e) {
            // Reflection error — log and skip
            warning(null, "Typestate checking skipped: %s", e.getMessage());
        }
    }

    /**
     * Process a single @Session-annotated type through steps 1–6.
     *
     * @return the StateSpace if all checks pass, or null if any check fails
     */
    private StateSpace processType(TypeElement typeElement) {
        Session session = typeElement.getAnnotation(Session.class);
        String typeString = session.value();
        String className = typeElement.getQualifiedName().toString();
        boolean skipTermination = session.skipTermination();

        // 1. Parse
        SessionType ast;
        try {
            ast = Parser.parse(typeString);
        } catch (ParseError e) {
            error(typeElement, "Failed to parse session type for %s: %s", className, e.getMessage());
            return null;
        }

        // 2. Termination check
        TerminationResult termResult = TerminationChecker.checkTermination(ast);
        if (!termResult.isTerminating()) {
            if (skipTermination) {
                warning(typeElement, "Session type for %s is non-terminating (variables: %s) — skipped",
                        className, String.join(", ", termResult.nonTerminatingVars()));
            } else {
                error(typeElement, "Session type for %s is non-terminating (variables: %s)",
                        className, String.join(", ", termResult.nonTerminatingVars()));
                return null;
            }
        }

        // 3. WF-Par check
        WFParallelResult wfResult = TerminationChecker.checkWfParallel(ast);
        if (!wfResult.isWellFormed()) {
            if (skipTermination) {
                for (String err : wfResult.errors()) {
                    warning(typeElement, "WF-Par violation in %s: %s — skipped", className, err);
                }
            } else {
                for (String err : wfResult.errors()) {
                    error(typeElement, "WF-Par violation in %s: %s", className, err);
                }
                return null;
            }
        }

        // 4. Build state space
        StateSpace stateSpace;
        try {
            stateSpace = StateSpaceBuilder.build(ast);
        } catch (Exception e) {
            error(typeElement, "Failed to build state space for %s: %s", className, e.getMessage());
            return null;
        }

        // 4.5 Object conformance — selection return types
        List<ConformanceError> conformanceErrors = ConformanceChecker.check(stateSpace, typeElement, ast);
        for (ConformanceError err : conformanceErrors) {
            error(typeElement, "Conformance violation in %s: %s", className, err.message());
        }
        if (!conformanceErrors.isEmpty()) return null;

        // 5. Lattice check
        LatticeResult latticeResult = LatticeChecker.checkLattice(stateSpace);
        if (!latticeResult.isLattice()) {
            String detail = latticeResult.counterexample() != null
                    ? " (counterexample: states " + latticeResult.counterexample().a()
                      + " and " + latticeResult.counterexample().b()
                      + ", missing " + latticeResult.counterexample().kind() + ")"
                    : "";
            if (skipTermination && !termResult.isTerminating()) {
                warning(typeElement, "State space for %s is not a lattice%s — skipped (non-terminating)",
                        className, detail);
            } else {
                error(typeElement, "State space for %s is not a lattice%s", className, detail);
                return null;
            }
        }

        // 6. Thread safety check
        var classifier = new SourceMethodClassifier(typeElement);
        ThreadSafetyResult safetyResult = ThreadSafetyChecker.check(ast, classifier);
        if (!safetyResult.isSafe()) {
            for (ThreadSafetyResult.Violation v : safetyResult.violations()) {
                error(typeElement,
                        "Thread safety violation in %s: %s (%s) and %s (%s) "
                                + "may execute concurrently but are not compatible",
                        className, v.method1(), v.level1().symbol(),
                        v.method2(), v.level2().symbol());
            }
            return null;
        }

        // All checks passed (or skipped)
        note(typeElement,
                "Session type for %s verified: %d states, %d transitions, "
                        + "lattice %s, %d methods classified",
                className, stateSpace.states().size(), stateSpace.transitions().size(),
                latticeResult.isLattice() ? "OK" : "skipped",
                safetyResult.classifications().size());

        return stateSpace;
    }

    /**
     * Load built-in protocols from the ProtocolRegistry.
     * These don't require @Session annotations on the target classes.
     * Maps Java standard library type names to their session type state spaces.
     */
    private void loadRegistryProtocols(Map<String, StateSpace> sessionStateSpaces) {
        // Map from ProtocolRegistry: well-known Java types → session types
        Map<String, String> typeToSessionType = Map.ofEntries(
            Map.entry("Connection", "rec X . &{createStatement: X, prepareStatement: X, commit: X, rollback: X, setAutoCommit: X, getAutoCommit: X, getMetaData: X, close: end}"),
            Map.entry("java.sql.Connection", "rec X . &{createStatement: X, prepareStatement: X, commit: X, rollback: X, setAutoCommit: X, getAutoCommit: X, getMetaData: X, close: end}"),
            Map.entry("Statement", "rec X . &{executeQuery: X, executeUpdate: X, execute: X, setQueryTimeout: X, close: end}"),
            Map.entry("java.sql.Statement", "rec X . &{executeQuery: X, executeUpdate: X, execute: X, setQueryTimeout: X, close: end}"),
            Map.entry("PreparedStatement", "rec X . &{executeQuery: X, executeUpdate: X, execute: X, setString: X, setInt: X, setLong: X, setDouble: X, setObject: X, close: end}"),
            Map.entry("java.sql.PreparedStatement", "rec X . &{executeQuery: X, executeUpdate: X, execute: X, setString: X, setInt: X, setLong: X, setDouble: X, setObject: X, close: end}"),
            Map.entry("ResultSet", "rec X . &{next: X, getString: X, getInt: X, getLong: X, getDouble: X, getObject: X, getBoolean: X, close: end}"),
            Map.entry("java.sql.ResultSet", "rec X . &{next: X, getString: X, getInt: X, getLong: X, getDouble: X, getObject: X, getBoolean: X, close: end}"),
            Map.entry("InputStream", "rec X . &{read: X, skip: X, available: X, mark: X, reset: X, close: end}"),
            Map.entry("java.io.InputStream", "rec X . &{read: X, skip: X, available: X, mark: X, reset: X, close: end}"),
            Map.entry("OutputStream", "rec X . &{write: X, flush: X, close: end}"),
            Map.entry("java.io.OutputStream", "rec X . &{write: X, flush: X, close: end}"),
            Map.entry("Reader", "rec X . &{read: X, readLine: X, ready: X, mark: X, reset: X, close: end}"),
            Map.entry("java.io.Reader", "rec X . &{read: X, readLine: X, ready: X, mark: X, reset: X, close: end}"),
            Map.entry("Writer", "rec X . &{write: X, append: X, flush: X, close: end}"),
            Map.entry("java.io.Writer", "rec X . &{write: X, append: X, flush: X, close: end}"),
            Map.entry("BufferedReader", "rec X . &{read: X, readLine: X, ready: X, mark: X, reset: X, lines: X, close: end}"),
            Map.entry("Socket", "rec X . &{connect: X, getInputStream: X, getOutputStream: X, setSoTimeout: X, close: end}"),
            Map.entry("java.net.Socket", "rec X . &{connect: X, getInputStream: X, getOutputStream: X, setSoTimeout: X, close: end}")
        );

        for (var entry : typeToSessionType.entrySet()) {
            if (sessionStateSpaces.containsKey(entry.getKey())) continue;
            try {
                var ast = Parser.parse(entry.getValue());
                var ss = StateSpaceBuilder.build(ast);
                sessionStateSpaces.put(entry.getKey(), ss);
            } catch (Exception e) {
                // Skip silently if parsing fails
            }
        }
    }

    /**
     * Find the @Session AnnotationMirror on an element for precise diagnostic targeting.
     * When present, errors point to the annotation itself rather than the class declaration,
     * giving better IDE integration (especially Eclipse ECJ which shows real-time errors).
     */
    private AnnotationMirror findSessionMirror(Element element) {
        for (AnnotationMirror mirror : element.getAnnotationMirrors()) {
            String name = mirror.getAnnotationType().asElement().getSimpleName().toString();
            if ("Session".equals(name)) {
                return mirror;
            }
        }
        return null;
    }

    private void error(Element element, String format, Object... args) {
        AnnotationMirror mirror = element != null ? findSessionMirror(element) : null;
        if (mirror != null) {
            processingEnv.getMessager().printMessage(
                    Diagnostic.Kind.ERROR, String.format(format, args), element, mirror);
        } else {
            processingEnv.getMessager().printMessage(
                    Diagnostic.Kind.ERROR, String.format(format, args), element);
        }
    }

    private void warning(Element element, String format, Object... args) {
        AnnotationMirror mirror = element != null ? findSessionMirror(element) : null;
        if (mirror != null) {
            processingEnv.getMessager().printMessage(
                    Diagnostic.Kind.WARNING, String.format(format, args), element, mirror);
        } else {
            processingEnv.getMessager().printMessage(
                    Diagnostic.Kind.WARNING, String.format(format, args), element);
        }
    }

    private void note(Element element, String format, Object... args) {
        AnnotationMirror mirror = element != null ? findSessionMirror(element) : null;
        if (mirror != null) {
            processingEnv.getMessager().printMessage(
                    Diagnostic.Kind.NOTE, String.format(format, args), element, mirror);
        } else {
            processingEnv.getMessager().printMessage(
                    Diagnostic.Kind.NOTE, String.format(format, args), element);
        }
    }
}

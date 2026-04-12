package com.bica.reborn.cli;

import com.bica.reborn.parser.Parser;
import com.bica.reborn.statespace.StateSpace;
import com.bica.reborn.statespace.StateSpaceBuilder;
import com.bica.reborn.typestate.MethodCall;
import com.bica.reborn.typestate.TypestateChecker;
import com.bica.reborn.typestate.TypestateError;
import com.bica.reborn.typestate.TypestateResult;

import java.io.*;
import java.nio.file.*;
import java.util.*;
import java.util.regex.*;
import java.util.stream.Stream;

/**
 * Scans a Java project directory for protocol violations.
 *
 * <p>Given a session type protocol and a set of variable name patterns,
 * extracts method call sequences from Java source files and checks each
 * against the protocol using {@link TypestateChecker}.
 *
 * <p>This scanner uses regex-based extraction (no compiler API needed),
 * making it usable on any Java source code without compilation.
 *
 * <pre>{@code
 * Usage: bica scan <project-dir> --protocol <session-type> --vars <var1,var2,...>
 * }</pre>
 */
public final class ProjectScanner {

    /** Result of scanning a single method in a source file. */
    public record Finding(
            String filePath,
            String className,
            String methodName,
            String variable,
            List<String> methodCalls,
            TypestateResult result) {

        public boolean hasViolations() {
            return !result.isValid();
        }

        public String summary() {
            if (result.isValid()) {
                return String.format("  PASS %s.%s() on %s [%s]",
                        className, methodName, variable,
                        String.join(", ", methodCalls));
            } else {
                var firstError = result.errors().get(0);
                return String.format("  FAIL %s.%s() on %s [%s]\n       %s",
                        className, methodName, variable,
                        String.join(", ", methodCalls),
                        firstError.message());
            }
        }
    }

    /** Aggregate scan report. */
    public record ScanReport(
            String projectDir,
            String sessionType,
            int filesScanned,
            int methodsChecked,
            int violations,
            int conforming,
            int incomplete,
            List<Finding> findings) {

        public String format() {
            var sb = new StringBuilder();
            sb.append("=" .repeat(78)).append('\n');
            sb.append("  BICA REBORN: PROJECT SCAN REPORT\n");
            sb.append("=" .repeat(78)).append('\n');
            sb.append(String.format("  Directory: %s%n", projectDir));
            sb.append(String.format("  Protocol: %s%n", sessionType.length() > 70
                    ? sessionType.substring(0, 70) + "..." : sessionType));
            sb.append(String.format("  Files scanned: %d%n", filesScanned));
            sb.append(String.format("  Methods checked: %d%n", methodsChecked));
            sb.append(String.format("  Conforming: %d%n", conforming));
            sb.append(String.format("  Violations: %d%n", violations));
            sb.append(String.format("  Incomplete: %d%n", incomplete));
            sb.append('\n');

            if (violations > 0) {
                sb.append("  --- VIOLATIONS ---\n");
                for (var f : findings) {
                    if (f.hasViolations()) {
                        sb.append(f.summary()).append('\n');
                    }
                }
                sb.append('\n');
            }

            sb.append("  --- ALL RESULTS ---\n");
            for (var f : findings) {
                sb.append(f.summary()).append('\n');
            }
            sb.append("=" .repeat(78)).append('\n');
            return sb.toString();
        }
    }

    private ProjectScanner() {}

    /**
     * Scan a Java project for protocol violations.
     *
     * @param projectDir   root directory to scan
     * @param sessionType  session type string defining the protocol
     * @param varPatterns  variable name patterns to track
     * @param checkEnd     if true, report incomplete sessions
     * @return scan report with all findings
     */
    public static ScanReport scan(Path projectDir, String sessionType,
                                   Set<String> varPatterns, boolean checkEnd)
            throws IOException {

        // Parse session type and build state space
        var ast = Parser.parse(sessionType);
        var stateSpace = StateSpaceBuilder.build(ast);

        var findings = new ArrayList<Finding>();
        int filesScanned = 0;

        // Walk all Java files
        try (Stream<Path> paths = Files.walk(projectDir)) {
            var javaFiles = paths
                    .filter(p -> p.toString().endsWith(".java"))
                    .filter(p -> !p.toString().contains("/target/"))
                    .filter(p -> !p.toString().contains("/build/"))
                    .filter(p -> !p.toString().contains("/.git/"))
                    .toList();

            for (Path javaFile : javaFiles) {
                filesScanned++;
                var fileFindings = scanFile(javaFile, stateSpace, varPatterns, checkEnd);
                findings.addAll(fileFindings);
            }
        }

        int violations = 0, conforming = 0, incomplete = 0;
        for (var f : findings) {
            if (f.hasViolations()) {
                boolean hasEndError = f.result.errors().stream()
                        .anyMatch(e -> e.method().equals("<end-of-method>"));
                boolean hasMethodError = f.result.errors().stream()
                        .anyMatch(e -> !e.method().equals("<end-of-method>"));
                if (hasMethodError) {
                    violations++;
                } else {
                    incomplete++;
                }
            } else {
                conforming++;
            }
        }

        return new ScanReport(projectDir.toString(), sessionType,
                filesScanned, findings.size(), violations, conforming,
                incomplete, findings);
    }

    /**
     * Scan a single Java file for method calls on tracked variables.
     */
    static List<Finding> scanFile(Path javaFile, StateSpace stateSpace,
                                    Set<String> varPatterns, boolean checkEnd) {
        var findings = new ArrayList<Finding>();

        String source;
        try {
            source = Files.readString(javaFile);
        } catch (IOException e) {
            return findings;
        }

        String fileName = javaFile.getFileName().toString().replace(".java", "");

        // Extract class name
        var classPattern = Pattern.compile(
                "(?:public|final|abstract)\\s+(?:final\\s+)?class\\s+(\\w+)");
        var classMatcher = classPattern.matcher(source);
        String className = classMatcher.find() ? classMatcher.group(1) : fileName;

        // Extract method bodies and their calls on tracked variables
        var methodPattern = Pattern.compile(
                "(?:public|private|protected)\\s+(?:static\\s+)?(?:final\\s+)?" +
                "(?:\\w+(?:<[^>]*>)?(?:\\[\\])?)\\s+(\\w+)\\s*\\([^)]*\\)\\s*" +
                "(?:throws[^{]*)?\\{");
        var methodMatcher = methodPattern.matcher(source);

        while (methodMatcher.find()) {
            String methodName = methodMatcher.group(1);
            int bodyStart = methodMatcher.end();

            // Find matching closing brace (simple brace counting)
            int depth = 1;
            int bodyEnd = bodyStart;
            for (int i = bodyStart; i < source.length() && depth > 0; i++) {
                if (source.charAt(i) == '{') depth++;
                else if (source.charAt(i) == '}') depth--;
                if (depth == 0) bodyEnd = i;
            }

            String body = source.substring(bodyStart, bodyEnd);

            // Extract method calls: variable.method(
            var callPattern = Pattern.compile("(\\w+)\\.(\\w+)\\s*\\(");
            var callMatcher = callPattern.matcher(body);

            // Group calls by variable
            var callsByVar = new LinkedHashMap<String, List<String>>();
            while (callMatcher.find()) {
                String var = callMatcher.group(1);
                String method = callMatcher.group(2);
                if (varPatterns.contains(var)) {
                    callsByVar.computeIfAbsent(var, k -> new ArrayList<>()).add(method);
                }
            }

            // Check each variable's call sequence
            for (var entry : callsByVar.entrySet()) {
                String var = entry.getKey();
                List<String> calls = entry.getValue();
                if (calls.isEmpty()) continue;

                // Build MethodCall list
                var methodCalls = calls.stream()
                        .map(m -> new MethodCall(var, m, 0))
                        .toList();

                // Run typestate check
                var result = TypestateChecker.check(stateSpace, methodCalls,
                        Map.of(var, stateSpace.top()), checkEnd);

                findings.add(new Finding(
                        javaFile.toString(), className, methodName,
                        var, calls, result));
            }
        }

        return findings;
    }

    /**
     * CLI entry point for the scan command.
     *
     * @return exit code (0 = no violations, 1 = violations found)
     */
    public static int main(String[] args, PrintStream out, PrintStream err) {
        if (args.length < 1) {
            err.println("Usage: bica scan <project-dir> --protocol <type> --vars <v1,v2,...>");
            err.println("       bica scan <project-dir> --protocol <type> --vars <v1,v2,...> --no-end-check");
            return 1;
        }

        String projectDir = args[0];
        String protocol = null;
        Set<String> vars = new LinkedHashSet<>();
        boolean checkEnd = true;

        for (int i = 1; i < args.length; i++) {
            switch (args[i]) {
                case "--protocol", "-p" -> {
                    if (i + 1 < args.length) protocol = args[++i];
                }
                case "--vars", "-v" -> {
                    if (i + 1 < args.length) {
                        for (String v : args[++i].split(",")) {
                            vars.add(v.trim());
                        }
                    }
                }
                case "--no-end-check" -> checkEnd = false;
            }
        }

        // Registry mode: scan with ALL protocols
        boolean registry = false;
        for (String arg : args) {
            if ("--registry".equals(arg)) registry = true;
            if ("--list-protocols".equals(arg)) {
                out.println("Available protocols:");
                for (var p : ProtocolRegistry.ALL) {
                    out.printf("  %-25s %s%n", p.name(), p.description());
                    out.printf("  %-25s vars: %s%n", "", p.varPatterns());
                }
                return 0;
            }
        }

        if (registry) {
            return runRegistryScan(Path.of(projectDir), out, err);
        }

        if (protocol == null) {
            err.println("Error: --protocol required (or use --registry for all protocols)");
            return 1;
        }
        if (vars.isEmpty()) {
            err.println("Error: --vars required");
            return 1;
        }

        try {
            var report = scan(Path.of(projectDir), protocol, vars, checkEnd);
            out.println(report.format());
            return report.violations() > 0 ? 1 : 0;
        } catch (Exception e) {
            err.println("Error: " + e.getMessage());
            return 1;
        }
    }

    /**
     * Scan with ALL protocols from the registry.
     */
    private static int runRegistryScan(Path projectDir, PrintStream out, PrintStream err) {
        out.println("=" .repeat(78));
        out.println("  BICA REBORN: MULTI-PROTOCOL SCAN");
        out.println("=" .repeat(78));
        out.printf("  Directory: %s%n", projectDir);
        out.printf("  Protocols: %d%n", ProtocolRegistry.ALL.size());
        out.println();

        int totalViolations = 0;
        int totalChecked = 0;
        int totalConforming = 0;
        int totalIncomplete = 0;

        for (var protocol : ProtocolRegistry.ALL) {
            try {
                var report = scan(projectDir, protocol.sessionType(),
                        protocol.varPatterns(), protocol.checkEnd());

                if (report.methodsChecked() > 0) {
                    out.printf("  --- %s ---%n", protocol.name());
                    out.printf("    Checked: %d, Violations: %d, Incomplete: %d, Conforming: %d%n",
                            report.methodsChecked(), report.violations(),
                            report.incomplete(), report.conforming());

                    // Show first 3 violations per protocol
                    int shown = 0;
                    for (var f : report.findings()) {
                        if (f.hasViolations() && shown < 3) {
                            boolean isMethodError = f.result().errors().stream()
                                    .anyMatch(e -> !e.method().equals("<end-of-method>"));
                            if (isMethodError) {
                                out.println("    " + f.summary().replace("\n", "\n    "));
                                shown++;
                            }
                        }
                    }
                    if (report.violations() > shown) {
                        out.printf("    ... and %d more violations%n", report.violations() - shown);
                    }
                    out.println();

                    totalViolations += report.violations();
                    totalChecked += report.methodsChecked();
                    totalConforming += report.conforming();
                    totalIncomplete += report.incomplete();
                }
            } catch (Exception e) {
                err.printf("  [SKIP] %s: %s%n", protocol.name(), e.getMessage());
            }
        }

        out.println("  === TOTALS ===");
        out.printf("  Methods checked: %d%n", totalChecked);
        out.printf("  Violations: %d%n", totalViolations);
        out.printf("  Incomplete: %d%n", totalIncomplete);
        out.printf("  Conforming: %d%n", totalConforming);
        out.println("=" .repeat(78));

        return totalViolations > 0 ? 1 : 0;
    }
}

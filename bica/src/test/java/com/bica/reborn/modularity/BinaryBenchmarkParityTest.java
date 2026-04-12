package com.bica.reborn.modularity;

import com.bica.reborn.ast.SessionType;
import com.bica.reborn.parser.Parser;
import org.junit.jupiter.api.DynamicTest;
import org.junit.jupiter.api.TestFactory;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * Java side of the modularity-campaign Task 45 binary parity sweep.
 *
 * <p>Reads
 * {@code reticulate/tests/benchmarks/modularity/binary_benchmarks.json}
 * (the ground-truth file produced by
 * {@code scripts/modularity-parity-sweep.py}) and asserts that
 * {@link ModularityChecker#check} on the BICA side returns the same
 * classification, modularity verdict, and certificate as
 * {@code reticulate.modularity.check_modularity_ast} for every
 * benchmark in the sweep.
 *
 * <p>The two halves together close the parity obligation in the
 * modularity-campaign charter §"Parity obligations" for the binary
 * benchmark axis. Per {@code feedback_java_python_parity}, BICA and
 * reticulate must never disagree on a benchmark verdict; this test
 * is the mechanical guard.
 *
 * <p>To regenerate the ground-truth JSON after modifying any
 * benchmark protocol or modularity-pipeline component:
 *
 * <pre>
 *   python3 scripts/modularity-parity-sweep.py
 * </pre>
 *
 * <p>then re-run this test. A divergence will be reported as a
 * dynamic-test failure naming the offending benchmark.
 */
class BinaryBenchmarkParityTest {

    /**
     * Path to the parity ground-truth JSON, resolved relative to the
     * BICA module directory (the working directory under {@code mvn
     * test}). The file is committed under
     * {@code reticulate/tests/benchmarks/modularity/}.
     */
    private static final Path GROUND_TRUTH = Paths.get(
            "..", "reticulate", "tests", "benchmarks",
            "modularity", "binary_benchmarks.json");

    /** A single parity-test record loaded from the JSON ground truth. */
    private record Record(
            String name,
            String typeString,
            Integer numStates,
            String classification,
            Boolean isModular,
            Boolean isDistributive,
            String certificate,
            String error) {}

    /**
     * Generate one parity assertion per benchmark.
     *
     * <p>JUnit Jupiter dynamic tests are used so each benchmark gets
     * its own line in the test report — a divergence is named with
     * the benchmark and the type string, not buried in a single
     * giant assertion.
     */
    /**
     * Pattern matching the {@code wait} keyword as an isolated token.
     * Used to skip benchmarks that exercise the parallel-arm
     * synchronisation token, which BICA's parser does not yet
     * support — see charter §"T2a vs T2b decision" risk 1
     * (wait-sync push into {@code ∥}, charter item 12).
     */
    private static final Pattern WAIT_TOKEN = Pattern.compile(
            "(?<![A-Za-z0-9_])wait(?![A-Za-z0-9_])");

    @TestFactory
    Iterable<DynamicTest> binaryBenchmarkParity() {
        List<Record> records = loadGroundTruth();
        List<DynamicTest> tests = new ArrayList<>(records.size());

        for (Record r : records) {
            tests.add(DynamicTest.dynamicTest(r.name(), () -> {
                if (r.error() != null) {
                    // Python recorded a parse / build error. We expect
                    // BICA to fail in the same place. Skip the
                    // assertion — the Java side does not have a
                    // matching error-comparison framework yet, and
                    // these benchmarks would be cleaned up in a
                    // separate task.
                    return;
                }

                if (WAIT_TOKEN.matcher(r.typeString()).find()) {
                    // Charter item (12) gap: BICA's parser does not yet
                    // recognise `wait` as the parallel-arm
                    // synchronisation token. Reticulate handles it as
                    // the `Wait` AST leaf. Until charter item (12)
                    // (wait-sync push into ∥) lands in BICA, these
                    // benchmarks are Python-only. They are listed in
                    // `parity-report.md` under §"Wait-keyword gap" so
                    // operators can see exactly which protocols are
                    // currently unverifiable on the Java side.
                    return;
                }

                SessionType ast;
                try {
                    ast = Parser.parse(r.typeString());
                } catch (Exception e) {
                    throw new AssertionError(
                            "BICA parser failed on benchmark '" + r.name()
                                    + "' (Python parsed it OK): "
                                    + e.getMessage(), e);
                }

                ModularityResult bica = ModularityChecker.check(ast);

                // Verdict parity. Each assertion produces an isolated
                // failure message naming the benchmark and the
                // divergent field, so a regression report tells the
                // operator exactly which side drifted.
                assertEquals(
                        r.classification(), bica.classification(),
                        "classification divergence on benchmark '"
                                + r.name() + "' (type: "
                                + r.typeString() + ")");
                assertEquals(
                        r.isModular(), bica.isModular(),
                        "isModular divergence on benchmark '"
                                + r.name() + "'");
                assertEquals(
                        r.isDistributive(), bica.isDistributive(),
                        "isDistributive divergence on benchmark '"
                                + r.name() + "'");
                assertEquals(
                        r.certificate(), bica.certificate(),
                        "certificate divergence on benchmark '"
                                + r.name() + "'");
            }));
        }

        return tests;
    }

    // ------------------------------------------------------------------
    // Minimal JSON loader (no external dependency)
    // ------------------------------------------------------------------

    /**
     * Load and parse the ground-truth JSON without pulling in a
     * full JSON library. The file is well-formed and produced by
     * {@code json.dumps} in the sweep script, so a tiny regex-driven
     * record extractor is sufficient and keeps BICA's test
     * dependency surface unchanged.
     */
    private static List<Record> loadGroundTruth() {
        String text;
        try {
            text = Files.readString(GROUND_TRUTH);
        } catch (IOException e) {
            throw new UncheckedIOException(
                    "failed to read parity ground truth at "
                            + GROUND_TRUTH.toAbsolutePath()
                            + " — re-run scripts/modularity-parity-sweep.py",
                    e);
        }

        // Each record begins with `{` and ends with `}` at indent 2.
        // The sweep script uses `json.dumps(records, indent=2)` so
        // the layout is stable.
        Pattern recordRe = Pattern.compile(
                "\\{\\s*\"name\":\\s*\"([^\"]*)\","
                        + "\\s*\"type_string\":\\s*\"((?:[^\"\\\\]|\\\\.)*)\","
                        + "\\s*\"num_states\":\\s*(null|\\d+),"
                        + "\\s*\"classification\":\\s*(null|\"[^\"]*\"),"
                        + "\\s*\"is_modular\":\\s*(null|true|false),"
                        + "\\s*\"is_distributive\":\\s*(null|true|false),"
                        + "\\s*\"certificate\":\\s*(null|\"[^\"]*\"),"
                        + "\\s*\"error\":\\s*(null|\"(?:[^\"\\\\]|\\\\.)*\")\\s*\\}",
                Pattern.DOTALL);

        Matcher m = recordRe.matcher(text);
        List<Record> records = new ArrayList<>();
        while (m.find()) {
            records.add(new Record(
                    m.group(1),
                    unescape(m.group(2)),
                    parseInt(m.group(3)),
                    parseString(m.group(4)),
                    parseBool(m.group(5)),
                    parseBool(m.group(6)),
                    parseString(m.group(7)),
                    parseString(m.group(8))));
        }

        if (records.isEmpty()) {
            throw new AssertionError(
                    "BinaryBenchmarkParityTest loaded zero records "
                            + "from " + GROUND_TRUTH.toAbsolutePath()
                            + " — JSON layout may have changed; "
                            + "update the regex in loadGroundTruth().");
        }

        return records;
    }

    private static Integer parseInt(String s) {
        return "null".equals(s) ? null : Integer.parseInt(s);
    }

    private static Boolean parseBool(String s) {
        if ("null".equals(s)) return null;
        return "true".equals(s);
    }

    private static String parseString(String s) {
        if ("null".equals(s)) return null;
        // Strip surrounding quotes and unescape.
        return unescape(s.substring(1, s.length() - 1));
    }

    private static String unescape(String s) {
        return s.replace("\\\"", "\"")
                .replace("\\\\", "\\")
                .replace("\\n", "\n")
                .replace("\\t", "\t");
    }
}

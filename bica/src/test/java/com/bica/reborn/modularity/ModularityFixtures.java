package com.bica.reborn.modularity;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

/**
 * Loader for the canonical modularity fixtures under
 * {@code bica/src/test/resources/modularity/}.
 *
 * <p>Java mirror of
 * {@code reticulate/tests/test_modularity_fixtures.py::load_fixture}.
 * The file format is: lines starting with {@code #} are comments,
 * blank lines are ignored, and the first non-comment non-empty line
 * is the session type string.
 *
 * <p>Per {@code feedback_java_python_parity} the fixture payload
 * strings in this directory MUST stay character-identical to the
 * Python copies under
 * {@code reticulate/tests/benchmarks/modularity/}. The Python test
 * {@code TestJavaParity::test_python_and_java_fixtures_agree}
 * enforces this at test time on the Python side. No corresponding
 * Java-side enforcement is needed — the two sides fail together if
 * either drifts.
 */
public final class ModularityFixtures {

    private ModularityFixtures() {}

    /** The directory holding the {@code .session} fixture files. */
    public static final Path FIXTURE_DIR =
            Paths.get("src", "test", "resources", "modularity");

    /**
     * Load the session type payload from a fixture by its stem name.
     *
     * @param name the fixture stem, e.g. {@code "n5_canonical"}
     * @return the session type string, suitable for
     *         {@link com.bica.reborn.parser.Parser#parse(String)}
     * @throws UncheckedIOException if the fixture cannot be read
     * @throws IllegalStateException if the fixture has no payload line
     */
    public static String load(String name) {
        Path path = FIXTURE_DIR.resolve(name + ".session");
        try {
            for (String line : Files.readAllLines(path)) {
                String stripped = line.strip();
                if (stripped.isEmpty() || stripped.startsWith("#")) {
                    continue;
                }
                return stripped;
            }
        } catch (IOException e) {
            throw new UncheckedIOException(
                    "failed to read fixture " + path, e);
        }
        throw new IllegalStateException(
                "fixture " + path + " has no non-comment session type string");
    }
}

package com.bica.reborn.testgen;

import java.util.Objects;

/**
 * Configuration for JUnit 5 test generation from a session type state space.
 *
 * @param className      class under test (e.g. "FileHandle")
 * @param packageName    package for the generated test class (null for default package)
 * @param varName        local variable name for the object under test (default "obj")
 * @param maxRevisits    maximum times a state may be revisited during DFS path enumeration (default 2)
 * @param maxPaths       maximum number of valid paths to emit before truncation (default 100)
 * @param violationStyle how violation tests are rendered (default {@link ViolationStyle#DISABLED_ANNOTATION})
 */
public record TestGenConfig(
        String className,
        String packageName,
        String varName,
        int maxRevisits,
        int maxPaths,
        ViolationStyle violationStyle) {

    public TestGenConfig {
        Objects.requireNonNull(className, "className must not be null");
        if (className.isBlank()) {
            throw new IllegalArgumentException("className must not be blank");
        }
        if (varName == null || varName.isBlank()) {
            varName = "obj";
        }
        if (maxRevisits < 0) {
            throw new IllegalArgumentException("maxRevisits must be non-negative");
        }
        if (maxPaths < 1) {
            throw new IllegalArgumentException("maxPaths must be at least 1");
        }
        if (violationStyle == null) {
            violationStyle = ViolationStyle.DISABLED_ANNOTATION;
        }
    }

    /** Creates a config with defaults for everything except className. */
    public static TestGenConfig withDefaults(String className) {
        return new TestGenConfig(className, null, "obj", 2, 100, ViolationStyle.DISABLED_ANNOTATION);
    }

    /** Creates a config with a package name and defaults for everything else. */
    public static TestGenConfig withPackage(String className, String packageName) {
        return new TestGenConfig(className, packageName, "obj", 2, 100, ViolationStyle.DISABLED_ANNOTATION);
    }
}

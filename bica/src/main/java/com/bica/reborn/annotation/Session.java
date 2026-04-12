package com.bica.reborn.annotation;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Declares the session type for a class.
 *
 * <p>The annotation processor parses the session type string, builds the state
 * space, checks lattice properties, termination, WF-Par, and thread safety.
 *
 * <p>Example:
 * <pre>{@code
 * @Session("open . +{OK: (read . end || write . end) . close . end, ERR: end}")
 * public class FileObject { ... }
 * }</pre>
 */
@Target(ElementType.TYPE)
@Retention(RetentionPolicy.SOURCE)
public @interface Session {

    /** The session type string in BICA syntax. */
    String value();

    /**
     * If {@code true}, non-terminating recursive types produce a warning
     * instead of a compilation error, and the pipeline continues.
     */
    boolean skipTermination() default false;
}

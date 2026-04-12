package com.bica.reborn.annotation;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Asserts that a method is safe for concurrent access from any thread.
 *
 * <p>This is a trust boundary: the programmer asserts thread safety and
 * the checker takes their word for it (level {@code SHARED}).
 */
@Target(ElementType.METHOD)
@Retention(RetentionPolicy.SOURCE)
public @interface Shared {
}

package com.bica.reborn.annotation;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Asserts that a method requires exclusive access to the object's state.
 *
 * <p>Exclusive methods cannot execute concurrently with any other method.
 * This is the default assumption for unannotated methods (level {@code EXCLUSIVE}).
 */
@Target(ElementType.METHOD)
@Retention(RetentionPolicy.SOURCE)
public @interface Exclusive {
}

package com.bica.reborn.annotation;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Asserts that a method performs read-only access to the object's state.
 *
 * <p>Read-only methods are compatible with other read-only or synchronized
 * methods, but not with exclusive methods (level {@code READ_ONLY}).
 */
@Target(ElementType.METHOD)
@Retention(RetentionPolicy.SOURCE)
public @interface ReadOnly {
}

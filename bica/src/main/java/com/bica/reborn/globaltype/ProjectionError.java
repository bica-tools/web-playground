package com.bica.reborn.globaltype;

/**
 * Raised when projection of a global type onto a role is undefined.
 */
public class ProjectionError extends RuntimeException {

    private final String role;

    public ProjectionError(String message, String role) {
        super(message);
        this.role = role;
    }

    public ProjectionError(String message) {
        this(message, null);
    }

    /** The role for which projection failed, or null. */
    public String role() {
        return role;
    }
}

package com.bica.reborn.globaltype;

import com.bica.reborn.ast.SessionType;

/**
 * Result of projecting a global type onto a role.
 */
public record ProjectionResult(
        String role,
        SessionType localType,
        String localTypeStr,
        boolean isWellDefined) {
}

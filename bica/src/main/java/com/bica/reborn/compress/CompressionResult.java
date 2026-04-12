package com.bica.reborn.compress;

import com.bica.reborn.ast.SessionType;

/**
 * Result of session type compression analysis.
 *
 * @param compressed      the compressed session type (shared subtrees replaced with Var/Rec)
 * @param originalSize    AST node count of the original type
 * @param compressedSize  AST node count after compression
 * @param compressionRatio originalSize / compressedSize (>1 means sharing found)
 * @param sharedSubtrees  number of distinct shared subtrees factored out
 */
public record CompressionResult(
        SessionType compressed,
        int originalSize,
        int compressedSize,
        double compressionRatio,
        int sharedSubtrees) {
}

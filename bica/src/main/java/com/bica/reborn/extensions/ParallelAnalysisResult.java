package com.bica.reborn.extensions;

import com.bica.reborn.statespace.StateSpace;

import java.util.List;
import java.util.Objects;

/**
 * Result of parallel-level factor analysis on a state space.
 *
 * @param factorCount           number of product factors (0 if not a product)
 * @param factors               the decomposed factor state spaces (empty if not a product)
 * @param factorSizes           sizes of each factor
 * @param factorHeights         heights of each factor
 * @param factorWidths          widths of each factor
 * @param distributiveFromFactors whether the product is distributive based on factor analysis
 * @param predictedSize         predicted product size (product of factor sizes)
 * @param actualSize            actual product state space size
 */
public record ParallelAnalysisResult(
        int factorCount,
        List<StateSpace> factors,
        List<Integer> factorSizes,
        List<Integer> factorHeights,
        List<Integer> factorWidths,
        boolean distributiveFromFactors,
        int predictedSize,
        int actualSize) {

    public ParallelAnalysisResult {
        Objects.requireNonNull(factors, "factors must not be null");
        Objects.requireNonNull(factorSizes, "factorSizes must not be null");
        Objects.requireNonNull(factorHeights, "factorHeights must not be null");
        Objects.requireNonNull(factorWidths, "factorWidths must not be null");
        factors = List.copyOf(factors);
        factorSizes = List.copyOf(factorSizes);
        factorHeights = List.copyOf(factorHeights);
        factorWidths = List.copyOf(factorWidths);
    }
}

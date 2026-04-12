package com.bica.reborn.fca;

/**
 * Complete FCA analysis result.
 *
 * @param context           the formal context
 * @param numObjects        number of objects (states)
 * @param numAttributes     number of attributes (labels)
 * @param numConcepts       number of formal concepts
 * @param conceptLattice    the concept lattice
 * @param density           incidence density |I| / (|G| x |M|)
 * @param isClarified       true iff no two objects have same intent and no two attributes have same extent
 * @param numOriginalStates number of states in the original state space
 * @param latticeSizeRatio  numConcepts / numOriginalStates
 */
public record FCAResult(
        FormalContext context,
        int numObjects,
        int numAttributes,
        int numConcepts,
        ConceptLattice conceptLattice,
        double density,
        boolean isClarified,
        int numOriginalStates,
        double latticeSizeRatio) {
}

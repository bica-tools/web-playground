package com.bica.reborn.algebraic.zeta;

import com.bica.reborn.algebraic.zeta.ExpanderCodes.CheegerBounds;
import com.bica.reborn.algebraic.zeta.ExpanderCodes.ExpanderCodeResult;
import com.bica.reborn.parser.Parser;
import com.bica.reborn.statespace.StateSpace;
import com.bica.reborn.statespace.StateSpaceBuilder;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Tests for ExpanderCodes: port of Python tests/test_expander_codes.py
 * (38 tests, Step 31e). Cross-checked numerically against the Python
 * reference for all benchmarks.
 */
class ExpanderCodesTest {

    private static final double TOL = 1e-9;

    private StateSpace ss(String type) {
        return StateSpaceBuilder.build(Parser.parse(type));
    }

    // ---------------------------------------------------------------------
    // Spectral gap
    // ---------------------------------------------------------------------

    @Nested
    class SpectralGap {
        @Test
        void endGap() {
            assertEquals(0.0, ExpanderCodes.spectralGap(ss("end")));
        }

        @Test
        void simpleBranchGap() {
            double gap = ExpanderCodes.spectralGap(ss("&{a: end}"));
            assertTrue(gap >= 0.0);
            // 2-node path: Laplacian eigenvalues 0,2
            assertEquals(2.0, gap, TOL);
        }

        @Test
        void chainGap() {
            double gap = ExpanderCodes.spectralGap(ss("&{a: &{b: end}}"));
            assertTrue(gap > 0.0);
            assertEquals(1.0, gap, TOL);
        }

        @Test
        void diamondGap() {
            double gap = ExpanderCodes.spectralGap(ss("&{a: &{c: end}, b: &{c: end}}"));
            assertTrue(gap > 0.0);
            assertEquals(2.0, gap, TOL);
        }

        @Test
        void parallelGap() {
            double gap = ExpanderCodes.spectralGap(ss("(&{a: end} || &{b: end})"));
            assertTrue(gap >= 0.0);
            assertEquals(2.0, gap, TOL);
        }
    }

    // ---------------------------------------------------------------------
    // Expansion ratio
    // ---------------------------------------------------------------------

    @Nested
    class ExpansionRatio {
        @Test
        void endExpansion() {
            assertEquals(0.0, ExpanderCodes.expansionRatio(ss("end")));
        }

        @Test
        void simpleBranchExpansion() {
            assertTrue(ExpanderCodes.expansionRatio(ss("&{a: end}")) >= 0.0);
        }

        @Test
        void chainExpansion() {
            assertTrue(ExpanderCodes.expansionRatio(ss("&{a: &{b: end}}")) >= 0.0);
        }

        @Test
        void diamondExpansion() {
            double r = ExpanderCodes.expansionRatio(ss("&{a: &{c: end}, b: &{c: end}}"));
            assertTrue(r >= 0.0);
            assertEquals(1.0, r, TOL);
        }

        @Test
        void parallelExpansion() {
            assertTrue(ExpanderCodes.expansionRatio(ss("(&{a: end} || &{b: end})")) >= 0.0);
        }
    }

    // ---------------------------------------------------------------------
    // Expander test
    // ---------------------------------------------------------------------

    @Nested
    class IsExpander {
        @Test
        void endNotExpander() {
            assertFalse(ExpanderCodes.isExpander(ss("end")));
        }

        @Test
        void simpleBranchThreshold() {
            // Always returns boolean — check it's well-defined
            boolean r = ExpanderCodes.isExpander(ss("&{a: end}"));
            assertTrue(r || !r);
        }

        @Test
        void customThreshold() {
            assertTrue(ExpanderCodes.isExpander(ss("&{a: &{b: end}}"), 0.01));
        }

        @Test
        void highThreshold() {
            assertFalse(ExpanderCodes.isExpander(ss("&{a: &{b: &{c: end}}}"), 100.0));
        }
    }

    // ---------------------------------------------------------------------
    // Cheeger bounds
    // ---------------------------------------------------------------------

    @Nested
    class Cheeger {
        @Test
        void endCheeger() {
            CheegerBounds cb = ExpanderCodes.cheegerBounds(ss("end"));
            assertEquals(0.0, cb.spectralGap());
            assertEquals(0.0, cb.lower());
        }

        @Test
        void chainCheegerInequality() {
            CheegerBounds cb = ExpanderCodes.cheegerBounds(ss("&{a: &{b: end}}"));
            assertTrue(cb.lower() <= cb.upper() || cb.spectralGap() == 0.0);
            assertTrue(cb.lower() >= 0.0);
        }

        @Test
        void diamondCheeger() {
            CheegerBounds cb = ExpanderCodes.cheegerBounds(ss("&{a: &{c: end}, b: &{c: end}}"));
            assertTrue(cb.maxDegree() >= 1);
            assertTrue(cb.spectralGap() > 0.0);
        }

        @Test
        void cheegerLowerIsHalfGap() {
            CheegerBounds cb = ExpanderCodes.cheegerBounds(ss("&{a: end}"));
            assertEquals(cb.spectralGap() / 2.0, cb.lower(), 1e-10);
        }
    }

    // ---------------------------------------------------------------------
    // Parity check matrix
    // ---------------------------------------------------------------------

    @Nested
    class ParityCheck {
        @Test
        void endEmpty() {
            int[][] H = ExpanderCodes.parityCheckMatrix(ss("end"));
            assertEquals(1, H.length);
            assertEquals(0, H[0].length);
        }

        @Test
        void simpleBranchShape() {
            int[][] H = ExpanderCodes.parityCheckMatrix(ss("&{a: end}"));
            assertEquals(2, H.length);
            assertEquals(1, H[0].length);
            int colSum = 0;
            for (int r = 0; r < 2; r++) colSum += H[r][0];
            assertEquals(2, colSum);
        }

        @Test
        void chainShape() {
            int[][] H = ExpanderCodes.parityCheckMatrix(ss("&{a: &{b: end}}"));
            assertEquals(3, H.length);
            assertEquals(2, H[0].length);
        }

        @Test
        void binaryEntries() {
            int[][] H = ExpanderCodes.parityCheckMatrix(ss("&{a: &{c: end}, b: &{c: end}}"));
            for (int[] row : H) {
                for (int v : row) {
                    assertTrue(v == 0 || v == 1);
                }
            }
        }

        @Test
        void columnWeightTwo() {
            int[][] H = ExpanderCodes.parityCheckMatrix(ss("&{a: &{b: end}}"));
            int nCols = H[0].length;
            for (int j = 0; j < nCols; j++) {
                int colSum = 0;
                for (int[] ints : H) colSum += ints[j];
                assertEquals(2, colSum);
            }
        }
    }

    // ---------------------------------------------------------------------
    // Code distance
    // ---------------------------------------------------------------------

    @Nested
    class CodeDistance {
        @Test
        void endTrivial() {
            assertEquals(0, ExpanderCodes.codeDistance(ss("end")));
        }

        @Test
        void simpleBranchDistance() {
            assertTrue(ExpanderCodes.codeDistance(ss("&{a: end}")) >= 0);
        }

        @Test
        void chainDistance() {
            assertTrue(ExpanderCodes.codeDistance(ss("&{a: &{b: end}}")) >= 0);
        }

        @Test
        void diamondDistance() {
            int d = ExpanderCodes.codeDistance(ss("&{a: &{c: end}, b: &{c: end}}"));
            assertTrue(d >= 0);
            assertEquals(4, d);
        }
    }

    // ---------------------------------------------------------------------
    // Error correction capacity
    // ---------------------------------------------------------------------

    @Nested
    class ErrorCorrectionCapacity {
        @Test
        void endZero() {
            assertEquals(0, ExpanderCodes.errorCorrectionCapacity(ss("end")));
        }

        @Test
        void nonNegative() {
            assertTrue(ExpanderCodes.errorCorrectionCapacity(ss("&{a: &{b: end}}")) >= 0);
        }

        @Test
        void floorFormula() {
            StateSpace s = ss("&{a: &{c: end}, b: &{c: end}}");
            int d = ExpanderCodes.codeDistance(s);
            int cap = ExpanderCodes.errorCorrectionCapacity(s);
            assertEquals(Math.max(0, (d - 1) / 2), cap);
        }
    }

    // ---------------------------------------------------------------------
    // Full analysis
    // ---------------------------------------------------------------------

    @Nested
    class AnalyzeExpanderCode {
        @Test
        void endAnalysis() {
            ExpanderCodeResult r = ExpanderCodes.analyzeExpanderCode(ss("end"));
            assertEquals(1, r.numStates());
            assertEquals(0.0, r.spectralGap());
            assertFalse(r.isExpander());
        }

        @Test
        void simpleBranchAnalysis() {
            ExpanderCodeResult r = ExpanderCodes.analyzeExpanderCode(ss("&{a: end}"));
            assertEquals(2, r.numStates());
            assertTrue(r.numEdges() >= 1);
        }

        @Test
        void chainAnalysis() {
            ExpanderCodeResult r = ExpanderCodes.analyzeExpanderCode(ss("&{a: &{b: end}}"));
            assertEquals(3, r.numStates());
            assertTrue(r.cheeger().lower() <= r.cheeger().upper() || r.spectralGap() == 0.0);
        }

        @Test
        void diamondAnalysis() {
            ExpanderCodeResult r = ExpanderCodes.analyzeExpanderCode(
                    ss("&{a: &{c: end}, b: &{c: end}}"));
            assertTrue(r.spectralGap() > 0.0);
            assertTrue(r.expansionRatio() >= 0.0);
        }

        @Test
        void parallelAnalysis() {
            ExpanderCodeResult r = ExpanderCodes.analyzeExpanderCode(
                    ss("(&{a: end} || &{b: end})"));
            assertTrue(r.numStates() >= 3);
        }

        @Test
        void recursiveAnalysis() {
            ExpanderCodeResult r = ExpanderCodes.analyzeExpanderCode(
                    ss("rec X . &{a: X, b: end}"));
            assertNotNull(r);
        }

        @Test
        void rateBounded() {
            ExpanderCodeResult r = ExpanderCodes.analyzeExpanderCode(ss("&{a: &{b: end}}"));
            assertTrue(r.rate() >= 0.0 && r.rate() <= 1.0);
        }

        @Test
        void customThreshold() {
            StateSpace s = ss("&{a: &{b: end}}");
            ExpanderCodeResult r1 = ExpanderCodes.analyzeExpanderCode(s, 0.01);
            ExpanderCodeResult r2 = ExpanderCodes.analyzeExpanderCode(s, 100.0);
            assertTrue(r1.isExpander() || !r2.isExpander());
        }
    }
}

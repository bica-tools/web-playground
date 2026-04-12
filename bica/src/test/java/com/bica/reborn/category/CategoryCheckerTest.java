package com.bica.reborn.category;

import com.bica.reborn.ast.SessionType;
import com.bica.reborn.lattice.LatticeChecker;
import com.bica.reborn.parser.Parser;
import com.bica.reborn.statespace.ProductStateSpace;
import com.bica.reborn.statespace.StateSpace;
import com.bica.reborn.statespace.StateSpaceBuilder;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Tests for {@link CategoryChecker} — the SessLat category of session-type lattices.
 */
class CategoryCheckerTest {

    private static SessionType parse(String input) {
        return Parser.parse(input);
    }

    private static StateSpace build(String input) {
        return StateSpaceBuilder.build(parse(input));
    }

    // =========================================================================
    // Identity morphism
    // =========================================================================

    @Nested
    class IdentityTests {

        @Test
        void identityOnEnd() {
            StateSpace ss = build("end");
            LatticeHomomorphism id = CategoryChecker.identityMorphism(ss);
            assertEquals(ss, id.source());
            assertEquals(ss, id.target());
            for (int s : ss.states()) {
                assertEquals(s, id.mapping().get(s));
            }
            assertTrue(id.preservesMeets());
            assertTrue(id.preservesJoins());
        }

        @Test
        void identityIsLatticeHomomorphism() {
            StateSpace ss = build("&{a: end, b: end}");
            LatticeHomomorphism id = CategoryChecker.identityMorphism(ss);
            assertTrue(CategoryChecker.isLatticeHomomorphism(ss, ss, id.mapping()));
        }

        @Test
        void identityOnBranch() {
            StateSpace ss = build("&{open: &{read: end, write: end}}");
            LatticeHomomorphism id = CategoryChecker.identityMorphism(ss);
            assertEquals(ss.states().size(), id.mapping().size());
            assertTrue(id.preservesMeets());
        }
    }

    // =========================================================================
    // Composition
    // =========================================================================

    @Nested
    class CompositionTests {

        @Test
        void composeIdentityWithItself() {
            StateSpace ss = build("&{a: end, b: end}");
            LatticeHomomorphism id = CategoryChecker.identityMorphism(ss);
            LatticeHomomorphism composed = CategoryChecker.compose(id, id);
            assertEquals(id.mapping(), composed.mapping());
        }

        @Test
        void composePreservesProperties() {
            StateSpace ss = build("&{a: end}");
            LatticeHomomorphism id = CategoryChecker.identityMorphism(ss);
            LatticeHomomorphism composed = CategoryChecker.compose(id, id);
            assertTrue(composed.preservesMeets());
            assertTrue(composed.preservesJoins());
        }

        @Test
        void composeIncompatibleThrows() {
            StateSpace ss1 = build("&{a: end}");
            StateSpace ss2 = build("&{a: end, b: end}");
            LatticeHomomorphism id1 = CategoryChecker.identityMorphism(ss1);
            LatticeHomomorphism id2 = CategoryChecker.identityMorphism(ss2);
            // id1 maps to ss1 states, id2 expects ss2 states — may or may not overlap
            // This tests that compose validates domain compatibility
            // If states don't overlap, it should throw
            if (!ss2.states().containsAll(id1.target().states())) {
                assertThrows(IllegalArgumentException.class,
                        () -> CategoryChecker.compose(id1, id2));
            }
        }

        @Test
        void composeAssociativity() {
            StateSpace ss = build("&{a: end, b: end}");
            LatticeHomomorphism f = CategoryChecker.identityMorphism(ss);
            LatticeHomomorphism g = CategoryChecker.identityMorphism(ss);
            LatticeHomomorphism h = CategoryChecker.identityMorphism(ss);
            LatticeHomomorphism fg = CategoryChecker.compose(f, g);
            LatticeHomomorphism gh = CategoryChecker.compose(g, h);
            LatticeHomomorphism fgh1 = CategoryChecker.compose(fg, h);
            LatticeHomomorphism fgh2 = CategoryChecker.compose(f, gh);
            assertEquals(fgh1.mapping(), fgh2.mapping());
        }
    }

    // =========================================================================
    // Lattice homomorphism check
    // =========================================================================

    @Nested
    class HomomorphismTests {

        @Test
        void identityIsHomomorphism() {
            StateSpace ss = build("&{a: end, b: end}");
            Map<Integer, Integer> id = new HashMap<>();
            for (int s : ss.states()) id.put(s, s);
            assertTrue(CategoryChecker.isLatticeHomomorphism(ss, ss, id));
        }

        @Test
        void incompleteMappingFails() {
            StateSpace ss = build("&{a: end, b: end}");
            // Empty mapping doesn't cover source states
            assertFalse(CategoryChecker.isLatticeHomomorphism(ss, ss, Map.of()));
        }

        @Test
        void mappingToWrongTargetFails() {
            StateSpace ss1 = build("&{a: end}");
            StateSpace ss2 = build("&{a: end, b: end}");
            Map<Integer, Integer> bad = new HashMap<>();
            for (int s : ss1.states()) bad.put(s, s + 1000); // out of range
            assertFalse(CategoryChecker.isLatticeHomomorphism(ss1, ss2, bad));
        }
    }

    // =========================================================================
    // Product projections
    // =========================================================================

    @Nested
    class ProjectionTests {

        @Test
        void projectionsFromSimpleProduct() {
            StateSpace left = build("&{a: end}");
            StateSpace right = build("&{b: end}");
            StateSpace product = ProductStateSpace.product(left, right);

            List<LatticeHomomorphism> projs = CategoryChecker.findProjections(
                    product, left, right);
            assertEquals(2, projs.size());

            // π₁ maps product top to left top
            assertEquals(left.top(), (int) projs.get(0).mapping().get(product.top()));
            // π₂ maps product top to right top
            assertEquals(right.top(), (int) projs.get(1).mapping().get(product.top()));

            // π₁ maps product bottom to left bottom
            assertEquals(left.bottom(), (int) projs.get(0).mapping().get(product.bottom()));
            // π₂ maps product bottom to right bottom
            assertEquals(right.bottom(), (int) projs.get(1).mapping().get(product.bottom()));
        }

        @Test
        void projectionsAreHomomorphisms() {
            StateSpace left = build("&{a: end}");
            StateSpace right = build("&{b: end}");
            StateSpace product = ProductStateSpace.product(left, right);

            List<LatticeHomomorphism> projs = CategoryChecker.findProjections(
                    product, left, right);
            for (LatticeHomomorphism pi : projs) {
                assertTrue(CategoryChecker.isLatticeHomomorphism(
                        pi.source(), pi.target(), pi.mapping()));
            }
        }
    }

    // =========================================================================
    // Universal property
    // =========================================================================

    @Nested
    class UniversalPropertyTests {

        @Test
        void simpleProductSatisfiesUniversalProperty() {
            StateSpace left = build("&{a: end}");
            StateSpace right = build("&{b: end}");
            StateSpace product = ProductStateSpace.product(left, right);

            assertTrue(CategoryChecker.checkProductUniversalProperty(product, left, right));
        }

        @Test
        void branchProductSatisfiesUniversalProperty() {
            StateSpace left = build("&{a: end, b: end}");
            StateSpace right = build("&{c: end}");
            StateSpace product = ProductStateSpace.product(left, right);

            assertTrue(CategoryChecker.checkProductUniversalProperty(product, left, right));
        }

        @Test
        void nonProductFailsUniversalProperty() {
            StateSpace ss = build("&{a: end, b: end}");
            StateSpace factor = build("&{a: end}");
            // ss is not a product of factor × factor
            assertFalse(CategoryChecker.checkProductUniversalProperty(ss, factor, factor));
        }
    }

    // =========================================================================
    // Product detection
    // =========================================================================

    @Nested
    class ProductDetectionTests {

        @Test
        void parallelTypeIsProduct() {
            StateSpace ss = build("(&{a: end} || &{b: end})");
            assertTrue(CategoryChecker.isProduct(ss));
        }

        @Test
        void simpleChainIsNotProduct() {
            StateSpace ss = build("&{a: end}");
            assertFalse(CategoryChecker.isProduct(ss));
        }

        @Test
        void endIsNotProduct() {
            StateSpace ss = build("end");
            assertFalse(CategoryChecker.isProduct(ss));
        }
    }
}

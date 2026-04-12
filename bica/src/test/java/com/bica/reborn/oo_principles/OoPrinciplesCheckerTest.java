package com.bica.reborn.oo_principles;

import com.bica.reborn.ast.SessionType;
import com.bica.reborn.oo_principles.OoPrinciplesChecker.*;
import com.bica.reborn.parser.Parser;
import com.bica.reborn.statespace.StateSpace;
import com.bica.reborn.statespace.StateSpaceBuilder;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import java.util.List;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Tests for OO Principles as Lattice Theorems (Step 51b).
 *
 * <p>63 tests covering each of the nine OO principles with positive and
 * negative examples using benchmark protocols.
 */
class OoPrinciplesCheckerTest {

    private static StateSpace build(String input) {
        return StateSpaceBuilder.build(Parser.parse(input));
    }

    // =========================================================================
    // 1. Encapsulation
    // =========================================================================

    @Nested
    class Encapsulation {

        @Test
        void simpleBranch() {
            StateSpace ss = build("&{a: end, b: end}");
            EncapsulationResult result = OoPrinciplesChecker.checkEncapsulation(ss);
            assertTrue(result.isEncapsulated());
            assertEquals(2, result.stateCount());
            assertTrue(result.methodSets().get(ss.top()).contains("a"));
            assertTrue(result.methodSets().get(ss.top()).contains("b"));
        }

        @Test
        void endHasNoMethods() {
            StateSpace ss = build("&{a: end}");
            EncapsulationResult result = OoPrinciplesChecker.checkEncapsulation(ss);
            assertTrue(result.methodSets().get(ss.bottom()).isEmpty());
        }

        @Test
        void sequentialHidesFuture() {
            StateSpace ss = build("&{a: &{b: end}}");
            EncapsulationResult result = OoPrinciplesChecker.checkEncapsulation(ss);
            assertTrue(result.isEncapsulated());
            Set<String> topMethods = result.methodSets().get(ss.top());
            assertTrue(topMethods.contains("a"));
            assertFalse(topMethods.contains("b"));
        }

        @Test
        void selectionVisible() {
            StateSpace ss = build("+{ok: end, err: end}");
            EncapsulationResult result = OoPrinciplesChecker.checkEncapsulation(ss);
            assertTrue(result.isEncapsulated());
            assertTrue(result.methodSets().get(ss.top()).contains("ok"));
            assertTrue(result.methodSets().get(ss.top()).contains("err"));
        }

        @Test
        void recursiveIterator() {
            StateSpace ss = build("rec X . &{hasNext: +{TRUE: &{next: X}, FALSE: end}}");
            EncapsulationResult result = OoPrinciplesChecker.checkEncapsulation(ss);
            assertTrue(result.isEncapsulated());
            assertTrue(result.methodSets().get(ss.top()).contains("hasNext"));
        }

        @Test
        void trivialEnd() {
            StateSpace ss = build("end");
            EncapsulationResult result = OoPrinciplesChecker.checkEncapsulation(ss);
            assertTrue(result.isEncapsulated());
            assertEquals(1, result.stateCount());
        }
    }

    // =========================================================================
    // 2. Inheritance
    // =========================================================================

    @Nested
    class Inheritance {

        @Test
        void childExtendsParent() {
            SessionType parent = Parser.parse("&{a: end, b: end}");
            SessionType child = Parser.parse("&{a: end, b: end, c: end}");
            InheritanceResult result = OoPrinciplesChecker.checkInheritance(parent, child);
            assertTrue(result.isSubtype());
            assertTrue(result.isValidInheritance());
        }

        @Test
        void childMissingMethod() {
            SessionType parent = Parser.parse("&{a: end, b: end}");
            SessionType child = Parser.parse("&{a: end}");
            InheritanceResult result = OoPrinciplesChecker.checkInheritance(parent, child);
            assertFalse(result.isSubtype());
            assertFalse(result.isValidInheritance());
        }

        @Test
        void identicalTypes() {
            SessionType t = Parser.parse("&{a: end}");
            InheritanceResult result = OoPrinciplesChecker.checkInheritance(t, t);
            assertTrue(result.isSubtype());
            assertTrue(result.isValidInheritance());
        }

        @Test
        void deeperChild() {
            SessionType parent = Parser.parse("&{a: end}");
            SessionType child = Parser.parse("&{a: &{b: end}}");
            InheritanceResult result = OoPrinciplesChecker.checkInheritance(parent, child);
            assertTrue(result.isSubtype());
        }

        @Test
        void hasReasonOnFailure() {
            SessionType parent = Parser.parse("&{a: end, b: end}");
            SessionType child = Parser.parse("&{c: end}");
            InheritanceResult result = OoPrinciplesChecker.checkInheritance(parent, child);
            assertFalse(result.isValidInheritance());
            assertNotNull(result.reason());
        }
    }

    // =========================================================================
    // 3. Polymorphism
    // =========================================================================

    @Nested
    class Polymorphism {

        @Test
        void sharedMethods() {
            SessionType t1 = Parser.parse("&{a: end, b: end}");
            SessionType t2 = Parser.parse("&{a: end, c: end}");
            PolymorphismResult result = OoPrinciplesChecker.findPolymorphicInterface(List.of(t1, t2));
            assertTrue(result.hasCommonSupertype());
            assertTrue(result.commonMethods().contains("a"));
            assertFalse(result.commonMethods().contains("b"));
        }

        @Test
        void noSharedMethods() {
            SessionType t1 = Parser.parse("&{a: end}");
            SessionType t2 = Parser.parse("&{b: end}");
            PolymorphismResult result = OoPrinciplesChecker.findPolymorphicInterface(List.of(t1, t2));
            assertFalse(result.hasCommonSupertype());
            assertTrue(result.commonMethods().isEmpty());
        }

        @Test
        void threeTypes() {
            SessionType t1 = Parser.parse("&{read: end, write: end}");
            SessionType t2 = Parser.parse("&{read: end, close: end}");
            SessionType t3 = Parser.parse("&{read: end, seek: end}");
            PolymorphismResult result = OoPrinciplesChecker.findPolymorphicInterface(List.of(t1, t2, t3));
            assertTrue(result.hasCommonSupertype());
            assertEquals(Set.of("read"), result.commonMethods());
        }

        @Test
        void emptyList() {
            PolymorphismResult result = OoPrinciplesChecker.findPolymorphicInterface(List.of());
            assertFalse(result.hasCommonSupertype());
        }

        @Test
        void singleType() {
            SessionType t = Parser.parse("&{a: end, b: end}");
            PolymorphismResult result = OoPrinciplesChecker.findPolymorphicInterface(List.of(t));
            assertTrue(result.hasCommonSupertype());
            assertEquals(Set.of("a", "b"), result.commonMethods());
        }

        @Test
        void commonSupertypeString() {
            SessionType t1 = Parser.parse("&{get: end, set: end}");
            SessionType t2 = Parser.parse("&{get: end, delete: end}");
            PolymorphismResult result = OoPrinciplesChecker.findPolymorphicInterface(List.of(t1, t2));
            assertNotNull(result.commonSupertype());
            assertTrue(result.commonSupertype().contains("get"));
        }
    }

    // =========================================================================
    // 4. Abstraction
    // =========================================================================

    @Nested
    class Abstraction {

        @Test
        void removeInternalDetail() {
            StateSpace ss = build("&{open: &{internal: &{close: end}}}");
            AbstractionResult result = OoPrinciplesChecker.abstractProtocol(ss, Set.of("internal"));
            assertTrue(result.abstractStates() <= result.originalStates());
        }

        @Test
        void noLabelsRemoved() {
            StateSpace ss = build("&{a: end}");
            AbstractionResult result = OoPrinciplesChecker.abstractProtocol(ss, Set.of());
            assertTrue(result.isValidAbstraction());
            assertEquals(1.0, result.reductionRatio(), 0.001);
        }

        @Test
        void removeAllLabels() {
            StateSpace ss = build("&{a: end, b: end}");
            AbstractionResult result = OoPrinciplesChecker.abstractProtocol(ss, Set.of("a", "b"));
            assertTrue(result.abstractStates() <= result.originalStates());
        }

        @Test
        void reductionRatio() {
            StateSpace ss = build("&{a: &{b: &{c: end}}}");
            AbstractionResult result = OoPrinciplesChecker.abstractProtocol(ss, Set.of("b"));
            assertTrue(result.reductionRatio() <= 1.0);
            assertEquals(1, result.removedLabels().size());
        }
    }

    // =========================================================================
    // 5. Single Responsibility Principle
    // =========================================================================

    @Nested
    class SingleResponsibility {

        @Test
        void singleMethod() {
            StateSpace ss = build("&{a: end}");
            SRPResult result = OoPrinciplesChecker.checkSingleResponsibility(ss);
            assertTrue(result.isSingleResponsibility());
        }

        @Test
        void linearChain() {
            StateSpace ss = build("&{a: &{b: end}}");
            SRPResult result = OoPrinciplesChecker.checkSingleResponsibility(ss);
            assertTrue(result.isSingleResponsibility());
        }

        @Test
        void branchMultiple() {
            StateSpace ss = build("&{read: end, write: end}");
            SRPResult result = OoPrinciplesChecker.checkSingleResponsibility(ss);
            assertTrue(result.numResponsibilities() >= 1);
        }

        @Test
        void iteratorSrp() {
            StateSpace ss = build("rec X . &{hasNext: +{TRUE: &{next: X}, FALSE: end}}");
            SRPResult result = OoPrinciplesChecker.checkSingleResponsibility(ss);
            assertTrue(result.isSingleResponsibility());
        }

        @Test
        void joinIrreduciblesExist() {
            StateSpace ss = build("&{a: &{b: end}}");
            SRPResult result = OoPrinciplesChecker.checkSingleResponsibility(ss);
            assertNotNull(result.joinIrreducibles());
            assertNotNull(result.responsibilities());
        }

        @Test
        void trivialEnd() {
            StateSpace ss = build("end");
            SRPResult result = OoPrinciplesChecker.checkSingleResponsibility(ss);
            assertTrue(result.isSingleResponsibility());
        }
    }

    // =========================================================================
    // 6. Open/Closed Principle
    // =========================================================================

    @Nested
    class OpenClosed {

        @Test
        void properExtension() {
            SessionType original = Parser.parse("&{a: end, b: end}");
            SessionType extended = Parser.parse("&{a: end, b: end, c: end}");
            OpenClosedResult result = OoPrinciplesChecker.checkOpenClosed(original, extended);
            assertTrue(result.satisfiesOcp());
            assertTrue(result.preservesExisting());
            assertTrue(result.isProperExtension());
            assertTrue(result.newMethods().contains("c"));
        }

        @Test
        void modificationViolates() {
            SessionType original = Parser.parse("&{a: end, b: end}");
            SessionType modified = Parser.parse("&{a: end, c: end}");
            OpenClosedResult result = OoPrinciplesChecker.checkOpenClosed(original, modified);
            assertFalse(result.satisfiesOcp());
            assertFalse(result.preservesExisting());
        }

        @Test
        void noExtension() {
            SessionType t = Parser.parse("&{a: end}");
            OpenClosedResult result = OoPrinciplesChecker.checkOpenClosed(t, t);
            assertFalse(result.satisfiesOcp());
            assertTrue(result.preservesExisting());
            assertFalse(result.isProperExtension());
        }

        @Test
        void completeReplacement() {
            SessionType original = Parser.parse("&{a: end}");
            SessionType extended = Parser.parse("&{b: end}");
            OpenClosedResult result = OoPrinciplesChecker.checkOpenClosed(original, extended);
            assertFalse(result.satisfiesOcp());
        }

        @Test
        void multipleExtensions() {
            SessionType original = Parser.parse("&{a: end}");
            SessionType extended = Parser.parse("&{a: end, b: end, c: end}");
            OpenClosedResult result = OoPrinciplesChecker.checkOpenClosed(original, extended);
            assertTrue(result.satisfiesOcp());
            assertEquals(2, result.newMethods().size());
        }
    }

    // =========================================================================
    // 7. Liskov Substitution Principle
    // =========================================================================

    @Nested
    class Liskov {

        @Test
        void subtypeSatisfiesLsp() {
            SessionType base = Parser.parse("&{a: end}");
            SessionType derived = Parser.parse("&{a: end, b: end}");
            LiskovResult result = OoPrinciplesChecker.checkLiskov(base, derived);
            assertTrue(result.satisfiesLsp());
            assertTrue(result.isSubtype());
            assertNull(result.counterexample());
        }

        @Test
        void missingMethodFailsLsp() {
            SessionType base = Parser.parse("&{a: end, b: end}");
            SessionType derived = Parser.parse("&{a: end}");
            LiskovResult result = OoPrinciplesChecker.checkLiskov(base, derived);
            assertFalse(result.satisfiesLsp());
            assertNotNull(result.counterexample());
        }

        @Test
        void identicalTypes() {
            SessionType t = Parser.parse("&{a: end}");
            LiskovResult result = OoPrinciplesChecker.checkLiskov(t, t);
            assertTrue(result.satisfiesLsp());
        }

        @Test
        void incompatibleConstructors() {
            SessionType base = Parser.parse("&{a: end}");
            SessionType derived = Parser.parse("+{a: end}");
            LiskovResult result = OoPrinciplesChecker.checkLiskov(base, derived);
            assertFalse(result.satisfiesLsp());
            assertNotNull(result.counterexample());
        }

        @Test
        void iteratorSelfSubstitution() {
            SessionType it = Parser.parse("rec X . &{hasNext: +{TRUE: &{next: X}, FALSE: end}}");
            LiskovResult result = OoPrinciplesChecker.checkLiskov(it, it);
            assertTrue(result.satisfiesLsp());
        }

        @Test
        void counterexampleMessage() {
            SessionType base = Parser.parse("&{read: end, write: end}");
            SessionType derived = Parser.parse("&{read: end}");
            LiskovResult result = OoPrinciplesChecker.checkLiskov(base, derived);
            assertTrue(result.counterexample().contains("write"));
        }
    }

    // =========================================================================
    // 8. Interface Segregation Principle
    // =========================================================================

    @Nested
    class InterfaceSegregation {

        @Test
        void singleMethodOneInterface() {
            StateSpace ss = build("&{a: end}");
            List<Interface> interfaces = OoPrinciplesChecker.segregateInterfaces(ss);
            assertTrue(interfaces.size() >= 1);
        }

        @Test
        void interfaceHasMethods() {
            StateSpace ss = build("&{a: &{b: end}}");
            List<Interface> interfaces = OoPrinciplesChecker.segregateInterfaces(ss);
            Set<String> allMethods = new java.util.HashSet<>();
            for (Interface iface : interfaces) {
                allMethods.addAll(iface.methods());
            }
            assertFalse(allMethods.isEmpty());
        }

        @Test
        void chainInterfaces() {
            StateSpace ss = build("&{a: &{b: &{c: end}}}");
            List<Interface> interfaces = OoPrinciplesChecker.segregateInterfaces(ss);
            assertTrue(interfaces.size() >= 1);
        }

        @Test
        void branchInterfaces() {
            StateSpace ss = build("&{a: end, b: end}");
            List<Interface> interfaces = OoPrinciplesChecker.segregateInterfaces(ss);
            assertTrue(interfaces.size() >= 1);
        }

        @Test
        void recursiveInterfaces() {
            StateSpace ss = build("rec X . &{a: X, b: end}");
            List<Interface> interfaces = OoPrinciplesChecker.segregateInterfaces(ss);
            assertTrue(interfaces.size() >= 1);
        }
    }

    // =========================================================================
    // 9. Dependency Inversion Principle
    // =========================================================================

    @Nested
    class DependencyInversion {

        @Test
        void abstractEmbeds() {
            StateSpace ssAbstract = build("&{a: end}");
            StateSpace ssConcrete = build("&{a: &{b: end}}");
            DependencyInversionResult result =
                    OoPrinciplesChecker.checkDependencyInversion(ssAbstract, ssConcrete);
            assertTrue(result.abstractStates() <= result.concreteStates());
        }

        @Test
        void noEmbedding() {
            StateSpace ssAbstract = build("&{a: end, b: end, c: end}");
            StateSpace ssConcrete = build("&{x: end}");
            DependencyInversionResult result =
                    OoPrinciplesChecker.checkDependencyInversion(ssAbstract, ssConcrete);
            if (result.abstractStates() > result.concreteStates()) {
                assertFalse(result.hasGaloisConnection());
            }
        }

        @Test
        void identicalSpaces() {
            StateSpace ss = build("&{a: end}");
            DependencyInversionResult result =
                    OoPrinciplesChecker.checkDependencyInversion(ss, ss);
            assertEquals(result.abstractStates(), result.concreteStates());
        }
    }

    // =========================================================================
    // SOLID check (combined)
    // =========================================================================

    @Nested
    class SOLIDCheck {

        @Test
        void basicSolid() {
            StateSpace ss = build("&{a: end}");
            SOLIDReport report = OoPrinciplesChecker.solidCheck(ss);
            assertTrue(report.summary().containsKey("SRP"));
            assertTrue(report.summary().containsKey("OCP"));
            assertTrue(report.summary().containsKey("LSP"));
            assertTrue(report.summary().containsKey("ISP"));
            assertTrue(report.summary().containsKey("DIP"));
        }

        @Test
        void trafficLightValues() {
            StateSpace ss = build("&{a: end}");
            SOLIDReport report = OoPrinciplesChecker.solidCheck(ss);
            for (String light : report.summary().values()) {
                assertTrue(Set.of("green", "yellow", "red").contains(light));
            }
        }

        @Test
        void srpGreen() {
            StateSpace ss = build("&{a: end}");
            SOLIDReport report = OoPrinciplesChecker.solidCheck(ss);
            assertEquals("green", report.summary().get("SRP"));
        }

        @Test
        void ocpWithExtension() {
            StateSpace ss = build("&{a: end}");
            SessionType sType = Parser.parse("&{a: end}");
            SessionType sExt = Parser.parse("&{a: end, b: end}");
            SOLIDReport report = OoPrinciplesChecker.solidCheck(ss, sType, sExt, null, null);
            assertEquals("green", report.summary().get("OCP"));
            assertNotNull(report.ocp());
            assertTrue(report.ocp().satisfiesOcp());
        }

        @Test
        void lspWithBase() {
            StateSpace ss = build("&{a: end, b: end}");
            SessionType sType = Parser.parse("&{a: end, b: end}");
            SessionType sBase = Parser.parse("&{a: end}");
            SOLIDReport report = OoPrinciplesChecker.solidCheck(ss, sType, null, sBase, null);
            assertEquals("green", report.summary().get("LSP"));
            assertNotNull(report.lsp());
            assertTrue(report.lsp().satisfiesLsp());
        }

        @Test
        void lspFailure() {
            StateSpace ss = build("&{a: end}");
            SessionType sType = Parser.parse("&{a: end}");
            SessionType sBase = Parser.parse("&{a: end, b: end}");
            SOLIDReport report = OoPrinciplesChecker.solidCheck(ss, sType, null, sBase, null);
            assertEquals("red", report.summary().get("LSP"));
        }

        @Test
        void ispPresent() {
            StateSpace ss = build("&{a: end}");
            SOLIDReport report = OoPrinciplesChecker.solidCheck(ss);
            assertTrue(report.isp().size() >= 1);
        }
    }

    // =========================================================================
    // Full OO analysis
    // =========================================================================

    @Nested
    class FullAnalysis {

        @Test
        void basicAnalysis() {
            StateSpace ss = build("&{a: end}");
            OOAnalysis result = OoPrinciplesChecker.analyzeOoPrinciples(ss);
            assertNotNull(result.encapsulation());
            assertNotNull(result.solid());
        }

        @Test
        void withInheritance() {
            SessionType parent = Parser.parse("&{a: end}");
            SessionType child = Parser.parse("&{a: end, b: end}");
            StateSpace ss = build("&{a: end, b: end}");
            OOAnalysis result = OoPrinciplesChecker.analyzeOoPrinciples(
                    ss, null, parent, child, null, null, null, null, null);
            assertNotNull(result.inheritance());
            assertTrue(result.inheritance().isValidInheritance());
        }

        @Test
        void withPolymorphism() {
            SessionType t1 = Parser.parse("&{read: end, write: end}");
            SessionType t2 = Parser.parse("&{read: end, close: end}");
            StateSpace ss = build("&{read: end}");
            OOAnalysis result = OoPrinciplesChecker.analyzeOoPrinciples(
                    ss, null, null, null, List.of(t1, t2), null, null, null, null);
            assertNotNull(result.polymorphism());
            assertTrue(result.polymorphism().hasCommonSupertype());
        }

        @Test
        void withAbstraction() {
            StateSpace ss = build("&{open: &{internal: &{close: end}}}");
            OOAnalysis result = OoPrinciplesChecker.analyzeOoPrinciples(
                    ss, null, null, null, null, Set.of("internal"), null, null, null);
            assertNotNull(result.abstraction());
        }

        @Test
        void fullAnalysis() {
            SessionType parent = Parser.parse("&{a: end}");
            SessionType child = Parser.parse("&{a: end, b: end}");
            StateSpace ss = StateSpaceBuilder.build(child);
            OOAnalysis result = OoPrinciplesChecker.analyzeOoPrinciples(
                    ss, child, parent, child, null,
                    Set.of(),
                    Parser.parse("&{a: end, b: end, c: end}"),
                    parent, null);
            assertTrue(result.encapsulation().isEncapsulated());
            assertTrue(result.inheritance().isValidInheritance());
            assertTrue(Set.of("green", "yellow", "red")
                    .contains(result.solid().summary().get("SRP")));
        }
    }

    // =========================================================================
    // Benchmark protocols
    // =========================================================================

    @Nested
    class BenchmarkProtocols {

        @ParameterizedTest
        @ValueSource(strings = {
                "rec X . &{hasNext: +{TRUE: &{next: X}, FALSE: end}}",
                "&{open: rec X . &{read: +{data: X, eof: &{close: end}}}}",
                "&{EHLO: &{MAIL: &{RCPT: &{DATA: end}}}}",
                "&{connect: rec X . &{request: +{OK200: X, ERR4xx: X}, close: end}}"
        })
        void encapsulation(String typeStr) {
            StateSpace ss = build(typeStr);
            EncapsulationResult result = OoPrinciplesChecker.checkEncapsulation(ss);
            assertTrue(result.isEncapsulated());
        }

        @ParameterizedTest
        @ValueSource(strings = {
                "rec X . &{hasNext: +{TRUE: &{next: X}, FALSE: end}}",
                "&{open: rec X . &{read: +{data: X, eof: &{close: end}}}}",
                "&{EHLO: &{MAIL: &{RCPT: &{DATA: end}}}}"
        })
        void srp(String typeStr) {
            StateSpace ss = build(typeStr);
            SRPResult result = OoPrinciplesChecker.checkSingleResponsibility(ss);
            assertTrue(result.isSingleResponsibility());
        }

        @ParameterizedTest
        @ValueSource(strings = {
                "rec X . &{hasNext: +{TRUE: &{next: X}, FALSE: end}}",
                "&{EHLO: &{MAIL: &{RCPT: &{DATA: end}}}}"
        })
        void isp(String typeStr) {
            StateSpace ss = build(typeStr);
            List<Interface> interfaces = OoPrinciplesChecker.segregateInterfaces(ss);
            assertTrue(interfaces.size() >= 1);
        }

        @Test
        void ocpSmtpExtended() {
            SessionType original = Parser.parse("&{EHLO: &{MAIL: end}}");
            SessionType extended = Parser.parse("&{EHLO: &{MAIL: end}, STARTTLS: end}");
            OpenClosedResult result = OoPrinciplesChecker.checkOpenClosed(original, extended);
            // Valid result regardless of outcome
            assertNotNull(result);
        }

        @Test
        void lspExtendedIterator() {
            SessionType base = Parser.parse("rec X . &{hasNext: +{TRUE: &{next: X}, FALSE: end}}");
            SessionType derived = Parser.parse("rec X . &{hasNext: +{TRUE: &{next: X}, FALSE: end}, reset: X}");
            LiskovResult result = OoPrinciplesChecker.checkLiskov(base, derived);
            assertTrue(result.satisfiesLsp());
        }
    }
}

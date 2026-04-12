package com.bica.reborn.mars_rover;

import com.bica.reborn.mars_rover.MarsRoverChecker.*;
import com.bica.reborn.parser.Parser;

import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.util.*;

import static com.bica.reborn.mars_rover.MarsRoverChecker.*;
import static org.junit.jupiter.api.Assertions.*;

/**
 * Tests for Mars Rover mode-transition lattice (Step 803).
 * Mirrors all 39 Python tests from test_mars_rover.py.
 */
class MarsRoverCheckerTest {

    // -----------------------------------------------------------------------
    @Nested
    class TestEnums {
        @Test
        void modes() {
            assertEquals(6, RoverMode.values().length);
            assertEquals("SAFE", RoverMode.SAFE.name());
        }

        @Test
        void phases() {
            assertEquals(3, SolPhase.values().length);
            assertEquals("wake", SolPhase.WAKE.value);
        }

        @Test
        void sensors() {
            assertEquals(4, SensorPipeline.values().length);
            assertEquals("imu", SensorPipeline.IMU.value);
        }
    }

    // -----------------------------------------------------------------------
    @Nested
    class TestSupervisorOrder {
        @Test
        void reflexive() {
            var s = new SupervisorState(RoverMode.SAFE, SolPhase.WAKE, Set.of());
            assertTrue(s.leq(s));
            assertFalse(s.lt(s));
        }

        @Test
        void chain() {
            var a = new SupervisorState(RoverMode.SAFE, SolPhase.WAKE, Set.of());
            var b = new SupervisorState(RoverMode.MIN_OPS, SolPhase.WAKE,
                    Set.of(SensorPipeline.IMU));
            var c = new SupervisorState(RoverMode.NOMINAL_SCIENCE, SolPhase.SLEEP,
                    Set.of(SensorPipeline.IMU, SensorPipeline.HAZCAM));
            assertTrue(a.leq(b));
            assertTrue(b.leq(c));
            assertTrue(a.lt(c));
        }

        @Test
        void incomparable() {
            var a = new SupervisorState(RoverMode.NOMINAL_DRIVE, SolPhase.WAKE,
                    Set.of(SensorPipeline.IMU));
            var b = new SupervisorState(RoverMode.MIN_OPS, SolPhase.SLEEP,
                    Set.of(SensorPipeline.IMU));
            assertFalse(a.leq(b));
            assertFalse(b.leq(a));
        }
    }

    // -----------------------------------------------------------------------
    @Nested
    class TestTypeStrings {
        @Test void modeTypeParses() { Parser.parse(roverModeType()); }

        @Test void solCycleParses() { Parser.parse(solCycleType()); }

        @Test void sensorPipelineParses() { Parser.parse(sensorPipelineType("imu")); }

        @Test
        void parallelSensorDefault() {
            String s = parallelSensorType(null);
            Parser.parse(s);
            assertTrue(s.contains("||"));
        }

        @Test
        void parallelSensorSubset() {
            String s = parallelSensorType(List.of(SensorPipeline.IMU, SensorPipeline.HAZCAM));
            Parser.parse(s);
            assertTrue(s.contains("imu"));
            assertTrue(s.contains("hazcam"));
        }

        @Test
        void parallelSensorEmpty() {
            assertEquals("end", parallelSensorType(List.of()));
        }

        @Test
        void parallelSensorSingle() {
            Parser.parse(parallelSensorType(List.of(SensorPipeline.DRILL)));
        }

        @Test
        void fullRoverParses() {
            Parser.parse(fullRoverType(List.of(SensorPipeline.IMU)));
        }
    }

    // -----------------------------------------------------------------------
    @Nested
    class TestBuildProtocol {
        @Test
        void minimalBuilds() {
            var rp = buildMinimalRover();
            assertTrue(rp.numStates() > 0);
            assertTrue(rp.numTransitions() > 0);
            assertEquals("minimal", rp.name());
        }

        @Test
        void minimalIsLattice() {
            assertTrue(buildMinimalRover().isLattice());
        }

        @Test
        void fullProtocolBuilds() {
            var rp = buildRoverProtocol("full",
                    List.of(SensorPipeline.IMU, SensorPipeline.HAZCAM));
            assertTrue(rp.numStates() > 0);
        }

        @Test
        void protocolName() {
            var rp = buildRoverProtocol("curiosity", List.of(SensorPipeline.IMU));
            assertEquals("curiosity", rp.name());
        }
    }

    // -----------------------------------------------------------------------
    @Nested
    class TestCompression {
        @Test
        void naiveSizeSimple() {
            assertEquals(2L, naiveInterleavingSize(1, 2));
        }

        @Test
        void naiveSizeGrows() {
            assertEquals(369600L, naiveInterleavingSize(3, 4));
        }

        @Test
        void productLatticeSizeTest() {
            assertEquals(256L, productLatticeSize(3, 4));
            assertEquals(4L, productLatticeSize(1, 2));
        }

        @Test
        void compressionRatioPositive() {
            var rep = compressionReport(3, 4);
            assertTrue(rep.naiveSize() > rep.latticeSize());
            assertTrue(rep.compressionRatio() > 1.0);
        }

        @Test
        void compressionReportDefault() {
            var rep = compressionReport();
            assertEquals(369600L, rep.naiveSize());
            assertEquals(256L, rep.latticeSize());
        }
    }

    // -----------------------------------------------------------------------
    @Nested
    class TestMorphisms {
        @Test
        void phiReturnsSupervisor() {
            var rp = buildMinimalRover();
            assertNotNull(phiLatticeToSupervisor(0, rp.stateSpace()));
        }

        @Test
        void phiBottomIsSafe() {
            var rp = buildMinimalRover();
            assertEquals(RoverMode.SAFE, phiLatticeToSupervisor(0, rp.stateSpace()).mode());
        }

        @Test
        void phiTopIsScience() {
            var rp = buildMinimalRover();
            int n = rp.numStates();
            assertEquals(RoverMode.NOMINAL_SCIENCE,
                    phiLatticeToSupervisor(n - 1, rp.stateSpace()).mode());
        }

        @Test
        void psiInRange() {
            var rp = buildMinimalRover();
            var sup = new SupervisorState(RoverMode.NOMINAL_SCIENCE, SolPhase.SLEEP,
                    Set.of(SensorPipeline.values()));
            int s = psiSupervisorToLattice(sup, rp.stateSpace());
            assertTrue(s >= 0 && s < rp.numStates());
        }

        @Test
        void psiSafeBottom() {
            var rp = buildMinimalRover();
            var sup = new SupervisorState(RoverMode.SAFE, SolPhase.WAKE, Set.of());
            assertEquals(0, psiSupervisorToLattice(sup, rp.stateSpace()));
        }

        @Test
        void galoisPair() {
            assertTrue(isGaloisPair(buildMinimalRover().stateSpace()));
        }
    }

    // -----------------------------------------------------------------------
    @Nested
    class TestContingency {
        @Test
        void tableNonempty() {
            assertTrue(CONTINGENCY_TABLE.size() >= 5);
        }

        @Test
        void findBatteryLow() {
            var ct = findContingency(RoverMode.NOMINAL_SCIENCE, "battery_low");
            assertTrue(ct.isPresent());
            assertEquals(RoverMode.MIN_OPS, ct.get().toMode());
        }

        @Test
        void findImuFault() {
            var ct = findContingency(RoverMode.NOMINAL_DRIVE, "imu_fault");
            assertTrue(ct.isPresent());
            assertTrue(ct.get().safetyAction().contains("reset_imu"));
        }

        @Test
        void findMissing() {
            assertTrue(findContingency(RoverMode.SAFE, "nonexistent").isEmpty());
        }

        @Test
        void uncategorisedToSafe() {
            var ct = findContingency(RoverMode.NOMINAL_SCIENCE, "uncategorised");
            assertTrue(ct.isPresent());
            assertEquals(RoverMode.SAFE, ct.get().toMode());
        }
    }

    // -----------------------------------------------------------------------
    @Nested
    class TestGroundLink {
        @Test
        void windowsCount() {
            assertEquals(21, groundLinkWindows(7).size());
        }

        @Test
        void windowsPhases() {
            var ws = groundLinkWindows(2);
            Set<SolPhase> phases = new HashSet<>();
            for (var w : ws) phases.add(w.phase());
            assertEquals(Set.of(SolPhase.WAKE, SolPhase.COMM_WINDOW, SolPhase.SLEEP), phases);
        }

        @Test
        void windowsSols() {
            var ws = groundLinkWindows(3);
            Set<Integer> sols = new HashSet<>();
            for (var w : ws) sols.add(w.sol());
            assertEquals(Set.of(0, 1, 2), sols);
        }
    }

    // -----------------------------------------------------------------------
    @Nested
    class TestVerification {
        @Test
        void verifyMinimal() {
            var v = verifyRover(buildMinimalRover());
            assertTrue(v.get("is_lattice"));
            assertTrue(v.get("has_safe_bottom"));
            assertTrue(v.get("bounded_states"));
        }

        @Test
        void verifyKeys() {
            var v = verifyRover(buildMinimalRover());
            assertEquals(Set.of("is_lattice", "has_safe_bottom",
                    "sensors_independent", "bounded_states"), v.keySet());
        }
    }
}

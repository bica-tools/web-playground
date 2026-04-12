package com.bica.reborn.algebraic.tropical;

import com.bica.reborn.algebraic.zeta.Zeta;
import com.bica.reborn.parser.Parser;
import com.bica.reborn.statespace.StateSpace;
import com.bica.reborn.statespace.StateSpaceBuilder;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;

import static com.bica.reborn.algebraic.tropical.MaxPlusPaths.*;
import static org.junit.jupiter.api.Assertions.*;

/**
 * Tests for {@link MaxPlusPaths}: port of Python
 * {@code tests/test_max_plus_paths.py} (Step 30n). 76 tests translated 1:1.
 */
class MaxPlusPathsTest {

    private StateSpace build(String type) {
        return StateSpaceBuilder.build(Parser.parse(type));
    }

    // =======================================================================
    // Longest path
    // =======================================================================

    @Nested
    class LongestPath {
        @Test
        void endZero() {
            assertEquals(0, longestPathLength(build("end")));
        }

        @Test
        void singleBranch() {
            assertEquals(1, longestPathLength(build("&{a: end}")));
        }

        @Test
        void chainTwo() {
            assertEquals(2, longestPathLength(build("&{a: &{b: end}}")));
        }

        @Test
        void chainThree() {
            assertEquals(3, longestPathLength(build("&{a: &{b: &{c: end}}}")));
        }

        @Test
        void branchSymmetric() {
            assertEquals(1, longestPathLength(build("&{a: end, b: end}")));
        }

        @Test
        void branchAsymmetric() {
            assertEquals(2, longestPathLength(build("&{a: end, b: &{c: end}}")));
        }

        @Test
        void selectionSingle() {
            assertEquals(1, longestPathLength(build("+{a: end}")));
        }

        @Test
        void selectionAsymmetric() {
            assertEquals(2, longestPathLength(build("+{OK: &{data: end}, ERR: end}")));
        }

        @Test
        void parallelSimple() {
            assertEquals(0, longestPathLength(build("(end || end)")));
        }

        @Test
        void parallelNontrivial() {
            assertEquals(2, longestPathLength(build("(&{a: end} || &{b: end})")));
        }

        @Test
        void longestNonnegative() {
            for (String t : List.of("end", "&{a: end}", "+{x: end}", "&{a: &{b: end}}")) {
                assertTrue(longestPathLength(build(t)) >= 0);
            }
        }
    }

    // =======================================================================
    // Shortest path
    // =======================================================================

    @Nested
    class ShortestPath {
        @Test
        void endZero() {
            assertEquals(0, shortestPathLength(build("end")));
        }

        @Test
        void singleBranch() {
            assertEquals(1, shortestPathLength(build("&{a: end}")));
        }

        @Test
        void chainTwo() {
            assertEquals(2, shortestPathLength(build("&{a: &{b: end}}")));
        }

        @Test
        void branchAsymmetric() {
            assertEquals(1, shortestPathLength(build("&{a: end, b: &{c: end}}")));
        }

        @Test
        void shortestLeqLongest() {
            List<String> types = List.of(
                    "end", "&{a: end}", "&{a: &{b: end}}",
                    "&{a: end, b: &{c: end}}",
                    "+{OK: &{data: end}, ERR: end}");
            for (String t : types) {
                StateSpace ss = build(t);
                assertTrue(shortestPathLength(ss) <= longestPathLength(ss));
            }
        }

        @Test
        void shortestNonnegative() {
            for (String t : List.of("end", "&{a: end}", "+{x: end}")) {
                assertTrue(shortestPathLength(build(t)) >= 0);
            }
        }

        @Test
        void chainEqualShortestLongest() {
            StateSpace ss = build("&{a: &{b: &{c: end}}}");
            assertEquals(longestPathLength(ss), shortestPathLength(ss));
        }
    }

    // =======================================================================
    // Critical path
    // =======================================================================

    @Nested
    class CriticalPath {
        @Test
        void endSingleState() {
            StateSpace ss = build("end");
            List<Integer> path = criticalPath(ss);
            assertTrue(path.size() >= 1);
            assertTrue(path.contains(ss.top()));
        }

        @Test
        void chainIncludesTopAndBottom() {
            StateSpace ss = build("&{a: &{b: end}}");
            List<Integer> path = criticalPath(ss);
            assertEquals(ss.top(), path.get(0));
            assertEquals(ss.bottom(), path.get(path.size() - 1));
        }

        @Test
        void pathLengthMatchesLongest() {
            StateSpace ss = build("&{a: &{b: &{c: end}}}");
            List<Integer> path = criticalPath(ss);
            assertEquals(longestPathLength(ss), path.size() - 1);
        }

        @Test
        void pathValidTransitions() {
            StateSpace ss = build("&{a: end, b: &{c: end}}");
            List<Integer> path = criticalPath(ss);
            Map<Integer, Set<Integer>> adj = new HashMap<>();
            for (int s : ss.states()) adj.put(s, new HashSet<>());
            for (var t : ss.transitions()) adj.get(t.source()).add(t.target());
            for (int i = 0; i < path.size() - 1; i++) {
                assertTrue(adj.get(path.get(i)).contains(path.get(i + 1)),
                        "No transition from " + path.get(i) + " to " + path.get(i + 1));
            }
        }

        @Test
        void branchCriticalFollowsLongest() {
            StateSpace ss = build("&{a: end, b: &{c: end}}");
            assertEquals(2, criticalPath(ss).size() - 1);
        }

        @Test
        void selectionCriticalPath() {
            StateSpace ss = build("+{OK: &{data: end}, ERR: end}");
            List<Integer> path = criticalPath(ss);
            assertEquals(ss.top(), path.get(0));
            assertEquals(ss.bottom(), path.get(path.size() - 1));
        }
    }

    // =======================================================================
    // All-pairs longest
    // =======================================================================

    @Nested
    class AllPairs {
        @Test
        void endSingle() {
            double[][] L = allPairsLongestPaths(build("end"));
            assertEquals(1, L.length);
            assertEquals(0.0, L[0][0]);
        }

        @Test
        void diagonalZero() {
            double[][] L = allPairsLongestPaths(build("&{a: &{b: end}}"));
            for (int i = 0; i < L.length; i++) assertEquals(0.0, L[i][i]);
        }

        @Test
        void consistentWithLongest() {
            StateSpace ss = build("&{a: end, b: &{c: end}}");
            double[][] L = allPairsLongestPaths(ss);
            List<Integer> states = Zeta.stateList(ss);
            int topI = states.indexOf(ss.top());
            int botI = states.indexOf(ss.bottom());
            assertEquals(longestPathLength(ss), (int) L[topI][botI]);
        }

        @Test
        void unreachableNegInf() {
            StateSpace ss = build("&{a: end}");
            double[][] L = allPairsLongestPaths(ss);
            List<Integer> states = Zeta.stateList(ss);
            int topI = states.indexOf(ss.top());
            int botI = states.indexOf(ss.bottom());
            if (topI != botI) {
                assertEquals(NEG_INF, L[botI][topI]);
            }
        }

        @Test
        void triangleInequalityReversed() {
            double[][] L = allPairsLongestPaths(build("&{a: &{b: end}}"));
            int n = L.length;
            for (int i = 0; i < n; i++) {
                for (int j = 0; j < n; j++) {
                    assertTrue(L[i][j] >= NEG_INF);
                }
            }
        }

        @Test
        void chainAllPairs() {
            StateSpace ss = build("&{a: &{b: end}}");
            double[][] L = allPairsLongestPaths(ss);
            List<Integer> states = Zeta.stateList(ss);
            int topI = states.indexOf(ss.top());
            int botI = states.indexOf(ss.bottom());
            assertEquals(2, (int) L[topI][botI]);
        }
    }

    // =======================================================================
    // Width
    // =======================================================================

    @Nested
    class Width {
        @Test
        void endWidthOne() {
            assertEquals(1, pathWidth(build("end")));
        }

        @Test
        void chainWidthOne() {
            assertEquals(1, pathWidth(build("&{a: &{b: &{c: end}}}")));
        }

        @Test
        void singleBranchWidthOne() {
            assertEquals(1, pathWidth(build("&{a: end}")));
        }

        @Test
        void branchWidth() {
            assertTrue(pathWidth(build("&{a: end, b: end}")) >= 1);
        }

        @Test
        void parallelProductWidth() {
            assertTrue(pathWidth(build("(&{a: end} || &{b: end})")) >= 2);
        }

        @Test
        void widthPositive() {
            for (String t : List.of("end", "&{a: end}", "&{a: &{b: end}}")) {
                assertTrue(pathWidth(build(t)) >= 1);
            }
        }
    }

    // =======================================================================
    // Path count
    // =======================================================================

    @Nested
    class PathCount {
        @Test
        void endOnePath() {
            assertEquals(1L, totalPathsTopBottom(build("end")));
        }

        @Test
        void chainOnePath() {
            assertEquals(1L, totalPathsTopBottom(build("&{a: &{b: end}}")));
        }

        @Test
        void branchTwoPaths() {
            assertEquals(2L, totalPathsTopBottom(build("&{a: end, b: end}")));
        }

        @Test
        void branchThreePaths() {
            assertEquals(3L, totalPathsTopBottom(build("&{a: end, b: end, c: end}")));
        }

        @Test
        void diamondPaths() {
            assertEquals(2L, totalPathsTopBottom(build("&{a: &{c: end}, b: &{c: end}}")));
        }

        @Test
        void countPathsSrcTgt() {
            StateSpace ss = build("&{a: end}");
            assertEquals(1L, countPaths(ss, ss.top(), ss.bottom()));
        }

        @Test
        void countPathsSameState() {
            StateSpace ss = build("&{a: end}");
            assertEquals(1L, countPaths(ss, ss.top(), ss.top()));
        }

        @Test
        void selectionTwoPaths() {
            assertEquals(2L, totalPathsTopBottom(build("+{a: end, b: end}")));
        }

        @Test
        void nestedBranches() {
            assertEquals(2L, totalPathsTopBottom(build("&{a: &{c: end}, b: end}")));
        }

        @Test
        void pathCountPositive() {
            for (String t : List.of("end", "&{a: end}", "+{x: end}", "&{a: &{b: end}}")) {
                assertTrue(totalPathsTopBottom(build(t)) >= 1L);
            }
        }
    }

    // =======================================================================
    // Bottleneck
    // =======================================================================

    @Nested
    class Bottleneck {
        @Test
        void endBottleneckZero() {
            assertEquals(0.0, bottleneckPath(build("end")).value());
        }

        @Test
        void chainBottleneckOne() {
            assertEquals(1.0, bottleneckPath(build("&{a: end}")).value());
        }

        @Test
        void bottleneckPathValid() {
            StateSpace ss = build("&{a: &{b: end}}");
            BottleneckPath bp = bottleneckPath(ss);
            assertEquals(ss.top(), bp.path().get(0));
            assertEquals(ss.bottom(), bp.path().get(bp.path().size() - 1));
        }

        @Test
        void bottleneckValueNonneg() {
            for (String t : List.of("end", "&{a: end}", "&{a: &{b: end}}")) {
                assertTrue(bottleneckPath(build(t)).value() >= 0.0);
            }
        }

        @Test
        void bottleneckBranch() {
            StateSpace ss = build("&{a: end, b: &{c: end}}");
            BottleneckPath bp = bottleneckPath(ss);
            assertEquals(1.0, bp.value());
            assertTrue(bp.path().size() >= 2);
        }
    }

    // =======================================================================
    // Geodesic
    // =======================================================================

    @Nested
    class Geodesic {
        @Test
        void endGeodesic() {
            assertTrue(isGeodesic(build("end")));
        }

        @Test
        void chainGeodesic() {
            assertTrue(isGeodesic(build("&{a: &{b: end}}")));
        }

        @Test
        void symmetricBranchGeodesic() {
            assertTrue(isGeodesic(build("&{a: end, b: end}")));
        }

        @Test
        void asymmetricBranchNotGeodesic() {
            assertFalse(isGeodesic(build("&{a: end, b: &{c: end}}")));
        }

        @Test
        void selectionSymmetricGeodesic() {
            assertTrue(isGeodesic(build("+{a: end, b: end}")));
        }

        @Test
        void selectionAsymmetricNotGeodesic() {
            assertFalse(isGeodesic(build("+{OK: &{data: end}, ERR: end}")));
        }
    }

    // =======================================================================
    // Histogram
    // =======================================================================

    @Nested
    class Histogram {
        @Test
        void endHistogram() {
            assertEquals(Map.of(0, 1L), new TreeMap<>(pathHistogram(build("end"))));
        }

        @Test
        void chainHistogram() {
            assertEquals(Map.of(2, 1L), new TreeMap<>(pathHistogram(build("&{a: &{b: end}}"))));
        }

        @Test
        void branchSymmetricHistogram() {
            assertEquals(Map.of(1, 2L), new TreeMap<>(pathHistogram(build("&{a: end, b: end}"))));
        }

        @Test
        void branchAsymmetricHistogram() {
            Map<Integer, Long> h = pathHistogram(build("&{a: end, b: &{c: end}}"));
            assertEquals(1L, h.get(1));
            assertEquals(1L, h.get(2));
            assertEquals(2, h.size());
        }

        @Test
        void threeBranchesHistogram() {
            assertEquals(Map.of(1, 3L), new TreeMap<>(pathHistogram(build("&{a: end, b: end, c: end}"))));
        }

        @Test
        void histogramSumsToTotalPaths() {
            List<String> types = List.of(
                    "end", "&{a: end}", "&{a: end, b: end}",
                    "&{a: end, b: &{c: end}}",
                    "+{OK: &{data: end}, ERR: end}");
            for (String t : types) {
                StateSpace ss = build(t);
                long total = pathHistogram(ss).values().stream().mapToLong(Long::longValue).sum();
                assertEquals(totalPathsTopBottom(ss), total, "Type " + t);
            }
        }

        @Test
        void histogramKeysBetweenShortestLongest() {
            StateSpace ss = build("&{a: end, b: &{c: end}}");
            Map<Integer, Long> h = pathHistogram(ss);
            int s = shortestPathLength(ss);
            int l = longestPathLength(ss);
            for (int k : h.keySet()) {
                assertTrue(s <= k && k <= l);
            }
        }
    }

    // =======================================================================
    // Analyze
    // =======================================================================

    @Nested
    class Analyze {
        @Test
        void endAnalysis() {
            MaxPlusPathResult r = analyzeMaxPlusPaths(build("end"));
            assertEquals(0, r.longestPath());
            assertEquals(0, r.shortestPath());
            assertTrue(r.isGeodesic());
            assertEquals(1L, r.pathCountTopBottom());
        }

        @Test
        void chainAnalysis() {
            MaxPlusPathResult r = analyzeMaxPlusPaths(build("&{a: &{b: end}}"));
            assertEquals(2, r.longestPath());
            assertEquals(2, r.shortestPath());
            assertTrue(r.isGeodesic());
            assertEquals(1L, r.pathCountTopBottom());
            assertEquals(1, r.width());
            assertTrue(r.diameter() >= 2);
        }

        @Test
        void branchAnalysis() {
            MaxPlusPathResult r = analyzeMaxPlusPaths(build("&{a: end, b: &{c: end}}"));
            assertEquals(2, r.longestPath());
            assertEquals(1, r.shortestPath());
            assertFalse(r.isGeodesic());
            assertEquals(2L, r.pathCountTopBottom());
        }

        @Test
        void selectionAnalysis() {
            MaxPlusPathResult r = analyzeMaxPlusPaths(build("+{OK: end, ERR: end}"));
            assertEquals(1, r.longestPath());
            assertEquals(1, r.shortestPath());
            assertTrue(r.isGeodesic());
            assertEquals(2L, r.pathCountTopBottom());
        }

        @Test
        void resultFields() {
            MaxPlusPathResult r = analyzeMaxPlusPaths(build("&{a: end}"));
            assertNotNull(r.criticalPath());
            assertNotNull(r.allPairsLongest());
        }
    }

    // =======================================================================
    // Benchmarks
    // =======================================================================

    @Nested
    class Benchmarks {
        @Test
        void iterator() {
            MaxPlusPathResult r = analyzeMaxPlusPaths(
                    build("rec X . &{hasNext: +{TRUE: &{next: X}, FALSE: end}}"));
            assertTrue(r.longestPath() >= 1);
            assertTrue(r.shortestPath() >= 1);
            assertTrue(r.pathCountTopBottom() >= 1L);
        }

        @Test
        void fileObject() {
            MaxPlusPathResult r = analyzeMaxPlusPaths(
                    build("&{open: &{read: &{close: end}}}"));
            assertEquals(3, r.longestPath());
            assertEquals(3, r.shortestPath());
            assertTrue(r.isGeodesic());
            assertEquals(1, r.width());
        }

        @Test
        void parallelBranches() {
            MaxPlusPathResult r = analyzeMaxPlusPaths(build("(&{a: end} || &{b: end})"));
            assertEquals(2, r.longestPath());
            assertTrue(r.width() >= 2);
        }

        @Test
        void selectionBranchCombo() {
            MaxPlusPathResult r = analyzeMaxPlusPaths(
                    build("&{init: +{OK: &{run: end}, FAIL: end}}"));
            assertEquals(3, r.longestPath());
            assertEquals(2, r.shortestPath());
            assertFalse(r.isGeodesic());
        }

        @Test
        void recursiveShortestLeqLongest() {
            MaxPlusPathResult r = analyzeMaxPlusPaths(build("rec X . &{a: X, b: end}"));
            assertTrue(r.shortestPath() <= r.longestPath());
        }

        @Test
        void deepChain() {
            MaxPlusPathResult r = analyzeMaxPlusPaths(build("&{a: &{b: &{c: &{d: &{e: end}}}}}"));
            assertEquals(5, r.longestPath());
            assertEquals(5, r.shortestPath());
            assertTrue(r.isGeodesic());
            assertEquals(1L, r.pathCountTopBottom());
        }

        @Test
        void wideBranch() {
            MaxPlusPathResult r = analyzeMaxPlusPaths(build("&{a: end, b: end, c: end, d: end}"));
            assertEquals(1, r.longestPath());
            assertEquals(1, r.shortestPath());
            assertEquals(4L, r.pathCountTopBottom());
            assertTrue(r.isGeodesic());
        }
    }
}

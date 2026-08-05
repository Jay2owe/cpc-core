package sc.fiji.cpc.core;

import ij.ImagePlus;
import org.junit.Test;

import java.util.Arrays;
import java.util.List;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

public class MultiTargetSummaryTest {

    /** One object in each of the four possible states across two targets. */
    private static List<Channel> threeChannels() {
        ImagePlus source = Volumes.labels("src", new int[][][]{
                {{1, 0, 2, 0, 3, 0, 4}}});
        ImagePlus targetB = Volumes.labels("B", new int[][][]{
                {{5, 0, 6, 0, 0, 0, 0}}});
        ImagePlus targetC = Volumes.labels("C", new int[][][]{
                {{7, 0, 8, 0, 9, 0, 0}}});
        return Arrays.asList(
                Channel.of("src", source),
                Channel.of("B", targetB),
                Channel.of("C", targetC));
    }

    @Test
    public void countsEachCombinationExactlyOnce() {
        MultiTargetResult result = MultiTargetSummary.run(threeChannels()).get(0);

        assertEquals("src", result.sourceName());
        assertEquals(Arrays.asList("B", "C"), result.targetNames());
        assertEquals(4, result.sourceTotal());

        assertEquals(2, patternCount(result, "B + C"));   // objects 1 and 2
        assertEquals(1, patternCount(result, "C"));       // object 3
        assertEquals(1, patternCount(result, MultiTargetResult.NONE)); // object 4
        assertEquals(0, patternCount(result, "B"));
    }

    @Test
    public void combinationCountsPartitionTheSourceObjects() {
        MultiTargetResult result = MultiTargetSummary.run(threeChannels()).get(0);

        int total = 0;
        for (CombinationPattern pattern : result.patterns()) total += pattern.count();
        assertEquals(result.sourceTotal(), total);
    }

    @Test
    public void anyAndNoneAreDefinedEvenWhenNoPatternRowExists() {
        List<Channel> everythingHits = Arrays.asList(
                Channel.of("src", Volumes.labels("src", new int[][][]{{{1, 0, 2}}})),
                Channel.of("B", Volumes.labels("B", new int[][][]{{{5, 5, 5}}})));

        MultiTargetResult result = MultiTargetSummary.run(everythingHits).get(0);

        // No "None" row exists, because no object matched nothing. The count is
        // still available, which is what a script reading the non-coincident
        // total needs.
        assertEquals(0, patternRows(result, MultiTargetResult.NONE));
        assertEquals(0, result.noneCount());
        assertEquals(2, result.anyCount());
        assertEquals(100.0, result.percentAny(), 0.0);
        assertEquals(0.0, result.percentNone(), 0.0);
    }

    @Test
    public void nothingHitsGivesAnAllNoneBreakdown() {
        List<Channel> nothingHits = Arrays.asList(
                Channel.of("src", Volumes.labels("src", new int[][][]{{{1, 0, 0}}})),
                Channel.of("B", Volumes.labels("B", new int[][][]{{{0, 0, 5}}})));

        MultiTargetResult result = MultiTargetSummary.run(nothingHits).get(0);

        assertEquals(1, result.noneCount());
        assertEquals(0, result.anyCount());
        assertEquals(100.0, result.percentNone(), 0.0);
    }

    @Test
    public void emptySourceHasNoPatternsAndNoDivisionByZero() {
        List<Channel> emptySource = Arrays.asList(
                Channel.of("src", Volumes.labels("src", new int[][][]{{{0, 0}}})),
                Channel.of("B", Volumes.labels("B", new int[][][]{{{5, 5}}})));

        MultiTargetResult result = MultiTargetSummary.run(emptySource).get(0);

        assertTrue(result.patterns().isEmpty());
        assertEquals(0, result.sourceTotal());
        assertEquals(0.0, result.percentAny(), 0.0);
        assertEquals(0.0, result.percentNone(), 0.0);
    }

    @Test
    public void combinationKeysFollowChannelOrderNotHitOrder() {
        MultiTargetResult result = MultiTargetSummary.run(threeChannels()).get(0);

        boolean found = false;
        for (CombinationPattern pattern : result.patterns()) {
            if (pattern.count() == 2) {
                assertEquals("B + C", pattern.key());
                found = true;
            }
        }
        assertTrue("no two-target pattern found", found);
    }

    @Test
    public void everyChannelGetsATurnAsTheSource() {
        List<MultiTargetResult> results = MultiTargetSummary.run(threeChannels());

        assertEquals(3, results.size());
        assertEquals("src", results.get(0).sourceName());
        assertEquals("B", results.get(1).sourceName());
        assertEquals("C", results.get(2).sourceName());
        assertEquals(Arrays.asList("src", "C"), results.get(1).targetNames());
    }

    @Test
    public void perObjectPartnersAreReportedForEveryTarget() {
        MultiTargetResult result = MultiTargetSummary.run(threeChannels()).get(0);

        assertEquals(Integer.valueOf(5), result.partnersFor(0).get("B"));
        assertEquals(Integer.valueOf(7), result.partnersFor(0).get("C"));
        assertEquals(2, result.targetsHit(0));
        assertEquals(Integer.valueOf(0), result.partnersFor(3).get("B"));
        assertEquals(0, result.targetsHit(3));
    }

    private static int patternCount(MultiTargetResult result, String key) {
        for (CombinationPattern pattern : result.patterns()) {
            if (pattern.key().equals(key)) return pattern.count();
        }
        return 0;
    }

    private static int patternRows(MultiTargetResult result, String key) {
        int rows = 0;
        for (CombinationPattern pattern : result.patterns()) {
            if (pattern.key().equals(key)) rows++;
        }
        return rows;
    }
}

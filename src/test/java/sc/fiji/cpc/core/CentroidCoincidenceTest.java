package sc.fiji.cpc.core;

import ij.ImagePlus;
import org.junit.Test;

import java.util.Arrays;
import java.util.List;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

public class CentroidCoincidenceTest {

    @Test
    public void readsTheLabelUnderAPoint() {
        ImagePlus target = Volumes.labels("target", new int[][][]{
                {{0, 0, 0},
                 {0, 7, 0},
                 {0, 0, 0}}});

        assertEquals(7, CentroidCoincidence.labelAt(target, 1, 1, 0));
        assertEquals(CentroidCoincidence.NO_PARTNER,
                CentroidCoincidence.labelAt(target, 0, 0, 0));
    }

    /**
     * 2.7 is nearer voxel 3 than voxel 2. Truncating would bias every lookup
     * towards the origin by up to half a voxel.
     */
    @Test
    public void roundsToTheNearestVoxelRatherThanTruncating() {
        ImagePlus target = Volumes.labels("target", new int[][][]{
                {{0, 0, 0, 4}}});

        assertEquals(4, CentroidCoincidence.labelAt(target, 2.7, 0, 0));
        assertEquals(CentroidCoincidence.NO_PARTNER,
                CentroidCoincidence.labelAt(target, 2.4, 0, 0));
    }

    @Test
    public void pointsOutsideTheStackHaveNoPartner() {
        ImagePlus target = Volumes.labels("target", new int[][][]{
                {{5, 5},
                 {5, 5}}});

        assertEquals(CentroidCoincidence.NO_PARTNER,
                CentroidCoincidence.labelAt(target, -1, 0, 0));
        assertEquals(CentroidCoincidence.NO_PARTNER,
                CentroidCoincidence.labelAt(target, 0, -1, 0));
        assertEquals(CentroidCoincidence.NO_PARTNER,
                CentroidCoincidence.labelAt(target, 0, 0, 1));
        assertEquals(CentroidCoincidence.NO_PARTNER,
                CentroidCoincidence.labelAt(target, 2, 0, 0));
    }

    /** Math.round would turn NaN into 0 and sample the corner of the stack. */
    @Test
    public void nonFiniteCoordinatesHaveNoPartner() {
        ImagePlus target = Volumes.labels("target", new int[][][]{{{5}}});

        assertEquals(CentroidCoincidence.NO_PARTNER,
                CentroidCoincidence.labelAt(target, Double.NaN, 0, 0));
        assertEquals(CentroidCoincidence.NO_PARTNER,
                CentroidCoincidence.labelAt(target, 0, Double.POSITIVE_INFINITY, 0));
    }

    @Test
    public void testsEveryObjectInOneDirection() {
        Channel source = Channel.of("A", Volumes.labels("A", new int[][][]{
                {{1, 1, 0, 0, 2, 2},
                 {1, 1, 0, 0, 2, 2}}}));
        Channel target = Channel.of("B", Volumes.labels("B", new int[][][]{
                {{9, 9, 9, 0, 0, 0},
                 {9, 9, 9, 0, 0, 0}}}));

        DirectionResult result = CentroidCoincidence.test(source, target);

        assertEquals("A", result.sourceName());
        assertEquals("B", result.targetName());
        assertEquals(2, result.totalObjects());
        assertEquals(1, result.coincidentCount());
        assertEquals(9, result.partnerFor(1));
        assertEquals(CentroidCoincidence.NO_PARTNER, result.partnerFor(2));
        assertEquals(50.0, result.percentCoincident(), 0.0);
    }

    /**
     * A C-shaped object's own centroid falls in background. The test is about
     * position, not overlap, and this is the case that proves it.
     */
    @Test
    public void aCentroidCanFallOutsideItsOwnObject() {
        int[][][] cShape = {
                {{0, 0, 0, 0, 0},
                 {0, 1, 1, 1, 0},
                 {0, 1, 0, 0, 0},
                 {0, 1, 1, 1, 0},
                 {0, 0, 0, 0, 0}}};
        Channel source = Channel.of("C", Volumes.labels("C", cShape));
        Channel selfTarget = Channel.of("Cagain", Volumes.labels("Cagain", cShape));

        DirectionResult result = CentroidCoincidence.test(source, selfTarget);

        assertEquals(CentroidCoincidence.NO_PARTNER, result.partnerFor(1));
        assertFalse(result.objects().get(0).isCoincident());
    }

    @Test
    public void reportsWhichSourceObjectsLandedInsideEachTarget() {
        Channel source = Channel.of("puncta", Volumes.labels("puncta", new int[][][]{
                {{1, 0, 2, 0, 3},
                 {0, 0, 0, 0, 0}}}));
        Channel target = Channel.of("cell", Volumes.labels("cell", new int[][][]{
                {{8, 8, 8, 0, 0},
                 {8, 8, 8, 0, 0}}}));

        DirectionResult result = CentroidCoincidence.test(source, target);

        assertEquals(Arrays.asList(Integer.valueOf(1), Integer.valueOf(2)),
                result.containedIn(8));
        assertTrue(result.containedIn(99).isEmpty());
        assertEquals(1, result.containingTargetCount());
    }

    @Test
    public void emptyChannelsProduceEmptyResultsRatherThanFailing() {
        Channel empty = Channel.of("empty", Volumes.labels("empty", new int[][][]{{{0, 0}}}));
        Channel target = Channel.of("target", Volumes.labels("target", new int[][][]{{{4, 4}}}));

        DirectionResult forward = CentroidCoincidence.test(empty, target);
        DirectionResult reverse = CentroidCoincidence.test(target, empty);

        assertEquals(0, forward.totalObjects());
        assertEquals(0.0, forward.percentCoincident(), 0.0);
        assertEquals(0.0, reverse.percentOfTarget(), 0.0);
        assertEquals(0, reverse.coincidentCount());
    }

    @Test
    public void channelsWithoutAnImageAreRejectedByName() {
        try {
            Channel.of("mine", (ImagePlus) null);
            fail("Expected a null label image to be rejected.");
        } catch (IllegalArgumentException expected) {
            assertTrue(expected.getMessage(), expected.getMessage().contains("'mine'"));
        }
    }

    @Test
    public void unnamedChannelsFallBackToTheImageTitle() {
        Channel channel = Channel.of(null, Volumes.labels("Cells.tif", new int[][][]{{{1}}}));
        assertEquals("Cells.tif", channel.name());
    }

    @Test
    public void pairwiseRunVisitsPairsInIndexOrderForwardBeforeReverse() {
        List<Channel> channels = Arrays.asList(
                Channel.of("A", Volumes.labels("A", new int[][][]{{{1, 0, 0}}})),
                Channel.of("B", Volumes.labels("B", new int[][][]{{{0, 2, 0}}})),
                Channel.of("C", Volumes.labels("C", new int[][][]{{{0, 0, 3}}})));

        CoincidenceResult result = PairwiseCoincidenceRunner.run(channels, true);

        assertEquals(6, result.comparisonCount());
        assertEquals("A", result.directions().get(0).sourceName());
        assertEquals("B", result.directions().get(0).targetName());
        assertEquals("B", result.directions().get(1).sourceName());
        assertEquals("A", result.directions().get(1).targetName());
        assertEquals("C", result.directions().get(2).targetName());
    }

    @Test
    public void unidirectionalRunOmitsTheReverseDirections() {
        List<Channel> channels = Arrays.asList(
                Channel.of("A", Volumes.labels("A", new int[][][]{{{1, 0}}})),
                Channel.of("B", Volumes.labels("B", new int[][][]{{{0, 2}}})));

        CoincidenceResult result = PairwiseCoincidenceRunner.run(channels, false);

        assertEquals(1, result.comparisonCount());
        assertEquals(null, result.direction("B", "A"));
        assertTrue(result.direction("A", "B") != null);
    }

    @Test
    public void duplicateChannelNamesAreRejected() {
        List<Channel> channels = Arrays.asList(
                Channel.of("same", Volumes.labels("x", new int[][][]{{{1}}})),
                Channel.of("same", Volumes.labels("y", new int[][][]{{{1}}})));
        try {
            PairwiseCoincidenceRunner.run(channels, true);
            fail("Expected duplicate channel names to be rejected.");
        } catch (IllegalArgumentException expected) {
            assertTrue(expected.getMessage(),
                    expected.getMessage().contains("have the same name"));
        }
    }

    @Test
    public void fewerThanTwoChannelsIsRejected() {
        try {
            PairwiseCoincidenceRunner.run(
                    Arrays.asList(Channel.of("only", Volumes.labels("x", new int[][][]{{{1}}}))),
                    true);
            fail("Expected a single channel to be rejected.");
        } catch (IllegalArgumentException expected) {
            assertTrue(expected.getMessage(),
                    expected.getMessage().contains("At least 2 channels"));
        }
    }

    /**
     * A pair whose names collide with a separator character. The lookup must
     * not confuse them, which a concatenated key would.
     */
    @Test
    public void channelNamesContainingArrowsDoNotCollide() {
        List<Channel> channels = Arrays.asList(
                Channel.of("A→B", Volumes.labels("x", new int[][][]{{{1, 0}}})),
                Channel.of("C", Volumes.labels("y", new int[][][]{{{0, 2}}})));

        CoincidenceResult result = PairwiseCoincidenceRunner.run(channels, true);

        assertTrue(result.direction("A→B", "C") != null);
        assertTrue(result.direction("C", "A→B") != null);
        assertEquals("A→B", result.direction("A→B", "C").sourceName());
    }
}

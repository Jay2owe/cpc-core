package sc.fiji.cpc.core;

import ij.ImagePlus;
import org.junit.Test;

import java.util.Arrays;
import java.util.List;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotSame;

public class CentroidMapBuilderTest {

    @Test
    public void drawsACrossAtEachPartnerCentroidCarryingItsLabel() {
        Channel base = Channel.of("base", Volumes.labels("base", new int[][][]{
                {{0, 0, 0, 0, 0},
                 {0, 0, 0, 0, 0},
                 {0, 0, 0, 0, 0},
                 {0, 0, 0, 0, 0},
                 {0, 0, 0, 0, 0}}}));
        Channel other = Channel.of("other", Volumes.labels("other", new int[][][]{
                {{0, 0, 0, 0, 0},
                 {0, 0, 0, 0, 0},
                 {0, 0, 6, 0, 0},
                 {0, 0, 0, 0, 0},
                 {0, 0, 0, 0, 0}}}));

        ImagePlus map = CentroidMapBuilder.build(base, Arrays.asList(base, other), "map");

        // Horizontal and vertical arms, radius 2, all carrying label 6.
        for (int offset = -2; offset <= 2; offset++) {
            assertEquals(6, map.getStack().getProcessor(1).getPixel(2 + offset, 2));
            assertEquals(6, map.getStack().getProcessor(1).getPixel(2, 2 + offset));
        }
        assertEquals(0, map.getStack().getProcessor(1).getPixel(0, 0));
    }

    @Test
    public void leavesTheCallersImageUntouched() {
        ImagePlus original = Volumes.labels("base", new int[][][]{
                {{0, 0, 0},
                 {0, 0, 0},
                 {0, 0, 0}}});
        Channel base = Channel.of("base", original);
        Channel other = Channel.of("other", Volumes.labels("other", new int[][][]{
                {{0, 0, 0},
                 {0, 3, 0},
                 {0, 0, 0}}}));

        ImagePlus map = CentroidMapBuilder.build(base, Arrays.asList(base, other), "map");

        assertNotSame(original, map);
        assertEquals(0, original.getStack().getProcessor(1).getPixel(1, 1));
        assertEquals(3, map.getStack().getProcessor(1).getPixel(1, 1));
    }

    /** An arm running past the edge is clipped; the rest of the cross stays. */
    @Test
    public void markersNearTheEdgeAreClippedNotDropped() {
        Channel base = Channel.of("base", Volumes.labels("base", new int[][][]{
                {{0, 0, 0},
                 {0, 0, 0},
                 {0, 0, 0}}}));
        Channel other = Channel.of("other", Volumes.labels("other", new int[][][]{
                {{5, 0, 0},
                 {0, 0, 0},
                 {0, 0, 0}}}));

        ImagePlus map = CentroidMapBuilder.build(base, Arrays.asList(base, other), "map");

        assertEquals(5, map.getStack().getProcessor(1).getPixel(0, 0));
        assertEquals(5, map.getStack().getProcessor(1).getPixel(1, 0));
        assertEquals(5, map.getStack().getProcessor(1).getPixel(0, 1));
        assertEquals(0, map.getStack().getProcessor(1).getPixel(1, 1));
    }

    @Test
    public void centroidsOnMissingSlicesAreSkipped() {
        Channel base = Channel.of("base", Volumes.labels("base", new int[][][]{
                {{0, 0, 0}}}));
        Channel other = Channel.of("other", Volumes.labels("other", new int[][][]{
                {{0, 0, 0}},
                {{0, 7, 0}}}));

        ImagePlus map = CentroidMapBuilder.build(base, Arrays.asList(base, other), "map");

        assertEquals(1, map.getStack().getSize());
        assertEquals(0, map.getStack().getProcessor(1).getPixel(1, 0));
    }

    @Test
    public void laterChannelsOverwriteEarlierOnesWhereMarkersMeet() {
        Channel base = Channel.of("base", Volumes.labels("base", new int[][][]{
                {{0, 0, 0},
                 {0, 0, 0},
                 {0, 0, 0}}}));
        int[][][] centred = {
                {{0, 0, 0},
                 {0, 4, 0},
                 {0, 0, 0}}};
        Channel first = Channel.of("first", Volumes.labels("first", centred));
        Channel second = Channel.of("second", Volumes.volume("second", new int[][][]{
                {{0, 0, 0},
                 {0, 8, 0},
                 {0, 0, 0}}}, 16));

        List<Channel> channels = Arrays.asList(base, first, second);
        ImagePlus map = CentroidMapBuilder.build(base, channels, "map");

        assertEquals(8, map.getStack().getProcessor(1).getPixel(1, 1));
    }

    @Test
    public void noCentroidsGivesAPlainCopy() {
        ImagePlus labels = Volumes.labels("base", new int[][][]{{{1, 1}, {1, 1}}});
        ImagePlus map = CentroidMapBuilder.draw(labels, null, "copy", 2);

        assertEquals("copy", map.getTitle());
        assertEquals(1, map.getStack().getProcessor(1).getPixel(0, 0));
    }
}

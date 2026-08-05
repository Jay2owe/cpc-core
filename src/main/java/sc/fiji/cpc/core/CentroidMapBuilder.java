package sc.fiji.cpc.core;

import ij.ImagePlus;
import ij.ImageStack;
import ij.process.ImageProcessor;

import java.util.ArrayList;
import java.util.List;

import sc.fiji.oc3d.core.measure.CentroidScan;

/**
 * Draws other channels' centroids onto a copy of a label image.
 *
 * <p>The visual check that the numbers are about the right objects. A table
 * saying 62% coincidence is unfalsifiable on its own; a map with a cross
 * sitting a few voxels outside every cell tells you the channels are
 * misregistered, and no summary statistic would have.
 *
 * <p>Markers carry the <em>source</em> object's label as their pixel value, so
 * a cross can be traced back to the row that produced it rather than being an
 * anonymous dot. They are drawn into a duplicate; the caller's image is not
 * touched.
 */
public final class CentroidMapBuilder {

    /** Arm length either side of centre, in voxels. */
    public static final int DEFAULT_MARKER_RADIUS = 2;

    private CentroidMapBuilder() {
        // Utility class.
    }

    /**
     * A copy of {@code base}'s label image with every other channel's centroids
     * drawn on.
     *
     * @param base     the channel to draw onto
     * @param channels every channel; {@code base} is skipped
     * @param title    title for the copy
     */
    public static ImagePlus build(Channel base, List<Channel> channels, String title) {
        List<CentroidScan.Centroid> markers = new ArrayList<CentroidScan.Centroid>();
        for (Channel channel : channels) {
            if (channel == base) continue;
            markers.addAll(channel.centroids().centroids());
        }
        return draw(base.labelImage(), markers, title, DEFAULT_MARKER_RADIUS);
    }

    /**
     * A copy of {@code labelImage} with a cross at each centroid.
     *
     * @param radius arm length either side of centre; 0 draws a single voxel
     */
    public static ImagePlus draw(ImagePlus labelImage, List<CentroidScan.Centroid> centroids,
                                 String title, int radius) {
        if (labelImage == null) {
            throw new IllegalArgumentException("labelImage must not be null (labelImage=null).");
        }
        if (radius < 0) {
            throw new IllegalArgumentException("radius must not be negative (radius="
                    + radius + ").");
        }
        ImagePlus map = labelImage.duplicate();
        if (title != null) map.setTitle(title);
        if (centroids == null || centroids.isEmpty()) return map;

        ImageStack stack = map.getStack();
        int width = map.getWidth();
        int height = map.getHeight();
        int depth = stack.getSize();

        for (CentroidScan.Centroid centroid : centroids) {
            if (isNotFinite(centroid.x()) || isNotFinite(centroid.y())
                    || isNotFinite(centroid.z())) {
                continue;
            }
            long column = Math.round(centroid.x());
            long row = Math.round(centroid.y());
            long slice = Math.round(centroid.z());
            // A centroid on a slice that does not exist has nowhere to be
            // drawn. Off-slice is skipped; off-edge is clipped per arm, so a
            // marker near the border still shows the part that fits.
            if (slice < 0 || slice >= depth) continue;

            ImageProcessor processor = stack.getProcessor((int) slice + 1);
            for (int offset = -radius; offset <= radius; offset++) {
                long armColumn = column + offset;
                long armRow = row + offset;
                if (armColumn >= 0 && armColumn < width && row >= 0 && row < height) {
                    processor.setf((int) armColumn, (int) row, centroid.label());
                }
                if (armRow >= 0 && armRow < height && column >= 0 && column < width) {
                    processor.setf((int) column, (int) armRow, centroid.label());
                }
            }
        }
        return map;
    }

    private static boolean isNotFinite(double value) {
        return Double.isNaN(value) || Double.isInfinite(value);
    }
}

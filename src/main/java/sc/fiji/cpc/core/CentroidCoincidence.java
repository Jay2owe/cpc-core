package sc.fiji.cpc.core;

import ij.ImagePlus;
import ij.ImageStack;

import java.util.ArrayList;
import java.util.List;

import sc.fiji.oc3d.core.label.LabelImages;
import sc.fiji.oc3d.core.measure.CentroidScan;

/**
 * The test: is this point inside an object in that channel?
 *
 * <p>Deliberately a point lookup and not an overlap measure. A centroid can sit
 * in background inside its own object - a C-shape, a ring, a horseshoe of
 * neuropil - and the answer is still "not inside", because the question being
 * asked is about the object's <em>position</em>, not its volume. Volumetric
 * overlap is a different question with a different answer, and it lives in
 * {@code volcoloc-core}.
 *
 * <p>Two rules matter and neither is arbitrary:
 *
 * <ul>
 *   <li><b>Round, do not truncate.</b> A centroid at x = 2.7 is nearer voxel 3
 *       than voxel 2. Truncating biases every lookup towards the origin by up to
 *       half a voxel, which for small objects is the difference between a hit
 *       and a miss.
 *   <li><b>Outside is not a partner.</b> A centroid that rounds outside the
 *       target stack has no partner rather than being clamped to the edge. A
 *       clamped lookup would silently pair an object with whatever happens to
 *       lie on the boundary.
 * </ul>
 */
public final class CentroidCoincidence {

    /** Returned when a point has no partner: background, or outside the stack. */
    public static final int NO_PARTNER = 0;

    private CentroidCoincidence() {
        // Utility class.
    }

    /**
     * The label at a point, or {@link #NO_PARTNER}.
     *
     * @param labelImage the channel being tested against
     * @param x          column, rounded to the nearest voxel
     * @param y          row, rounded to the nearest voxel
     * @param z          zero-based slice, rounded to the nearest voxel
     */
    public static int labelAt(ImagePlus labelImage, double x, double y, double z) {
        if (labelImage == null) {
            throw new IllegalArgumentException("labelImage must not be null (labelImage=null).");
        }
        // A non-finite coordinate cannot be rounded to a voxel. Math.round would
        // turn NaN into 0 and quietly sample the corner of the stack.
        if (isNotFinite(x) || isNotFinite(y) || isNotFinite(z)) return NO_PARTNER;

        ImageStack stack = labelImage.getStack();
        int width = labelImage.getWidth();
        int height = labelImage.getHeight();
        int depth = stack.getSize();

        long column = Math.round(x);
        long row = Math.round(y);
        long slice = Math.round(z);
        if (column < 0 || column >= width) return NO_PARTNER;
        if (row < 0 || row >= height) return NO_PARTNER;
        if (slice < 0 || slice >= depth) return NO_PARTNER;

        return LabelImages.labelFromPixel(
                stack.getProcessor((int) slice + 1).getf((int) column, (int) row));
    }

    /**
     * Tests every object in {@code source} against {@code target}.
     *
     * <p>One direction only. {@code A} in {@code B} and {@code B} in {@code A}
     * are different questions with different answers - an object can contain
     * three centroids while its own centroid lands nowhere - so the reverse
     * direction is a second call, not a flag.
     */
    public static DirectionResult test(Channel source, Channel target) {
        if (source == null || target == null) {
            throw new IllegalArgumentException("Both channels are required (source="
                    + source + ", target=" + target + ").");
        }
        List<CentroidScan.Centroid> centroids = source.centroids().centroids();
        List<CoincidenceObject> objects = new ArrayList<CoincidenceObject>(centroids.size());
        for (CentroidScan.Centroid centroid : centroids) {
            objects.add(new CoincidenceObject(centroid,
                    labelAt(target.labelImage(), centroid.x(), centroid.y(), centroid.z())));
        }
        return new DirectionResult(source.name(), target.name(), objects, target.objectCount());
    }

    private static boolean isNotFinite(double value) {
        return Double.isNaN(value) || Double.isInfinite(value);
    }
}

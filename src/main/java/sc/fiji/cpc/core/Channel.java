package sc.fiji.cpc.core;

import ij.ImagePlus;

import sc.fiji.oc3d.core.measure.CentroidScan;
import sc.fiji.oc3d.core.measure.LabelFeatureAccumulator;

/**
 * One channel taking part in a coincidence test: a name, a label image, and the
 * positions of the objects in it.
 *
 * <p>The centroids are held rather than recomputed. That is what lets a plugin
 * which has already measured its objects hand the same scan to this engine and
 * get answers that line up with its own table row for row - see
 * {@link #of(String, ImagePlus, CentroidScan.Result)} and
 * {@link #measured(String, ImagePlus, LabelFeatureAccumulator.Result, boolean)}.
 * A plugin that recomputed here could disagree with itself about how many
 * objects there are, and the disagreement would show up as a table with the
 * wrong number of rows rather than as an error.
 *
 * <p>The label image is kept because it is the <em>target</em> side of the test:
 * a coincidence lookup reads the voxel under another channel's centroid, so the
 * pixels are still needed even though the positions are precomputed.
 */
public final class Channel {

    private final String name;
    private final ImagePlus labelImage;
    private final CentroidScan.Result centroids;

    private Channel(String name, ImagePlus labelImage, CentroidScan.Result centroids) {
        this.name = name;
        this.labelImage = labelImage;
        this.centroids = centroids;
    }

    /** Scans {@code labelImage} for geometric centroids. */
    public static Channel of(String name, ImagePlus labelImage) {
        return of(name, labelImage, (ImagePlus) null);
    }

    /**
     * Scans {@code labelImage}, weighting positions by {@code intensityImage}
     * when one is given.
     *
     * @param intensityImage null for geometric centroids
     */
    public static Channel of(String name, ImagePlus labelImage, ImagePlus intensityImage) {
        requireImage(name, labelImage);
        return new Channel(nameOf(name, labelImage), labelImage,
                CentroidScan.scan(labelImage, intensityImage));
    }

    /**
     * Uses centroids the caller already has.
     *
     * <p>No pixels are read for the source side, so this is the cheap path and
     * the one that guarantees agreement with whatever produced the scan.
     */
    public static Channel of(String name, ImagePlus labelImage, CentroidScan.Result centroids) {
        requireImage(name, labelImage);
        if (centroids == null) {
            throw new IllegalArgumentException("centroids must not be null for channel '"
                    + nameOf(name, labelImage) + "'.");
        }
        return new Channel(nameOf(name, labelImage), labelImage, centroids);
    }

    /**
     * Reuses a completed measurement pass.
     *
     * <p>The intended entry point for a measurement plugin adding coincidence
     * columns to a table it has already built.
     *
     * @param intensityWeighted use the centre of mass rather than the centroid
     */
    public static Channel measured(String name, ImagePlus labelImage,
                                   LabelFeatureAccumulator.Result measurement,
                                   boolean intensityWeighted) {
        requireImage(name, labelImage);
        if (measurement == null) {
            throw new IllegalArgumentException("measurement must not be null for channel '"
                    + nameOf(name, labelImage) + "'.");
        }
        return new Channel(nameOf(name, labelImage), labelImage,
                CentroidScan.from(measurement, intensityWeighted));
    }

    public String name() {
        return name;
    }

    public ImagePlus labelImage() {
        return labelImage;
    }

    public CentroidScan.Result centroids() {
        return centroids;
    }

    public int objectCount() {
        return centroids.objectCount();
    }

    private static void requireImage(String name, ImagePlus labelImage) {
        if (labelImage == null) {
            throw new IllegalArgumentException("Label image for channel '"
                    + (name == null ? "(unnamed)" : name) + "' must not be null.");
        }
        if (labelImage.getStack() == null) {
            throw new IllegalArgumentException("Label image for channel '"
                    + nameOf(name, labelImage) + "' has no stack.");
        }
    }

    /** Falls back to the image's own title, which is what a user recognises. */
    private static String nameOf(String name, ImagePlus labelImage) {
        if (name != null && name.trim().length() > 0) return name;
        String title = labelImage.getTitle();
        return title == null || title.trim().length() == 0 ? "(unnamed)" : title;
    }

    @Override
    public String toString() {
        return "Channel[" + name + ", " + objectCount() + " objects]";
    }
}

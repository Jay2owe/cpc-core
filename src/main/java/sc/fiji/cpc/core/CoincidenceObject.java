package sc.fiji.cpc.core;

import sc.fiji.oc3d.core.measure.CentroidScan;

/**
 * One object, and what it was found to coincide with in one direction.
 *
 * <p>Immutable, unlike the mutable carrier this replaces. The old class was
 * reused across pairwise comparisons and had to be deep-copied before each one
 * so that the previous partner did not leak into the next result; forgetting
 * the copy produced results that depended on comparison order. There is nothing
 * to forget here.
 */
public final class CoincidenceObject {

    private final int label;
    private final double x;
    private final double y;
    private final double z;
    private final long voxelCount;
    private final int partnerLabel;

    CoincidenceObject(CentroidScan.Centroid centroid, int partnerLabel) {
        this(centroid.label(), centroid.x(), centroid.y(), centroid.z(),
                centroid.voxelCount(), partnerLabel);
    }

    CoincidenceObject(int label, double x, double y, double z,
                      long voxelCount, int partnerLabel) {
        this.label = label;
        this.x = x;
        this.y = y;
        this.z = z;
        this.voxelCount = voxelCount;
        this.partnerLabel = partnerLabel;
    }

    public int label() {
        return label;
    }

    /** Centroid column, in pixels. */
    public double x() {
        return x;
    }

    /** Centroid row, in pixels. */
    public double y() {
        return y;
    }

    /** Centroid slice, zero-based. */
    public double z() {
        return z;
    }

    public long voxelCount() {
        return voxelCount;
    }

    /**
     * The label this object's centroid landed in, or
     * {@link CentroidCoincidence#NO_PARTNER}.
     */
    public int partnerLabel() {
        return partnerLabel;
    }

    public boolean isCoincident() {
        return partnerLabel > CentroidCoincidence.NO_PARTNER;
    }

    @Override
    public String toString() {
        return "CoincidenceObject[label=" + label + " partner=" + partnerLabel + "]";
    }
}

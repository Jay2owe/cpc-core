package sc.fiji.cpc.core;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * The outcome of testing one channel's centroids against another.
 *
 * <p>Direction is part of the identity, not a detail. "62% of puncta sit inside
 * a cell" and "62% of cells contain a punctum" are different claims, and a
 * result that did not carry which one it was would let a caller print the wrong
 * sentence under the right number.
 *
 * <p>No {@code ResultsTable} appears anywhere in here. The consumer decides how
 * to present this - CPC opens a window per pair, a measurement plugin appends
 * two columns to a table it already has - and a core that built the table would
 * be usable by exactly one of them.
 */
public final class DirectionResult {

    private final String sourceName;
    private final String targetName;
    private final List<CoincidenceObject> objects;
    private final Map<Integer, CoincidenceObject> byLabel;
    private final Map<Integer, List<Integer>> containedByTargetLabel;
    private final int targetObjectCount;
    private final int coincidentCount;

    DirectionResult(String sourceName, String targetName,
                    List<CoincidenceObject> objects, int targetObjectCount) {
        this.sourceName = sourceName;
        this.targetName = targetName;
        this.objects = Collections.unmodifiableList(new ArrayList<CoincidenceObject>(objects));
        this.targetObjectCount = targetObjectCount;

        Map<Integer, CoincidenceObject> index = new LinkedHashMap<Integer, CoincidenceObject>();
        Map<Integer, List<Integer>> contained = new LinkedHashMap<Integer, List<Integer>>();
        int coincident = 0;
        for (CoincidenceObject object : this.objects) {
            index.put(Integer.valueOf(object.label()), object);
            if (!object.isCoincident()) continue;
            coincident++;
            Integer partner = Integer.valueOf(object.partnerLabel());
            List<Integer> sources = contained.get(partner);
            if (sources == null) {
                sources = new ArrayList<Integer>();
                contained.put(partner, sources);
            }
            sources.add(Integer.valueOf(object.label()));
        }
        this.byLabel = Collections.unmodifiableMap(index);
        this.containedByTargetLabel = Collections.unmodifiableMap(contained);
        this.coincidentCount = coincident;
    }

    public String sourceName() {
        return sourceName;
    }

    public String targetName() {
        return targetName;
    }

    /** Source objects in ascending label order. */
    public List<CoincidenceObject> objects() {
        return objects;
    }

    /** One source object by label, or null when the label is absent. */
    public CoincidenceObject forLabel(int label) {
        return byLabel.get(Integer.valueOf(label));
    }

    /**
     * The partner label for one source object, or
     * {@link CentroidCoincidence#NO_PARTNER} when it has none or is absent.
     * <p>
     * The lookup a plugin appending a column to its own table wants: it walks
     * its rows and asks about each label, rather than walking these objects.
     */
    public int partnerFor(int label) {
        CoincidenceObject object = forLabel(label);
        return object == null ? CentroidCoincidence.NO_PARTNER : object.partnerLabel();
    }

    /**
     * The inverse view: which source objects landed inside a given target
     * object, in ascending source-label order.
     *
     * <p>This is what makes "contains" a first-class answer rather than
     * something each consumer re-derives. An object can contain several
     * centroids, so this returns a list and not a flag.
     *
     * @return never null; empty when nothing landed inside
     */
    public List<Integer> containedIn(int targetLabel) {
        List<Integer> contained = containedByTargetLabel.get(Integer.valueOf(targetLabel));
        return contained == null ? Collections.<Integer>emptyList()
                : Collections.unmodifiableList(contained);
    }

    /** Target labels that contain at least one source centroid. */
    public int containingTargetCount() {
        return containedByTargetLabel.size();
    }

    public int totalObjects() {
        return objects.size();
    }

    public int targetObjectCount() {
        return targetObjectCount;
    }

    public int coincidentCount() {
        return coincidentCount;
    }

    /** Coincident objects as a percentage of this channel's objects; 0 when empty. */
    public double percentCoincident() {
        return objects.isEmpty() ? 0.0 : coincidentCount * 100.0 / objects.size();
    }

    /** Coincident objects as a percentage of the target channel's objects; 0 when empty. */
    public double percentOfTarget() {
        return targetObjectCount <= 0 ? 0.0 : coincidentCount * 100.0 / targetObjectCount;
    }

    @Override
    public String toString() {
        return "DirectionResult[" + sourceName + " -> " + targetName + ", "
                + coincidentCount + "/" + objects.size() + "]";
    }
}

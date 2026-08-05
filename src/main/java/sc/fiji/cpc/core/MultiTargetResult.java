package sc.fiji.cpc.core;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * One channel tested against every other, with the combinations counted.
 *
 * <p>Source-anchored: the rows are this channel's objects, and the question is
 * which of the other channels each one hits. That is a different shape from the
 * pairwise result, where each row belongs to one pair, and it is the shape that
 * answers "how many of my cells contain both markers rather than either".
 */
public final class MultiTargetResult {

    /** Pattern key for an object that hit nothing. */
    public static final String NONE = "None";

    /** Pattern key for the row totalling objects that hit at least one target. */
    public static final String ANY = "— Any —";

    /** Separator between target names in a combination key. */
    static final String JOIN = " + ";

    private final String sourceName;
    private final List<String> targetNames;
    private final List<CoincidenceObject> objects;
    private final List<Map<String, Integer>> partnersByObject;
    private final List<CombinationPattern> patterns;
    private final int anyCount;
    private final int noneCount;

    MultiTargetResult(String sourceName, List<String> targetNames,
                      List<CoincidenceObject> objects,
                      List<Map<String, Integer>> partnersByObject) {
        this.sourceName = sourceName;
        this.targetNames = Collections.unmodifiableList(new ArrayList<String>(targetNames));
        this.objects = Collections.unmodifiableList(
                new ArrayList<CoincidenceObject>(objects));

        List<Map<String, Integer>> partners =
                new ArrayList<Map<String, Integer>>(partnersByObject.size());
        for (Map<String, Integer> perObject : partnersByObject) {
            partners.add(Collections.unmodifiableMap(
                    new LinkedHashMap<String, Integer>(perObject)));
        }
        this.partnersByObject = Collections.unmodifiableList(partners);

        Map<String, Integer> counts = new LinkedHashMap<String, Integer>();
        int any = 0;
        for (int i = 0; i < this.objects.size(); i++) {
            String key = patternKey(this.partnersByObject.get(i));
            if (!NONE.equals(key)) any++;
            Integer count = counts.get(key);
            counts.put(key, Integer.valueOf(count == null ? 1 : count.intValue() + 1));
        }
        this.anyCount = any;
        this.noneCount = counts.containsKey(NONE) ? counts.get(NONE).intValue() : 0;

        List<CombinationPattern> built = new ArrayList<CombinationPattern>(counts.size());
        for (Map.Entry<String, Integer> entry : counts.entrySet()) {
            built.add(new CombinationPattern(entry.getKey(), entry.getValue().intValue(),
                    percentOf(entry.getValue().intValue())));
        }
        this.patterns = Collections.unmodifiableList(built);
    }

    /** The combination key for one object: targets joined, or {@link #NONE}. */
    private String patternKey(Map<String, Integer> partners) {
        StringBuilder key = new StringBuilder();
        for (String target : targetNames) {
            Integer partner = partners.get(target);
            if (partner == null || partner.intValue() <= CentroidCoincidence.NO_PARTNER) continue;
            if (key.length() > 0) key.append(JOIN);
            key.append(target);
        }
        return key.length() == 0 ? NONE : key.toString();
    }

    public String sourceName() {
        return sourceName;
    }

    /** The other channels, in the order they were supplied. */
    public List<String> targetNames() {
        return targetNames;
    }

    /** This channel's objects, ascending by label. */
    public List<CoincidenceObject> objects() {
        return objects;
    }

    /**
     * Target name to partner label for one object, by index into
     * {@link #objects()}. A target the object missed maps to
     * {@link CentroidCoincidence#NO_PARTNER}.
     */
    public Map<String, Integer> partnersFor(int objectIndex) {
        return partnersByObject.get(objectIndex);
    }

    /** How many targets one object hit. */
    public int targetsHit(int objectIndex) {
        int hits = 0;
        for (Integer partner : partnersByObject.get(objectIndex).values()) {
            if (partner != null && partner.intValue() > CentroidCoincidence.NO_PARTNER) hits++;
        }
        return hits;
    }

    /**
     * Combination patterns, in the order they were first observed.
     * <p>
     * Only patterns that actually occurred appear here — a run where every
     * object hit something has no {@link #NONE} entry. Callers that need the
     * non-coincident count unconditionally should read {@link #noneCount()},
     * which is always defined, rather than searching this list for a row that
     * may not exist.
     */
    public List<CombinationPattern> patterns() {
        return patterns;
    }

    /** Objects hitting at least one target. Always defined. */
    public int anyCount() {
        return anyCount;
    }

    /** Objects hitting nothing. Always defined, including when it is zero. */
    public int noneCount() {
        return noneCount;
    }

    public int sourceTotal() {
        return objects.size();
    }

    public double percentAny() {
        return percentOf(anyCount);
    }

    public double percentNone() {
        return percentOf(noneCount);
    }

    private double percentOf(int count) {
        return objects.isEmpty() ? 0.0 : count * 100.0 / objects.size();
    }

    @Override
    public String toString() {
        return "MultiTargetResult[" + sourceName + " vs " + targetNames + ", "
                + objects.size() + " objects]";
    }
}

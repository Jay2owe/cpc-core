package sc.fiji.cpc.core;

/**
 * One combination of targets, and how many source objects matched exactly it.
 *
 * <p>Exactly, not at least. An object hitting B and C counts towards
 * {@code "B + C"} and not towards {@code "B"}, so the counts partition the
 * source objects and sum to the source total. That is what makes a combination
 * table readable as a breakdown rather than as overlapping tallies.
 */
public final class CombinationPattern {

    private final String key;
    private final int count;
    private final double percentOfSource;

    CombinationPattern(String key, int count, double percentOfSource) {
        this.key = key;
        this.count = count;
        this.percentOfSource = percentOfSource;
    }

    /**
     * Target names joined by {@code " + "} in channel order, or
     * {@link MultiTargetResult#NONE}.
     */
    public String key() {
        return key;
    }

    public int count() {
        return count;
    }

    /** Unrounded. Rounding for display is the consumer's decision. */
    public double percentOfSource() {
        return percentOfSource;
    }

    public boolean isNone() {
        return MultiTargetResult.NONE.equals(key);
    }

    @Override
    public String toString() {
        return "CombinationPattern[" + key + " = " + count + "]";
    }
}

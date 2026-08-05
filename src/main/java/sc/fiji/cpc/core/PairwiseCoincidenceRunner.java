package sc.fiji.cpc.core;

import java.util.ArrayList;
import java.util.List;

/**
 * Every pair of channels, in one or both directions.
 *
 * <p>Pairs are visited in index order - (1,2), (1,3), (2,3) - and within a pair
 * the forward direction is emitted before the reverse. That ordering is part of
 * the contract: it decides the row order of every summary a consumer builds
 * from this, and a set that came back in a different order each run would make
 * saved CSVs differ for no reason.
 */
public final class PairwiseCoincidenceRunner {

    /** Fewer channels than this and there is no pair to test. */
    public static final int MIN_CHANNELS = 2;

    private PairwiseCoincidenceRunner() {
        // Utility class.
    }

    /**
     * Runs every pairwise comparison.
     *
     * @param channels      two or more channels; no upper bound is imposed here
     * @param bidirectional also test each target against each source
     * @throws IllegalArgumentException if fewer than two channels are given, or
     *                                  a channel is null
     */
    public static CoincidenceResult run(List<Channel> channels, boolean bidirectional) {
        validate(channels);
        List<DirectionResult> directions = new ArrayList<DirectionResult>();
        for (int i = 0; i < channels.size(); i++) {
            for (int j = i + 1; j < channels.size(); j++) {
                directions.add(CentroidCoincidence.test(channels.get(i), channels.get(j)));
                if (bidirectional) {
                    directions.add(CentroidCoincidence.test(channels.get(j), channels.get(i)));
                }
            }
        }
        return new CoincidenceResult(channels, directions, bidirectional);
    }

    private static void validate(List<Channel> channels) {
        if (channels == null || channels.size() < MIN_CHANNELS) {
            throw new IllegalArgumentException("At least " + MIN_CHANNELS
                    + " channels are required (given="
                    + (channels == null ? "null" : String.valueOf(channels.size())) + ").");
        }
        for (int i = 0; i < channels.size(); i++) {
            if (channels.get(i) == null) {
                throw new IllegalArgumentException("Channel " + (i + 1) + " is null.");
            }
        }
        // Two channels sharing a name would collide in every lookup keyed by
        // name, and the collision would surface as a table with a missing pair
        // rather than as an error.
        for (int i = 0; i < channels.size(); i++) {
            for (int j = i + 1; j < channels.size(); j++) {
                if (channels.get(i).name().equals(channels.get(j).name())) {
                    throw new IllegalArgumentException("Channels " + (i + 1) + " and " + (j + 1)
                            + " have the same name ('" + channels.get(i).name() + "').");
                }
            }
        }
    }
}

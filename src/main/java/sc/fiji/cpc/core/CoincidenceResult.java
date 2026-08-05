package sc.fiji.cpc.core;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Everything a pairwise run produced.
 *
 * <p>A model, not a report. It holds the channels that took part and the
 * direction results in the order they were computed, and answers lookups by
 * name; it formats nothing and writes nothing. What a consumer does with it -
 * one window per pair, extra columns on an existing table, a CSV, nothing at
 * all - is the consumer's decision, and that is precisely what makes the same
 * engine usable by plugins whose tables look nothing alike.
 */
public final class CoincidenceResult {

    private final List<Channel> channels;
    private final List<DirectionResult> directions;
    private final Map<Pair, DirectionResult> byPair;
    private final boolean bidirectional;

    CoincidenceResult(List<Channel> channels, List<DirectionResult> directions,
                      boolean bidirectional) {
        this.channels = Collections.unmodifiableList(new ArrayList<Channel>(channels));
        this.directions = Collections.unmodifiableList(
                new ArrayList<DirectionResult>(directions));
        this.bidirectional = bidirectional;

        Map<Pair, DirectionResult> index = new LinkedHashMap<Pair, DirectionResult>();
        for (DirectionResult direction : this.directions) {
            index.put(new Pair(direction.sourceName(), direction.targetName()), direction);
        }
        this.byPair = Collections.unmodifiableMap(index);
    }

    public List<Channel> channels() {
        return channels;
    }

    /** Pairs in index order; within a pair, forward before reverse. */
    public List<DirectionResult> directions() {
        return directions;
    }

    /**
     * One direction by channel name, or null when it was not computed - which
     * is the normal case for every reverse direction of a unidirectional run.
     */
    public DirectionResult direction(String sourceName, String targetName) {
        return byPair.get(new Pair(sourceName, targetName));
    }

    public boolean isBidirectional() {
        return bidirectional;
    }

    public int channelCount() {
        return channels.size();
    }

    public int comparisonCount() {
        return directions.size();
    }

    /** A channel by name, or null. */
    public Channel channel(String name) {
        for (Channel channel : channels) {
            if (channel.name().equals(name)) return channel;
        }
        return null;
    }

    /**
     * A source/target pair as a key in its own right.
     * <p>
     * Not the concatenation the original used. Any separator is a character a
     * user can also put in an image title, and a title containing it would make
     * two different pairs collide - silently returning the wrong direction's
     * numbers rather than failing.
     */
    private static final class Pair {

        private final String source;
        private final String target;

        Pair(String source, String target) {
            this.source = source;
            this.target = target;
        }

        @Override
        public boolean equals(Object other) {
            if (this == other) return true;
            if (!(other instanceof Pair)) return false;
            Pair pair = (Pair) other;
            return equal(source, pair.source) && equal(target, pair.target);
        }

        @Override
        public int hashCode() {
            return 31 * (source == null ? 0 : source.hashCode())
                    + (target == null ? 0 : target.hashCode());
        }

        private static boolean equal(String left, String right) {
            return left == null ? right == null : left.equals(right);
        }
    }

    @Override
    public String toString() {
        return "CoincidenceResult[" + channels.size() + " channels, "
                + directions.size() + " comparisons]";
    }
}

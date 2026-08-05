package sc.fiji.cpc.core;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import sc.fiji.oc3d.core.measure.CentroidScan;

/**
 * Each channel against all the others at once.
 *
 * <p>The pairwise run answers "how many of A sit inside B". This answers "how
 * many of A sit inside B <em>and</em> C, versus B only, versus neither" - the
 * question that actually gets asked of a three-marker experiment, and one that
 * cannot be recovered from the pairwise results because those lose which
 * individual object hit which combination.
 */
public final class MultiTargetSummary {

    private MultiTargetSummary() {
        // Utility class.
    }

    /**
     * Runs every channel as a source against all the others.
     *
     * @param channels two or more channels
     * @return one result per channel, in channel order
     */
    public static List<MultiTargetResult> run(List<Channel> channels) {
        if (channels == null || channels.size() < PairwiseCoincidenceRunner.MIN_CHANNELS) {
            throw new IllegalArgumentException("At least "
                    + PairwiseCoincidenceRunner.MIN_CHANNELS + " channels are required (given="
                    + (channels == null ? "null" : String.valueOf(channels.size())) + ").");
        }

        List<MultiTargetResult> results = new ArrayList<MultiTargetResult>(channels.size());
        for (int source = 0; source < channels.size(); source++) {
            Channel sourceChannel = channels.get(source);
            List<CentroidScan.Centroid> centroids = sourceChannel.centroids().centroids();

            List<String> targetNames = new ArrayList<String>();
            List<Map<String, Integer>> partnersByObject =
                    new ArrayList<Map<String, Integer>>(centroids.size());
            for (int i = 0; i < centroids.size(); i++) {
                partnersByObject.add(new LinkedHashMap<String, Integer>());
            }

            for (int target = 0; target < channels.size(); target++) {
                if (target == source) continue;
                Channel targetChannel = channels.get(target);
                targetNames.add(targetChannel.name());
                for (int i = 0; i < centroids.size(); i++) {
                    CentroidScan.Centroid centroid = centroids.get(i);
                    partnersByObject.get(i).put(targetChannel.name(), Integer.valueOf(
                            CentroidCoincidence.labelAt(targetChannel.labelImage(),
                                    centroid.x(), centroid.y(), centroid.z())));
                }
            }

            List<CoincidenceObject> objects = new ArrayList<CoincidenceObject>(centroids.size());
            for (int i = 0; i < centroids.size(); i++) {
                // The per-object partner map is the real answer here; the
                // single partnerLabel is the first hit, kept so the object type
                // is the same one the pairwise results use.
                objects.add(new CoincidenceObject(centroids.get(i),
                        firstHit(targetNames, partnersByObject.get(i))));
            }

            results.add(new MultiTargetResult(sourceChannel.name(), targetNames,
                    objects, partnersByObject));
        }
        return results;
    }

    private static int firstHit(List<String> targetNames, Map<String, Integer> partners) {
        for (String target : targetNames) {
            Integer partner = partners.get(target);
            if (partner != null && partner.intValue() > CentroidCoincidence.NO_PARTNER) {
                return partner.intValue();
            }
        }
        return CentroidCoincidence.NO_PARTNER;
    }
}

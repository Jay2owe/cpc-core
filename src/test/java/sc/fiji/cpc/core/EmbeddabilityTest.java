package sc.fiji.cpc.core;

import ij.ImagePlus;
import org.junit.Test;

import java.io.File;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import sc.fiji.oc3d.core.measure.CentroidScan;
import sc.fiji.oc3d.core.measure.LabelFeatureAccumulator;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

/**
 * The properties that make this module embeddable, asserted rather than assumed.
 * <p>
 * Each of these is a rule from the {@code -core} pattern that is easy to break
 * by accident and invisible until a consumer tries to use the module - by which
 * point the fix is a release of every plugin that embeds it.
 */
public class EmbeddabilityTest {

    /**
     * A measurement plugin appends coincidence columns to a table it already
     * has, without rescanning and without the two halves disagreeing about how
     * many objects exist.
     */
    @Test
    public void aMeasurementPluginCanAppendColumnsToItsOwnTable() {
        ImagePlus mine = Volumes.labels("cells", new int[][][]{
                {{1, 1, 0, 2, 2},
                 {1, 1, 0, 2, 2}}});
        // Object 1 spans x,y in [0,1], so its centroid is (0.5, 0.5) and rounds
        // to voxel (1, 1). The punctum sits exactly there; object 2's centroid
        // rounds to (4, 1), which is background.
        ImagePlus theirs = Volumes.labels("puncta", new int[][][]{
                {{0, 0, 0, 0, 0},
                 {0, 9, 0, 0, 0}}});

        // What the plugin already did for its own table.
        LabelFeatureAccumulator.Result measured = LabelFeatureAccumulator.scan(mine, null);

        // What it adds to offer coincidence. No pixels of `mine` are re-read.
        Channel source = Channel.measured("cells", mine, measured, false);
        DirectionResult coincidence =
                CentroidCoincidence.test(source, Channel.of("puncta", theirs));

        // The rows line up with the measurement table, label for label.
        List<Integer> tableLabels = measured.labelsSorted();
        assertEquals(tableLabels.size(), coincidence.objects().size());
        for (int row = 0; row < tableLabels.size(); row++) {
            int label = tableLabels.get(row).intValue();
            assertEquals("row " + row, label, coincidence.objects().get(row).label());
            // The column the plugin appends.
            coincidence.partnerFor(label);
        }
        assertEquals(9, coincidence.partnerFor(1));
        assertEquals(CentroidCoincidence.NO_PARTNER, coincidence.partnerFor(2));
    }

    /** Precomputed centroids and a fresh scan must give the same answers. */
    @Test
    public void reusedAndFreshCentroidsAgree() {
        ImagePlus labels = Volumes.labels("a", new int[][][]{
                {{1, 1, 0, 2},
                 {1, 0, 0, 2}}});
        ImagePlus target = Volumes.labels("b", new int[][][]{
                {{4, 4, 4, 4},
                 {4, 4, 4, 4}}});

        DirectionResult fresh = CentroidCoincidence.test(
                Channel.of("a", labels), Channel.of("b", target));
        DirectionResult reused = CentroidCoincidence.test(
                Channel.of("a", labels, CentroidScan.scan(labels)), Channel.of("b", target));

        assertEquals(fresh.objects().size(), reused.objects().size());
        for (int i = 0; i < fresh.objects().size(); i++) {
            assertEquals(fresh.objects().get(i).label(), reused.objects().get(i).label());
            assertEquals(fresh.objects().get(i).partnerLabel(),
                    reused.objects().get(i).partnerLabel());
            assertEquals(fresh.objects().get(i).x(), reused.objects().get(i).x(), 0.0);
        }
    }

    /**
     * No Swing, no dialogs, no {@code IJ.error}, no {@code System.exit}.
     * <p>
     * Checked against the compiled classes rather than the source, because it
     * is the bytecode a consumer inherits. A core that touched Swing would
     * throw a {@code HeadlessException} on a cluster, and one that called
     * {@code IJ.error} would open a dialog nobody is there to dismiss.
     */
    @Test
    public void noClassReferencesUserInterfaceOrProcessControl() throws Exception {
        List<String> offenders = new ArrayList<String>();
        for (Class<?> type : engineClasses()) {
            String bytecode = constantPoolText(type);
            for (String banned : Arrays.asList(
                    "javax/swing", "java/awt/Dialog", "java/awt/Frame",
                    "ij/gui/GenericDialog", "ij/IJ.error", "ij/IJ.showMessage",
                    "java/lang/System.exit")) {
                if (bytecode.contains(banned)) {
                    offenders.add(type.getName() + " references " + banned);
                }
            }
        }
        assertTrue(offenders.toString(), offenders.isEmpty());
    }

    /** No {@code ResultsTable} anywhere: cores return models, not tables. */
    @Test
    public void noPublicMethodReturnsAnImageJTable() throws Exception {
        List<String> offenders = new ArrayList<String>();
        for (Class<?> type : engineClasses()) {
            for (Method method : type.getMethods()) {
                if ("ij.measure.ResultsTable".equals(method.getReturnType().getName())) {
                    offenders.add(type.getName() + "." + method.getName());
                }
                for (Class<?> parameter : method.getParameterTypes()) {
                    if ("ij.measure.ResultsTable".equals(parameter.getName())) {
                        offenders.add(type.getName() + "." + method.getName() + " parameter");
                    }
                }
            }
        }
        assertTrue(offenders.toString(), offenders.isEmpty());
    }

    /**
     * Nothing here probes a class by name. Shading rewrites bytecode references
     * but cannot rewrite a string, so a reflective lookup would survive
     * relocation as a name that no longer resolves.
     */
    @Test
    public void noClassUsesReflectionByName() throws Exception {
        List<String> offenders = new ArrayList<String>();
        for (Class<?> type : engineClasses()) {
            String bytecode = constantPoolText(type);
            for (String banned : Arrays.asList(
                    "java/lang/Class.forName", "loadClass", "getDeclaredMethod")) {
                if (bytecode.contains(banned)) {
                    offenders.add(type.getName() + " references " + banned);
                }
            }
        }
        assertTrue(offenders.toString(), offenders.isEmpty());
    }

    private static List<Class<?>> engineClasses() throws Exception {
        List<Class<?>> classes = new ArrayList<Class<?>>();
        for (String name : Arrays.asList(
                "CentroidCoincidence", "CentroidMapBuilder", "Channel",
                "CoincidenceObject", "CoincidenceResult", "CombinationPattern",
                "DirectionResult", "MultiTargetResult", "MultiTargetSummary",
                "PairwiseCoincidenceRunner")) {
            classes.add(Class.forName("sc.fiji.cpc.core." + name));
        }
        return classes;
    }

    /** The class file as Latin-1 text — enough to spot a referenced name. */
    private static String constantPoolText(Class<?> type) throws Exception {
        String resource = type.getName().replace('.', '/') + ".class";
        java.io.InputStream in = type.getClassLoader().getResourceAsStream(resource);
        assertTrue("class file not found: " + resource, in != null);
        try {
            java.io.ByteArrayOutputStream out = new java.io.ByteArrayOutputStream();
            byte[] buffer = new byte[8192];
            int read;
            while ((read = in.read(buffer)) > 0) out.write(buffer, 0, read);
            return new String(out.toByteArray(), "ISO-8859-1");
        } finally {
            in.close();
        }
    }

    /** The module must not reach the filesystem; consumers decide where output goes. */
    @Test
    public void noPublicMethodTakesOrReturnsAFile() throws Exception {
        List<String> offenders = new ArrayList<String>();
        for (Class<?> type : engineClasses()) {
            for (Method method : type.getMethods()) {
                if (File.class.equals(method.getReturnType())) {
                    offenders.add(type.getName() + "." + method.getName());
                }
                for (Class<?> parameter : method.getParameterTypes()) {
                    if (File.class.equals(parameter)) {
                        offenders.add(type.getName() + "." + method.getName() + " parameter");
                    }
                }
            }
        }
        assertTrue(offenders.toString(), offenders.isEmpty());
    }
}

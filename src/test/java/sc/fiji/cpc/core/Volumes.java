package sc.fiji.cpc.core;

import ij.ImagePlus;
import ij.ImageStack;
import ij.process.ByteProcessor;
import ij.process.FloatProcessor;
import ij.process.ImageProcessor;
import ij.process.ShortProcessor;

/** Small label and intensity volumes, built from literal arrays. */
final class Volumes {

    private Volumes() {
    }

    /** 16-bit label image from {@code [z][y][x]}. */
    static ImagePlus labels(String title, int[][][] values) {
        return volume(title, values, 16);
    }

    static ImagePlus volume(String title, int[][][] values, int bitDepth) {
        int depth = values.length;
        int height = values[0].length;
        int width = values[0][0].length;
        ImageStack stack = new ImageStack(width, height);
        for (int z = 0; z < depth; z++) {
            ImageProcessor processor = processor(bitDepth, width, height);
            for (int y = 0; y < height; y++) {
                for (int x = 0; x < width; x++) {
                    processor.setf(x, y, values[z][y][x]);
                }
            }
            stack.addSlice(processor);
        }
        return new ImagePlus(title, stack);
    }

    static ImagePlus floats(String title, float[][][] values) {
        int depth = values.length;
        int height = values[0].length;
        int width = values[0][0].length;
        ImageStack stack = new ImageStack(width, height);
        for (int z = 0; z < depth; z++) {
            FloatProcessor processor = new FloatProcessor(width, height);
            for (int y = 0; y < height; y++) {
                for (int x = 0; x < width; x++) {
                    processor.setf(x, y, values[z][y][x]);
                }
            }
            stack.addSlice(processor);
        }
        return new ImagePlus(title, stack);
    }

    private static ImageProcessor processor(int bitDepth, int width, int height) {
        switch (bitDepth) {
            case 8: return new ByteProcessor(width, height);
            case 32: return new FloatProcessor(width, height);
            default: return new ShortProcessor(width, height);
        }
    }
}

package product;

import data.Image;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Provides lightweight data wrangling helpers for MNIST samples.
 */
public class DataWrangler {
    /**
     * Returns the descriptive summary of a dataset (descriptive method requirement).
     */
    public static String describeDataset(List<Image> images) {
        if (images == null || images.isEmpty()) {
            return "Dataset is empty or missing.";
        }

        Map<Integer, Integer> distribution = getLabelDistribution(images);
        int maxLabel = distribution.entrySet().stream()
                .max(Map.Entry.comparingByValue())
                .map(Map.Entry::getKey)
                .orElse(-1);
        int maxCount = distribution.getOrDefault(maxLabel, 0);

        return String.format("Loaded %d samples, label mode %d (%d samples).",
                images.size(), maxLabel, maxCount);
    }

    /**
     * Predictive method used to anticipate next round training gains.
     */
    public static double predictNextEpochAccuracy(double currentAccuracy, double changeRate) {
        return Math.min(1.0, currentAccuracy + changeRate * 0.25);
    }

    public static Map<Integer, Integer> getLabelDistribution(List<Image> images) {
        Map<Integer, Integer> distribution = new HashMap<>();
        if (images == null) {
            return distribution;
        }
        for (Image image : images) {
            distribution.merge(image.getLabel(), 1, Integer::sum);
        }
        return distribution;
    }

    public static int countForLabel(List<Image> images, int label) {
        if (images == null) {
            return 0;
        }
        return (int) images.stream()
                .filter(image -> image.getLabel() == label)
                .count();
    }

    public static List<double[]> extractFeatureVectors(List<Image> images) {
        List<double[]> vectors = new ArrayList<>();
        if (images == null) {
            return vectors;
        }
        for (Image image : images) {
            vectors.add(flatten(normalize(image.getData())));
        }
        return vectors;
    }

    public static double[] flatten(double[][] data) {
        double[] flat = new double[data.length * data[0].length];
        int index = 0;
        for (double[] row : data) {
            for (double v : row) {
                flat[index++] = v;
            }
        }
        return flat;
    }

    public static double[][] normalize(double[][] data) {
        double max = Double.MIN_VALUE;
        double min = Double.MAX_VALUE;
        for (double[] row : data) {
            for (double val : row) {
                max = Math.max(max, val);
                min = Math.min(min, val);
            }
        }
        double range = max - min;
        double[][] normalized = new double[data.length][data[0].length];
        for (int i = 0; i < data.length; i++) {
            for (int j = 0; j < data[i].length; j++) {
                normalized[i][j] = range == 0 ? 0 : (data[i][j] - min) / range;
            }
        }
        return normalized;
    }

    public static List<Image> cleanSparsity(List<Image> images) {
        List<Image> cleaned = new ArrayList<>();
        if (images == null) {
            return cleaned;
        }
        for (Image image : images) {
            if (averageIntensity(image) > 0.02) {
                cleaned.add(image);
            }
        }
        return cleaned;
    }

    public static double averageIntensity(Image image) {
        double[][] data = image.getData();
        double total = 0;
        for (double[] row : data) {
            for (double value : row) {
                total += value;
            }
        }
        return total / (data.length * data[0].length);
    }
}

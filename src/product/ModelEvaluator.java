package product;

import data.Image;
import network.Main;
import network.NeuralNetwork;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
/**
 * Couples the existing neural network with the product dashboard so the data product exposes
 * evaluation, prediction, and decision-support logic without mutating the original network code.
 */
public class ModelEvaluator {
    private final MonitoringTool monitor;

    public ModelEvaluator(MonitoringTool monitor) {
        this.monitor = monitor;
    }

    /**
     * Evaluates accuracy on a dataset and logs the metric.
     */
    public float evaluateAccuracy(NeuralNetwork network, List<Image> dataset) {
        if (network == null || dataset == null || dataset.isEmpty()) {
            monitor.record("Network evaluation skipped: invalid inputs.");
            return 0f;
        }

        float accuracy = network.test(dataset);
        monitor.recordMetric("Accuracy", accuracy * 100);
        return accuracy;
    }

    /**
     * Builds a simple trend of accuracy measurements to display on the dashboard.
     */
    public List<Double> buildAccuracyTrend(float baseline, float current) {
        double next = DataWrangler.predictNextEpochAccuracy(current, current - baseline);
        List<Double> trend = new ArrayList<>();
        trend.add((double) (baseline * 100));
        trend.add((double) (current * 100));
        trend.add(next * 100);
        return trend;
    }

    /**
     * Decision support logic that drives the textual guidance area.
     */
    public String generateDecisionSupport(float currentAccuracy, float bestAccuracy) {
        if (currentAccuracy >= 0.80) {
            return "Accuracy is stable above 80%. Focus on deployment and monitoring.";
        } else if (bestAccuracy - currentAccuracy < 0.02) {
            return "Progress is slowing. Consider hyperparameter tuning or more clean samples.";
        } else {
            return "Continue training; recent improvement shows the network is still learning.";
        }
    }

    /**
     * Predictive method to meet requirement for non-descriptive capability.
     */
    public double predictTrainingOutcome(float currentAccuracy, float bestAccuracy) {
        return DataWrangler.predictNextEpochAccuracy(currentAccuracy, bestAccuracy - currentAccuracy);
    }

    /**
     * Creates a few sample predictions so the dashboard can explain how the model behaves.
     */
    public List<String> samplePredictions(NeuralNetwork network, List<Image> samples, int limit) {
        if (network == null || samples == null) {
            return Collections.emptyList();
        }
        List<String> predictions = new ArrayList<>();
        for (int i = 0; i < limit && i < samples.size(); i++) {
            Image image = samples.get(i);
            int guess = network.guess(image);
            predictions.add(String.format("Label %d -> %d", image.getLabel(), guess));
        }
        return predictions;
    }

    /**
     * Loads the saved network while honoring the main loader.
     */
    public NeuralNetwork loadSavedNetwork(String path) {
        return Main.loadNetwork(path);
    }

}

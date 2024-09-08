package network;

import Layers.Layer;
import data.Image;

import java.io.Serial;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

import static data.MatrixUtility.add;
import static data.MatrixUtility.multiply;

public class NeuralNetwork implements Serializable {
    @Serial
    private static final long serialVersionUID = 1L;
    List<Layer> _layers;  // List of layers in the neural network
    double scaleFactor;    // Scale factor used for normalizing input data

    /**
     * Constructor to initialize the NeuralNetwork with a list of layers and a scale factor.
     *
     * @param _layers List of layers that make up the neural network.
     * @param scaleFactor Scale factor for normalizing input data.
     */
    public NeuralNetwork(List<Layer> _layers, double scaleFactor) {
        this._layers = _layers;
        this.scaleFactor = scaleFactor;
        linkLayers();  // Link the layers together to form the network
    }

    /**
     * Links the layers of the network together, setting the next and previous layers.
     */
    private void linkLayers() {
        try {
            if (_layers.size() <= 1) {
                return;  // No linking needed if there's only one layer
            }

            for (int i = 0; i < _layers.size(); i++) {
                if (i == 0) {
                    // First layer, only has a next layer
                    _layers.get(i).set_nextLayer(_layers.get(i + 1));
                } else if (i == _layers.size() - 1) {
                    // Last layer, only has a previous layer
                    _layers.get(i).set_previousLayer(_layers.get(i - 1));
                } else {
                    // Middle layers, set both previous and next layers
                    _layers.get(i).set_previousLayer(_layers.get(i - 1));
                    _layers.get(i).set_nextLayer(_layers.get(i + 1));
                }
            }
        } catch (Exception e) {
            System.err.println("Error linking layers: " + e.getMessage());
            e.printStackTrace();
        }
    }

    /**
     * Calculates the error between the network's output and the correct answer.
     *
     * @param networkOutput Array of outputs from the network.
     * @param correctAnswer The correct label for the input data.
     * @return Array of error values.
     */
    public double[] getErrors(double[] networkOutput, int correctAnswer) {
        int numClasses = networkOutput.length;

        double[] expected = new double[numClasses];
        expected[correctAnswer] = 1;  // Set the correct class to 1

        return add(networkOutput, multiply(expected, -1));  // Calculate the error
    }

    /**
     * Finds the index of the maximum value in the array.
     *
     * @param in Array of values to search through.
     * @return The index of the maximum value.
     */
    private int getMaxIndex(double[] in) {
        double max = Double.NEGATIVE_INFINITY;  // Initialize with the smallest possible value
        int index = 0;

            for (int i = 0; i < in.length; i++) {
                if (in[i] > max) {
                    max = in[i];
                    index = i;
                }
            }

        return index;
    }

    /**
     * Makes a prediction (guess) based on the input image.
     *
     * @param image The input image to be classified.
     * @return The predicted label for the image.
     */
    public int guess(Image image) {
        int guess = -1;
            List<double[][]> inList = new ArrayList<>();
            inList.add(multiply(image.getData(), (1.0 / scaleFactor)));  // Normalize the input data

            double[] out = _layers.get(0).getOutput(inList);  // Get the output from the network
            guess = getMaxIndex(out);  // Find the index of the maximum output value

        return guess;
    }

    /**
     * Tests the network on a set of images and calculates the accuracy.
     *
     * @param images List of images to test the network on.
     * @return The accuracy of the network as a percentage.
     */
    public float test(List<Image> images) {
        int correct = 0;

            for (Image img : images) {
                int guess = guess(img);  // Make a prediction for each image
                if (guess == img.getLabel()) {
                    correct++;  // Increment if the guess is correct
                }
            }

        return ((float) correct / images.size());  // Return the accuracy as a percentage
    }

    /**
     * Trains the network on a set of images.
     *
     * @param images List of images to train the network on.
     */
    public void train(List<Image> images) {
        try {
            for (Image img : images) {
                List<double[][]> inList = new ArrayList<>();
                inList.add(multiply(img.getData(), (1.0 / scaleFactor)));  // Normalize the input data

                double[] out = _layers.get(0).getOutput(inList);  // Forward pass through the network
                double[] dldO = getErrors(out, img.getLabel());  // Calculate the errors

                _layers.get((_layers.size() - 1)).backPropagation(dldO);  // Backpropagation
            }
        } catch (Exception e) {
            System.err.println("Error during training: " + e.getMessage());
            e.printStackTrace();
        }
    }
}

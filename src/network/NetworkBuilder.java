package network;

import Layers.ConvolutionLayer;
import Layers.FullyConnectedLayer;
import Layers.Layer;
import Layers.MaxPoolLayer;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

public class NetworkBuilder implements Serializable {
    private static final long serialVersionUID = 1L;
    private NeuralNetwork network;  // The neural network being built
    private int _inputRows;  // Number of rows in the input data
    private int _inputColumns;  // Number of columns in the input data
    private double _scaleFactor;  // Scale factor for normalizing input data
    List<Layer> _layers;  // List to hold the layers of the network

    /**
     * Constructor to initialize the NetworkBuilder with input dimensions and scale factor.
     *
     * @param _inputRows Number of rows in the input data.
     * @param _inputColumns Number of columns in the input data.
     * @param _scaleFactor Scale factor for normalizing the input data.
     */
    public NetworkBuilder(int _inputRows, int _inputColumns, double _scaleFactor) {
        this._inputRows = _inputRows;
        this._inputColumns = _inputColumns;
        this._scaleFactor = _scaleFactor;
        _layers = new ArrayList<>();  // Initialize the list to hold layers
    }

    /**
     * Adds a Convolutional Layer to the network.
     *
     * @param numFilters Number of filters to use in the convolutional layer.
     * @param filterSize Size of each filter (assumed to be square).
     * @param stepSize Step size for the convolution operation.
     * @param learningRate Learning rate for the layer.
     * @param SEED Random seed for initializing weights.
     */
    public void addConvolutionLayer(int numFilters, int filterSize, int stepSize, double learningRate, long SEED) {
        try {
            if (_layers.isEmpty()) {
                // First layer, no previous layer exists
                _layers.add(new ConvolutionLayer(filterSize, stepSize, 1, _inputRows, _inputColumns, SEED, numFilters, learningRate));
            } else {
                // Add convolutional layer after existing layers
                Layer previous = _layers.get(_layers.size() - 1);
                _layers.add(new ConvolutionLayer(filterSize, stepSize, previous.getOutputLength(), previous.getOutputRows(), previous.getOutputColumns(), SEED, numFilters, learningRate));
            }
        } catch (Exception e) {
            System.err.println("Error adding Convolutional Layer: " + e.getMessage());
            e.printStackTrace();
        }
    }

    /**
     * Adds a Max Pooling Layer to the network.
     *
     * @param windowSize Size of the pooling window.
     * @param stepSize Step size for the pooling operation.
     */
    public void addMaxPoolLayer(int windowSize, int stepSize) {
        try {
            if (_layers.isEmpty()) {
                // First layer, no previous layer exists
                _layers.add(new MaxPoolLayer(stepSize, windowSize, 1, _inputRows, _inputColumns));
            } else {
                // Add max pooling layer after existing layers
                Layer previous = _layers.get(_layers.size() - 1);
                _layers.add(new MaxPoolLayer(stepSize, windowSize, previous.getOutputLength(), previous.getOutputRows(), previous.getOutputColumns()));
            }
        } catch (Exception e) {
            System.err.println("Error adding Max Pooling Layer: " + e.getMessage());
            e.printStackTrace();
        }
    }

    /**
     * Adds a Fully Connected Layer to the network.
     *
     * @param outLength Number of output neurons in the fully connected layer.
     * @param learningRate Learning rate for the layer.
     * @param SEED Random seed for initializing weights.
     */
    public void addFullyConnectedLayer(int outLength, double learningRate, long SEED) {
        try {
            if (_layers.isEmpty()) {
                // First layer, fully connected directly to input
                _layers.add(new FullyConnectedLayer(_inputColumns * _inputRows, outLength, SEED, learningRate));
            } else {
                // Add fully connected layer after existing layers
                Layer previous = _layers.get(_layers.size() - 1);
                _layers.add(new FullyConnectedLayer(previous.getOutputElements(), outLength, SEED, learningRate));
            }
        } catch (Exception e) {
            System.err.println("Error adding Fully Connected Layer: " + e.getMessage());
            e.printStackTrace();
        }
    }

    /**
     * Builds and returns the final NeuralNetwork object.
     *
     * @return The constructed NeuralNetwork object.
     */
    public NeuralNetwork buildNetwork() {
            network = new NeuralNetwork(_layers, _scaleFactor);
            return network;
    }
}

package Layers;

import java.io.Serial;
import java.util.List;
import java.util.Random;

public class FullyConnectedLayer extends Layer {
    @Serial
    private static final long serialVersionUID = 1L;
    private transient long SEED;  // Random seed for initializing weights
    private final double leak = 0.01;  // Leak factor for Leaky ReLU activation

    private double[][] _weights;  // Weights of the layer
    private int _inLength;  // Number of input neurons
    private int _outLength;  // Number of output neurons
    private double _learningRate;  // Learning rate for weight updates

    private double[] lastZ;  // Stores the weighted sum before activation
    private double[] lastX;  // Stores the input for use in backpropagation

    /**
     * Constructor to initialize the FullyConnectedLayer with specified parameters.
     *
     * @param _inLength Number of input neurons.
     * @param _outLength Number of output neurons.
     * @param SEED Random seed for weight initialization.
     * @param learningRate Learning rate for the layer.
     */
    public FullyConnectedLayer(int _inLength, int _outLength, long SEED, double learningRate) {
        this._inLength = _inLength;
        this._outLength = _outLength;
        this.SEED = SEED;
        this._learningRate = learningRate;

        _weights = new double[_inLength][_outLength];  // Initialize the weight matrix
        setRandomWeights();  // Set random weights
    }

    /**
     * Performs the forward pass of the fully connected layer.
     *
     * @param input The input vector to the layer.
     * @return The output vector after applying the weights and activation function.
     */
    public double[] fullyConnectedForwardPass(double[] input) {
        lastX = input;  // Store input for use in backpropagation

        double[] z = new double[_outLength];  // Weighted sum before activation
        double[] out = new double[_outLength];  // Output after activation

        try {
            for (int i = 0; i < _inLength; i++) {
                for (int j = 0; j < _outLength; j++) {
                    z[j] += input[i] * _weights[i][j];  // Calculate weighted sum
                }
            }

            lastZ = z;  // Store weighted sum for use in backpropagation

            for (int j = 0; j < _outLength; j++) {
                out[j] = reLu(z[j]);  // Apply ReLU activation function
            }
        } catch (Exception e) {
            System.err.println("Error during forward pass: " + e.getMessage());
            e.printStackTrace();
        }

        return out;
    }

    @Override
    public double[] getOutput(List<double[][]> input) {
            double[] vector = matrixToVector(input);
            return getOutput(vector);
    }

    @Override
    public double[] getOutput(double[] input) {
            double[] forwardPass = fullyConnectedForwardPass(input);
            if (_nextLayer != null) {
                return _nextLayer.getOutput(forwardPass);
            } else {
                return forwardPass;
            }
    }

    @Override
    public void backPropagation(double[] dLdO) {
        double[] dLdX = new double[_inLength];  // Gradient w.r.t input of this layer

        try {
            for (int k = 0; k < _inLength; k++) {
                double dLdX_sum = 0;

                for (int j = 0; j < _outLength; j++) {
                    double dOdz = derivativeReLu(lastZ[j]);  // Derivative of activation function
                    double dzdw = lastX[k];  // Partial derivative of z w.r.t weight
                    double dZdX = _weights[k][j];  // Partial derivative of z w.r.t input

                    double dLdw = dLdO[j] * dOdz * dzdw;  // Gradient w.r.t weight

                    _weights[k][j] -= dLdw * _learningRate;  // Update the weights

                    dLdX_sum += dLdO[j] * dOdz * dZdX;  // Accumulate gradient w.r.t input
                }

                dLdX[k] = dLdX_sum;
            }

            if (_previousLayer != null) {
                _previousLayer.backPropagation(dLdX);
            }
        } catch (Exception e) {
            System.err.println("Error during backpropagation: " + e.getMessage());
            e.printStackTrace();
        }
    }

    @Override
    public void backPropagation(List<double[][]> dLdO) {
            double[] vector = matrixToVector(dLdO);
            backPropagation(vector);
    }

    @Override
    public int getOutputLength() {
        return 0;  // This method is not used in a fully connected layer
    }

    @Override
    public int getOutputRows() {
        return 0;  // This method is not used in a fully connected layer
    }

    @Override
    public int getOutputColumns() {
        return 0;  // This method is not used in a fully connected layer
    }

    @Override
    public int getOutputElements() {
        return _outLength;  // Return the number of output elements (neurons)
    }

    /**
     * Sets the weights of the layer to random values using a Gaussian distribution.
     */
    public void setRandomWeights() {
        Random random = new Random(SEED);

            for (int i = 0; i < _inLength; i++) {
                for (int j = 0; j < _outLength; j++) {
                    _weights[i][j] = random.nextGaussian();  // Initialize weights with Gaussian distribution
                }
            }
    }

    /**
     * ReLU activation function.
     *
     * @param input The input value.
     * @return The output after applying ReLU.
     */
    public double reLu(double input) {
        return input > 0 ? input : 0;
    }

    /**
     * Derivative of the ReLU activation function.
     *
     * @param input The input value.
     * @return The derivative of ReLU.
     */
    public double derivativeReLu(double input) {
        return input > 0 ? 1 : leak;
    }
}

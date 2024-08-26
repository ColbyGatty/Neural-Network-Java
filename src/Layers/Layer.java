package Layers;

import java.util.ArrayList;
import java.util.List;

public abstract class Layer {

    // Pointers to the next and previous layers in the network
    protected Layer _nextLayer;
    protected Layer _previousLayer;

    /**
     * Gets the next layer in the network.
     *
     * @return The next layer.
     */
    public Layer get_nextLayer() {
        return _nextLayer;
    }

    /**
     * Sets the next layer in the network.
     *
     * @param _nextLayer The next layer.
     */
    public void set_nextLayer(Layer _nextLayer) {
        this._nextLayer = _nextLayer;
    }

    /**
     * Gets the previous layer in the network.
     *
     * @return The previous layer.
     */
    public Layer get_previousLayer() {
        return _previousLayer;
    }

    /**
     * Sets the previous layer in the network.
     *
     * @param _previousLayer The previous layer.
     */
    public void set_previousLayer(Layer _previousLayer) {
        this._previousLayer = _previousLayer;
    }

    // Abstract methods that must be implemented by subclasses
    public abstract double[] getOutput(List<double[][]> input);
    public abstract double[] getOutput(double[] input);

    public abstract void backPropagation(double[] dLdO);
    public abstract void backPropagation(List<double[][]> dLdO);

    public abstract int getOutputLength();
    public abstract int getOutputRows();
    public abstract int getOutputColumns();
    public abstract int getOutputElements();

    /**
     * Converts a list of matrices into a single vector.
     *
     * @param input List of matrices to be converted.
     * @return A single vector containing all the elements of the input matrices.
     */
    public double[] matrixToVector(List<double[][]> input) {
        int length = input.size();
        int rows = input.get(0).length;
        int columns = input.get(0)[0].length;

        double[] vector = new double[length * rows * columns];

        try {
            int i = 0;
            for (int l = 0; l < length; l++) {
                for (int r = 0; r < rows; r++) {
                    for (int c = 0; c < columns; c++) {
                        vector[i] = input.get(l)[r][c];
                        i++;
                    }
                }
            }
        } catch (Exception e) {
            System.err.println("Error converting matrix to vector: " + e.getMessage());
            e.printStackTrace();
        }

        return vector;
    }

    /**
     * Converts a vector into a list of matrices.
     *
     * @param input The vector to be converted.
     * @param length Number of matrices in the output list.
     * @param rows Number of rows in each matrix.
     * @param columns Number of columns in each matrix.
     * @return A list of matrices reconstructed from the input vector.
     */
    List<double[][]> vectorToMatrix(double[] input, int length, int rows, int columns) {
        List<double[][]> out = new ArrayList<>();

        try {
            int i = 0;
            for (int l = 0; l < length; l++) {
                double[][] matrix = new double[rows][columns];

                for (int r = 0; r < rows; r++) {
                    for (int c = 0; c < columns; c++) {
                        matrix[r][c] = input[i];
                        i++;
                    }
                }
                out.add(matrix);
            }
        } catch (Exception e) {
            System.err.println("Error converting vector to matrix: " + e.getMessage());
            e.printStackTrace();
        }

        return out;
    }
}

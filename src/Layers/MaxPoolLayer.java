package Layers;

import java.io.Serial;
import java.util.ArrayList;
import java.util.List;

public class MaxPoolLayer extends Layer {
    @Serial
    private static final long serialVersionUID = 1L;
    private int _stepSize;    // Step size for the pooling operation
    private int _windowSize;  // Size of the pooling window

    private int _inLength;    // Number of input channels
    private int _inRows;      // Number of input rows
    private int _inColumns;   // Number of input columns

    List<int[][]> _lastMaxRow;    // Stores the row indices of max values during pooling
    List<int[][]> _lastMaxColumn; // Stores the column indices of max values during pooling

    /**
     * Constructor to initialize the MaxPoolLayer with specified parameters.
     *
     * @param _stepSize Step size for the pooling operation.
     * @param _windowSize Size of the pooling window.
     * @param _inLength Number of input channels.
     * @param _inRows Number of input rows.
     * @param _inColumns Number of input columns.
     */
    public MaxPoolLayer(int _stepSize, int _windowSize, int _inLength, int _inRows, int _inColumns) {
        this._stepSize = _stepSize;
        this._windowSize = _windowSize;
        this._inLength = _inLength;
        this._inRows = _inRows;
        this._inColumns = _inColumns;
    }

    /**
     * Performs the forward pass of max pooling on the input data.
     *
     * @param input List of input matrices (one for each channel).
     * @return List of output matrices after max pooling.
     */
    public List<double[][]> maxPoolForwardPass(List<double[][]> input) {
        List<double[][]> output = new ArrayList<>();
        _lastMaxRow = new ArrayList<>();
        _lastMaxColumn = new ArrayList<>();

        try {
            for (int l = 0; l < input.size(); l++) {
                output.add(pool(input.get(l)));  // Apply pooling to each channel
            }
        } catch (Exception e) {
            System.err.println("Error during max pooling forward pass: " + e.getMessage());
            e.printStackTrace();
        }

        return output;
    }

    /**
     * Applies max pooling to a single input matrix.
     *
     * @param input The input matrix to apply pooling on.
     * @return The output matrix after pooling.
     */
    public double[][] pool(double[][] input) {
        double[][] output = new double[getOutputRows()][getOutputColumns()];

        int[][] maxRow = new int[getOutputRows()][getOutputColumns()];
        int[][] maxColumn = new int[getOutputRows()][getOutputColumns()];

        try {
            for (int r = 0; r < getOutputRows(); r += _stepSize) {
                for (int c = 0; c < getOutputColumns(); c += _stepSize) {

                    double max = Double.NEGATIVE_INFINITY;  // Initialize max with the smallest possible value
                    maxRow[r][c] = -1;
                    maxColumn[r][c] = -1;

                    for (int x = 0; x < _windowSize; x++) {
                        for (int y = 0; y < _windowSize; y++) {
                            if (r + x < input.length && c + y < input[0].length && max < input[r + x][c + y]) {
                                max = input[r + x][c + y];
                                maxRow[r][c] = r + x;
                                maxColumn[r][c] = c + y;
                            }
                        }
                    }

                    output[r][c] = max;
                }
            }
        } catch (Exception e) {
            System.err.println("Error during pooling: " + e.getMessage());
            e.printStackTrace();
        }

        _lastMaxRow.add(maxRow);
        _lastMaxColumn.add(maxColumn);

        return output;
    }

    @Override
    public double[] getOutput(List<double[][]> input) {
        try {
            List<double[][]> outputPool = maxPoolForwardPass(input);
            return _nextLayer.getOutput(outputPool);
        } catch (Exception e) {
            System.err.println("Error getting output: " + e.getMessage());
            e.printStackTrace();
            return null;
        }
    }

    @Override
    public double[] getOutput(double[] input) {
        try {
            List<double[][]> matrixList = vectorToMatrix(input, _inLength, _inRows, _inColumns);
            return getOutput(matrixList);
        } catch (Exception e) {
            System.err.println("Error converting input to matrix and getting output: " + e.getMessage());
            e.printStackTrace();
            return null;
        }
    }

    @Override
    public void backPropagation(double[] dLdO) {
        try {
            List<double[][]> matrixList = vectorToMatrix(dLdO, getOutputLength(), getOutputRows(), getOutputColumns());
            backPropagation(matrixList);
        } catch (Exception e) {
            System.err.println("Error during backpropagation with vector input: " + e.getMessage());
            e.printStackTrace();
        }
    }

    @Override
    public void backPropagation(List<double[][]> dLdO) {
        try {
            List<double[][]> dXdL = new ArrayList<>();
            int l = 0;

            for (double[][] array : dLdO) {
                double[][] error = new double[_inRows][_inColumns];

                for (int r = 0; r < getOutputRows(); r++) {
                    for (int c = 0; c < getOutputColumns(); c++) {

                        int max_i = _lastMaxRow.get(l)[r][c];
                        int max_j = _lastMaxColumn.get(l)[r][c];

                        if (max_i != -1) {
                            error[max_i][max_j] += array[r][c];
                        }
                    }
                }

                dXdL.add(error);
                l++;
            }

            if (_previousLayer != null) {
                _previousLayer.backPropagation(dXdL);
            }
        } catch (Exception e) {
            System.err.println("Error during backpropagation: " + e.getMessage());
            e.printStackTrace();
        }
    }

    @Override
    public int getOutputLength() {
        return _inLength;
    }

    @Override
    public int getOutputRows() {
        try {
            return (_inRows - _windowSize) / _stepSize + 1;
        } catch (Exception e) {
            System.err.println("Error calculating output rows: " + e.getMessage());
            e.printStackTrace();
            return -1; // Return an invalid value to indicate an error
        }
    }

    @Override
    public int getOutputColumns() {
        try {
            return (_inColumns - _windowSize) / _stepSize + 1;
        } catch (Exception e) {
            System.err.println("Error calculating output columns: " + e.getMessage());
            e.printStackTrace();
            return -1; // Return an invalid value to indicate an error
        }
    }

    @Override
    public int getOutputElements() {
        try {
            return _inLength * getOutputRows() * getOutputColumns();
        } catch (Exception e) {
            System.err.println("Error calculating output elements: " + e.getMessage());
            e.printStackTrace();
            return -1; // Return an invalid value to indicate an error
        }
    }
}

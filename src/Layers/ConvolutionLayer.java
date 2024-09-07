package Layers;

import java.io.Serial;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;

import static data.MatrixUtility.add;
import static data.MatrixUtility.multiply;

public class ConvolutionLayer extends Layer {
    @Serial
    private static final long serialVersionUID = 1L;
    private transient long SEED;  // Random seed for initializing filters

    private List<double[][]> _filters;  // List of filters for convolution
    private int _filterSize;  // Size of each filter (assumed to be square)
    private int _stepSize;  // Step size for the convolution operation

    private int _inLength;  // Number of input channels
    private int _inRows;  // Number of input rows
    private int _inColumns;  // Number of input columns
    private double _learningRate;  // Learning rate for updating filters

    private List<double[][]> _lastInput;  // Stores the last input received for backpropagation

    /**
     * Constructor to initialize the ConvolutionLayer with specified parameters.
     *
     * @param _filterSize Size of the filters.
     * @param _stepSize Step size for the convolution operation.
     * @param _inLength Number of input channels.
     * @param _inRows Number of input rows.
     * @param _inColumns Number of input columns.
     * @param SEED Random seed for filter initialization.
     * @param numFilters Number of filters to generate.
     * @param learningRate Learning rate for the layer.
     */
    public ConvolutionLayer(int _filterSize, int _stepSize, int _inLength, int _inRows, int _inColumns, long SEED, int numFilters, double learningRate) {
        this._filterSize = _filterSize;
        this._stepSize = _stepSize;
        this._inLength = _inLength;
        this._inRows = _inRows;
        this._inColumns = _inColumns;
        this.SEED = SEED;
        _learningRate = learningRate;

        generateRandomFilters(numFilters);  // Generate filters randomly
    }

    /**
     * Generates random filters for the convolution layer.
     *
     * @param numFilters Number of filters to generate.
     */
    private void generateRandomFilters(int numFilters) {
        _filters = new ArrayList<>();
        Random random = new Random(SEED);

        try {
            for (int n = 0; n < numFilters; n++) {
                double[][] newFilter = new double[_filterSize][_filterSize];

                for (int i = 0; i < _filterSize; i++) {
                    for (int j = 0; j < _filterSize; j++) {
                        double value = random.nextGaussian();  // Initialize filter weights with Gaussian distribution
                        newFilter[i][j] = value;
                    }
                }

                _filters.add(newFilter);
            }
        } catch (Exception e) {
            System.err.println("Error generating filters: " + e.getMessage());
            e.printStackTrace();
        }
    }

    /**
     * Performs the forward pass of the convolutional layer.
     *
     * @param list List of input matrices (one for each channel).
     * @return List of output matrices after convolution.
     */
    public List<double[][]> convolutionForwardPass(List<double[][]> list) {
        _lastInput = list;  // Store the input for use in backpropagation

        List<double[][]> output = new ArrayList<>();

        try {
            for (int m = 0; m < list.size(); m++) {
                for (double[][] filter : _filters) {
                    output.add(convolve(list.get(m), filter, _stepSize));  // Apply convolution for each filter
                }
            }
        } catch (Exception e) {
            System.err.println("Error during convolution forward pass: " + e.getMessage());
            e.printStackTrace();
        }

        return output;
    }

    /**
     * Applies convolution to a single input matrix using a single filter.
     *
     * @param input The input matrix.
     * @param filter The filter matrix.
     * @param stepSize The step size for the convolution operation.
     * @return The output matrix after convolution.
     */
    private double[][] convolve(double[][] input, double[][] filter, int stepSize) {
        int outRows = (input.length - filter.length) / stepSize + 1;
        int outColumns = (input[0].length - filter[0].length) / stepSize + 1;

        double[][] output = new double[outRows][outColumns];

        try {
            int outRow = 0;
            int outColumn;

            for (int i = 0; i <= input.length - filter.length; i += stepSize) {
                outColumn = 0;

                for (int j = 0; j <= input[0].length - filter[0].length; j += stepSize) {
                    double sum = 0.0;

                    // Apply filter over this region
                    for (int x = 0; x < filter.length; x++) {
                        for (int y = 0; y < filter[0].length; y++) {
                            int inputRowIndex = i + x;
                            int inputColumnIndex = j + y;

                            double value = filter[x][y] * input[inputRowIndex][inputColumnIndex];
                            sum += value;
                        }
                    }
                    output[outRow][outColumn] = sum;
                    outColumn++;
                }
                outRow++;
            }
        } catch (Exception e) {
            System.err.println("Error during convolution: " + e.getMessage());
            e.printStackTrace();
        }

        return output;
    }

    /**
     * Expands the input matrix by spacing out elements according to the step size.
     *
     * @param input The input matrix to be spaced.
     * @return The spaced matrix.
     */
    public double[][] spaceArray(double[][] input) {
        if (_stepSize == 1) {
            return input;  // No need to space the array if step size is 1
        }

        int outRows = (input.length - 1) * _stepSize + 1;
        int outColumns = (input[0].length - 1) * _stepSize + 1;

        double[][] output = new double[outRows][outColumns];

        try {
            for (int i = 0; i < input.length; i++) {
                for (int j = 0; j < input[0].length; j++) {
                    output[i * _stepSize][j * _stepSize] = input[i][j];
                }
            }
        } catch (Exception e) {
            System.err.println("Error spacing array: " + e.getMessage());
            e.printStackTrace();
        }

        return output;
    }

    @Override
    public double[] getOutput(List<double[][]> input) {
        try {
            List<double[][]> output = convolutionForwardPass(input);
            return _nextLayer.getOutput(output);
        } catch (Exception e) {
            System.err.println("Error getting output from convolution layer: " + e.getMessage());
            e.printStackTrace();
            return null;
        }
    }

    @Override
    public double[] getOutput(double[] input) {
        try {
            List<double[][]> matrixInput = vectorToMatrix(input, _inLength, _inRows, _inColumns);
            return getOutput(matrixInput);
        } catch (Exception e) {
            System.err.println("Error converting input to matrix and getting output: " + e.getMessage());
            e.printStackTrace();
            return null;
        }
    }

    @Override
    public void backPropagation(double[] dLdO) {
        try {
            List<double[][]> matrixInput = vectorToMatrix(dLdO, _inLength, _inRows, _inColumns);
            backPropagation(matrixInput);
        } catch (Exception e) {
            System.err.println("Error during backpropagation with vector input: " + e.getMessage());
            e.printStackTrace();
        }
    }

    @Override
    public void backPropagation(List<double[][]> dLdO) {
        try {
            List<double[][]> filtersDelta = new ArrayList<>();
            List<double[][]> dldOPreviousLayer = new ArrayList<>();

            for (int f = 0; f < _filters.size(); f++) {
                filtersDelta.add(new double[_filterSize][_filterSize]);
            }

            for (int i = 0; i < _lastInput.size(); i++) {
                double[][] errorForInput = new double[_inRows][_inColumns];

                for (int f = 0; f < _filters.size(); f++) {
                    double[][] currFilter = _filters.get(f);
                    double[][] error = dLdO.get(i * _filters.size() + f);

                    double[][] spacedError = spaceArray(error);
                    double[][] dldF = convolve(_lastInput.get(i), spacedError, 1);

                    double[][] delta = multiply(dldF, _learningRate * -1);
                    double[][] newTotalDelta = add(filtersDelta.get(f), delta);
                    filtersDelta.set(f, newTotalDelta);

                    double[][] flippedError = flipArrayHorizontally(flipArrayVertically(spacedError));
                    errorForInput = add(errorForInput, fullConvolve(currFilter, flippedError));
                }

                dldOPreviousLayer.add(errorForInput);
            }

            for (int f = 0; f < _filters.size(); f++) {
                double[][] modified = add(filtersDelta.get(f), _filters.get(f));
                _filters.set(f, modified);
            }

            if (_previousLayer != null) {
                _previousLayer.backPropagation(dldOPreviousLayer);
            }
        } catch (Exception e) {
            System.err.println("Error during backpropagation: " + e.getMessage());
            e.printStackTrace();
        }
    }

    /**
     * Flips an array horizontally.
     *
     * @param array The input array to flip.
     * @return The horizontally flipped array.
     */
    public double[][] flipArrayHorizontally(double[][] array) {
        int rows = array.length;
        int columns = array[0].length;

        double[][] output = new double[rows][columns];

        try {
            for (int i = 0; i < rows; i++) {
                for (int j = 0; j < columns; j++) {
                    output[rows - i - 1][j] = array[i][j];
                }
            }
        } catch (Exception e) {
            System.err.println("Error flipping array horizontally: " + e.getMessage());
            e.printStackTrace();
        }

        return output;
    }

    /**
     * Flips an array vertically.
     *
     * @param array The input array to flip.
     * @return The vertically flipped array.
     */
    public double[][] flipArrayVertically(double[][] array) {
        int rows = array.length;
        int columns = array[0].length;

        double[][] output = new double[rows][columns];

        try {
            for (int i = 0; i < rows; i++) {
                for (int j = 0; j < columns; j++) {
                    output[i][columns - j - 1] = array[i][j];
                }
            }
        } catch (Exception e) {
            System.err.println("Error flipping array vertically: " + e.getMessage());
            e.printStackTrace();
        }

        return output;
    }

    /**
     * Applies full convolution on the input array with the given filter.
     *
     * @param input The input array.
     * @param filter The filter array.
     * @return The output array after applying full convolution.
     */
    private double[][] fullConvolve(double[][] input, double[][] filter) {
        int outRows = (input.length + filter.length) + 1;
        int outColumns = (input[0].length + filter[0].length) + 1;

        double[][] output = new double[outRows][outColumns];

        try {
            int outRow = 0;
            int outColumn;

            for (int i = -filter.length + 1; i < input.length; i++) {
                outColumn = 0;

                for (int j = -filter[0].length + 1; j < input[0].length; j++) {
                    double sum = 0.0;

                    // Apply filter over this region
                    for (int x = 0; x < filter.length; x++) {
                        for (int y = 0; y < filter[0].length; y++) {
                            int inputRowIndex = i + x;
                            int inputColumnIndex = j + y;

                            if (inputRowIndex >= 0 && inputColumnIndex >= 0 && inputRowIndex < input.length && inputColumnIndex < input[0].length) {
                                double value = filter[x][y] * input[inputRowIndex][inputColumnIndex];
                                sum += value;
                            }
                        }
                    }
                    output[outRow][outColumn] = sum;
                    outColumn++;
                }
                outRow++;
            }
        } catch (Exception e) {
            System.err.println("Error during full convolution: " + e.getMessage());
            e.printStackTrace();
        }

        return output;
    }

    @Override
    public int getOutputLength() {
        try {
            return _filters.size() * _inLength;
        } catch (Exception e) {
            System.err.println("Error calculating output length: " + e.getMessage());
            e.printStackTrace();
            return -1;  // Return an invalid value to indicate an error
        }
    }

    @Override
    public int getOutputRows() {
        try {
            return (_inRows - _filterSize) / _stepSize + 1;
        } catch (Exception e) {
            System.err.println("Error calculating output rows: " + e.getMessage());
            e.printStackTrace();
            return -1;  // Return an invalid value to indicate an error
        }
    }

    @Override
    public int getOutputColumns() {
        try {
            return (_inColumns - _filterSize) / _stepSize + 1;
        } catch (Exception e) {
            System.err.println("Error calculating output columns: " + e.getMessage());
            e.printStackTrace();
            return -1;  // Return an invalid value to indicate an error
        }
    }

    @Override
    public int getOutputElements() {
        try {
            return getOutputColumns() * getOutputRows() * getOutputLength();
        } catch (Exception e) {
            System.err.println("Error calculating output elements: " + e.getMessage());
            e.printStackTrace();
            return -1;  // Return an invalid value to indicate an error
        }
    }
}

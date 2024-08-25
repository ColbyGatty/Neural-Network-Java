package Layers;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

import static data.MatrixUtility.add;
import static data.MatrixUtility.multiply;

public class ConvolutionLayer extends Layer {

    private long SEED;

    private List<double[][]> _filters;
    private int _filterSize;
    private int _stepSize;

    private int _inLength;
    private int _inRows;
    private int _inColumns;
    private double _learningRate;

    private List<double[][]> _lastInput;

    public ConvolutionLayer(int _filterSize, int _stepSize, int _inLength, int _inRows, int _inColumns, long SEED, int numFilters, double learningRate) {
        this._filterSize = _filterSize;
        this._stepSize = _stepSize;
        this._inLength = _inLength;
        this._inRows = _inRows;
        this._inColumns = _inColumns;
        this.SEED = SEED;
        this._learningRate = learningRate;

        generateRandomFilters(numFilters);

    }

    private void generateRandomFilters(int numFilters){

        List<double[][]> filters = new ArrayList<>();
        Random random = new Random(SEED);

        for(int n = 0; n < numFilters; n++){
            double[][] newFilter = new double[_filterSize][_filterSize];

            for(int i = 0; i < _filterSize; i++){
                for(int j = 0; j < _filterSize; j++){

                    double value = random.nextGaussian();
                    newFilter[i][j] = value;
                }
            }

            filters.add(newFilter);
        }

        _filters = filters;

    }

    public List<double[][]> convolutionForwardPass(List<double[][]> list){
        _lastInput = list;

        List<double[][]> output = new ArrayList<>();

        for(int n = 0; n < list.size(); n++){
            for(double[][] filter : _filters){
                output.add(convolve(list.get(n), filter, _stepSize ));
            }
        }

        return output;

    }

    private double[][] convolve(double[][] input, double[][] filter, int stepSize) {

        int outRows = (input.length - filter.length)/stepSize + 1;
        int outColumns = (input[0].length - filter[0].length)/stepSize + 1;

        int inRows = input.length;
        int inColumns = input[0].length;

        int fRows = filter.length;
        int fColumns = filter[0].length;

        double[][] output = new double[outRows][outColumns];

        int outRow = 0;
        int outColumn;

        for(int i = 0; i <= inRows - fRows; i += stepSize) {

            outColumn = 0;

            for (int j = 0; j <= inColumns - fColumns; j += stepSize) {

                double sum = 0.0;

                //Applying filters around this position
                for (int x = 0; x < fRows; x++) {
                    for (int y = 0; y < fColumns; y++) {
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
        return output;
    }

    public double[][] spaceArray(double[][] input){

        if(_stepSize == 1){
            return input;
        }

        int outRows = (input.length - 1)*_stepSize + 1;
        int outColumns = (input[0].length - 1)*_stepSize + 1;

        double[][] output = new double[outRows][outColumns];

        for(int i = 0; i < input.length; i++){
            for(int j = 0; j < input[0].length; j++){
                output[i*_stepSize][j*_stepSize] = input[i][j];
            }
        }

        return output;

    }


    @Override
    public double[] getOutput(List<double[][]> input) {
        List<double[][]> output = convolutionForwardPass(input);

        return _nextLayer.getOutput(output);
    }

    @Override
    public double[] getOutput(double[] input) {

        List<double[][]> matrixInput = vectorToMatrix(input, _inLength, _inRows, _inColumns);

        return getOutput(matrixInput);

    }

    @Override
    public void backPropagation(double[] dLdO) {

        List<double[][]> matrixInput = vectorToMatrix(dLdO, _inLength, _inRows, _inColumns);
        backPropagation(matrixInput);

    }

    @Override
    public void backPropagation(List<double[][]> dLdO) {

        List<double[][]> filtersDelta = new ArrayList<>();
        List<double[][]> dldOPreviousLayer = new ArrayList<>();

        for(int f = 0; f < _filters.size(); f++){
            filtersDelta.add(new double[_filterSize][_filterSize]);
        }

        for(int i = 0; i < _lastInput.size(); i++){

            double[][] errorForInput = new double[_inRows][_inColumns];

            for(int f = 0; f < _filters.size(); f++){

                double[][] currFilter = _filters.get(f);
                double[][] error = dLdO.get(i*_filters.size() + f);

                double[][] spacedError = spaceArray(error);
                double[][] dldF = convolve(_lastInput.get(i), spacedError, 1);

                double[][] delta = multiply(dldF, _learningRate*-1);
                double[][] newTotalDelta = add(filtersDelta.get(f), delta);
                filtersDelta.set(f, newTotalDelta);

                double[][] flippedError = flipArrayHorizontally(flipArrayVertically(spacedError));
                errorForInput = add(errorForInput, fullConvolve(currFilter, flippedError));

            }

            dldOPreviousLayer.add(errorForInput);

        }
        for(int f = 0; f < _filters.size(); f++){
            double[][] modified = add(filtersDelta.get(f), _filters.get(f));
            _filters.set(f, modified);
        }

        if(_previousLayer != null){
            _previousLayer.backPropagation(dldOPreviousLayer);
        }

    }

    public double[][] flipArrayHorizontally(double[][] array){
        int rows = array.length;
        int columns = array[0].length;

        double[][] output = new double[rows][columns];

        for(int i = 0; i < rows; i++){
            for(int j = 0; j < columns; j++){
                output[rows-i-1][j] = array[i][j];
            }
        }

        return output;

    }

    public double[][] flipArrayVertically(double[][] array){
        int rows = array.length;
        int columns = array[0].length;

        double[][] output = new double[rows][columns];

        for(int i = 0; i < rows; i++){
            for(int j = 0; j < columns; j++){
                output[i][columns-i-1] = array[i][j];
            }
        }

        return output;

    }

    private double[][] fullConvolve(double[][] input, double[][] filter) {

        int outRows = (input.length + filter.length) + 1;
        int outColumns = (input[0].length + filter[0].length) + 1;

        int inRows = input.length;
        int inColumns = input[0].length;

        int fRows = filter.length;
        int fColumns = filter[0].length;

        double[][] output = new double[outRows][outColumns];

        int outRow = 0;
        int outColumn;

        for(int i = -fRows; i < inRows; i++) {

            outColumn = 0;

            for (int j = -fColumns; j < inColumns; j++) {

                double sum = 0.0;

                //Applying filters around this position
                for (int x = 0; x < fRows; x++) {
                    for (int y = 0; y < fColumns; y++) {
                        int inputRowIndex = i + x;
                        int inputColumnIndex = j + y;

                        if(inputRowIndex >= 0 && inputColumnIndex >= 0 && inputRowIndex < inRows && inputColumnIndex < inColumns){
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
        return output;
    }

    @Override
    public int getOutputLength() {
        return _filters.size()*_inLength;
    }

    @Override
    public int getOutputRows() {
        return (_inRows-_filterSize)/_stepSize + 1;
    }

    @Override
    public int getOutputColumns() {
        return (_inColumns-_filterSize)/_stepSize + 1;
    }

    @Override
    public int getOutputElements() {
        return getOutputColumns()*getOutputRows()*getOutputLength();
    }
}

package Layers;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

public class ConvolutionLayer extends Layer {

    private long SEED;

    private List<double[][]> _filters;
    private int _filterSize;
    private int _stepSize;

    private int _inLength;
    private int _inRows;
    private int _inColumns;

    public ConvolutionLayer(int _filterSize, int _stepSize, int _inLength, int _inRows, int _inColumns, long SEED, int numFilters) {
        this._filterSize = _filterSize;
        this._stepSize = _stepSize;
        this._inLength = _inLength;
        this._inRows = _inRows;
        this._inColumns = _inColumns;
        this.SEED = SEED;

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

        List<double[][]> output = new ArrayList<>();

        for(int n = 0; n < list.size(); n++){
            for(double[][] filter : _filters){
                output.add(convolve(list.get(n), filter, _stepSize ))
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

    }

    @Override
    public void backPropagation(List<double[][]> dLdO) {

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

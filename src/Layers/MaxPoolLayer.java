package Layers;

import java.util.ArrayList;
import java.util.List;

public class MaxPoolLayer extends Layer{

    private int _stepSize;
    private int _windowSize;

    private int _inLength;
    private int _inRows;
    private int _inColumns;

    List<int[][]> _lastMaxRow;
    List<int[][]> _lastMaxColumn;

    public MaxPoolLayer(int _stepSize, int _windowSize, int _inLength, int _inRows, int _inColumns) {
        this._stepSize = _stepSize;
        this._windowSize = _windowSize;
        this._inLength = _inLength;
        this._inRows = _inRows;
        this._inColumns = _inColumns;
    }

    public List<double[][]> maxPoolForwardPass(List<double[][]> input) {

        List<double[][]> output = new ArrayList<>();
        _lastMaxRow = new ArrayList<>();
        _lastMaxColumn = new ArrayList<>();

        for(int l = 0; l < input.size(); l++) {
            output.add(pool(input.get(l)));
        }

        return output;
    }

    public double[][] pool(double[][] input) {

        double[][] output = new double[getOutputRows()][getOutputColumns()];

        int[][] maxRow = new int[getOutputRows()][getOutputColumns()];
        int[][] maxColumn = new int[getOutputRows()][getOutputColumns()];

        for(int r = 0; r < getOutputRows(); r+=_stepSize) {
            for(int c = 0; c < getOutputColumns(); c+=_stepSize) {

                double max = 0.0;
                maxRow[r][c] = -1;
                maxColumn[r][c] = -1;

                for(int x = 0; x < _windowSize; x++){
                    for(int y = 0; y < _windowSize; y++){
                        if(max < input[r + x][c + y]) {
                            max = input[r + x][c + y];
                            maxRow[r][c] = r+x;
                            maxColumn[r][c] = c+y;
                        }
                    }
                }

                output[r][c] = max;

            }
        }

        _lastMaxRow.add(maxRow);
        _lastMaxColumn.add(maxColumn);

        return output;

    }


    @Override
    public double[] getOutput(List<double[][]> input) {
        List<double[][]> outputPool = maxPoolForwardPass(input);
        return _nextLayer.getOutput(outputPool);
    }

    @Override
    public double[] getOutput(double[] input) {
        List<double[][]> matrixList = vectorToMatrix(input, _inLength, _inRows, _inColumns);
        return getOutput(matrixList);
    }

    @Override
    public void backPropagation(double[] dLdO) {
        List<double[][]> matrixList = vectorToMatrix(dLdO, getOutputLength(), getOutputRows(), getOutputColumns());
        backPropagation(matrixList);
    }

    @Override
    public void backPropagation(List<double[][]> dLdO) {

        List<double[][]> dXdL = new ArrayList<>();

        int l = 0;
        for(double[][] array : dLdO){
            double[][] error = new double[_inRows][_inColumns];

            for(int r = 0; r < getOutputRows(); r++) {
                for(int c = 0; c < getOutputColumns(); c++) {

                    int max_i = _lastMaxRow.get(l)[r][c];
                    int max_j = _lastMaxColumn.get(l)[r][c];

                    if(max_i != -1){
                        error[max_i][max_j] += array[r][c];
                    }
                }
            }

            dXdL.add(error);
            l++;
        }

        if(_previousLayer != null){
            _previousLayer.backPropagation(dXdL);
        }

    }

    @Override
    public int getOutputLength() {
        return _inLength;
    }

    @Override
    public int getOutputRows() {
        return (_inRows-_windowSize)/_stepSize + 1;
    }

    @Override
    public int getOutputColumns() {
        return (_inColumns-_windowSize)/_stepSize + 1;
    }

    @Override
    public int getOutputElements() {
        return _inLength*getOutputRows()*getOutputColumns();
    }
}

package network;

import Layers.ConvolutionLayer;
import Layers.FullyConnectedLayer;
import Layers.Layer;
import Layers.MaxPoolLayer;

import java.util.ArrayList;
import java.util.List;

public class NetworkBuilder {

    private NeuralNetwork network;
    private int _inputRows;
    private int _inputColumns;
    private double _scaleFactor;
    List<Layer> _layers;

    public NetworkBuilder(int _inputRows, int _inputColumns, double _scaleFactor) {
        this._inputRows = _inputRows;
        this._inputColumns = _inputColumns;
        this._scaleFactor = _scaleFactor;
        _layers = new ArrayList<>();
    }

    public void addConvolutionLayer(int numFilters, int filterSize, int stepSize, double learningRate, long SEED) {
        if(_layers.isEmpty()){
            _layers.add(new ConvolutionLayer(filterSize, stepSize, 1, _inputRows, _inputColumns, SEED, numFilters, learningRate));
        } else{
            Layer previous = _layers.get(_layers.size()-1);
            _layers.add(new ConvolutionLayer(filterSize, stepSize, previous.getOutputLength(), previous.getOutputRows(), previous.getOutputColumns(), SEED, numFilters, learningRate));
        }
    }

    public void addMaxPoolLayer(int windowSize, int stepSize) {
        if(_layers.isEmpty()){
            _layers.add(new MaxPoolLayer(stepSize, windowSize, 1, _inputRows, _inputColumns));
        } else{
            Layer previous = _layers.get(_layers.size()-1);
            _layers.add(new MaxPoolLayer(stepSize, windowSize, previous.getOutputLength(), previous.getOutputRows(), previous.getOutputColumns()));
        }
    }

    public void addFullyConnectedLayer(int outLength, double learningRate, long SEED) {
        if(_layers.isEmpty()){
            _layers.add(new FullyConnectedLayer(_inputColumns*_inputRows, outLength, SEED, learningRate));
        }else{
            Layer previous = _layers.get(_layers.size()-1);
            _layers.add(new FullyConnectedLayer(previous.getOutputElements(), outLength, SEED, learningRate));
        }
    }

    public NeuralNetwork buildNetwork() {
        network = new NeuralNetwork(_layers, _scaleFactor);
        return network;
    }

}

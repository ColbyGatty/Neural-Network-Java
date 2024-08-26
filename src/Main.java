import data.DataReader;
import data.Image;
import network.NetworkBuilder;
import network.NeuralNetwork;

import java.util.List;

import static java.util.Collections.shuffle;

public class Main {

    public static void main(String[] args) {

        long SEED = 123;  // Seed for random number generation

        System.out.println("Starting data loading...");

        // Load test and training data from CSV files
        List<Image> imagesTest;
        List<Image> imagesTrain;

        try {
            imagesTest = new DataReader().readData("data/mnist_test.csv");
            imagesTrain = new DataReader().readData("data/mnist_train.csv");
        } catch (IllegalArgumentException e) {
            System.err.println("Error loading data: " + e.getMessage());
            return;  // Exit the program if data loading fails
        }

        // Output the size of the training and test datasets
        System.out.println("Images Train Size: " + imagesTrain.size());
        System.out.println("Images Test Size: " + imagesTest.size());

        // Build the neural network
        NetworkBuilder builder = new NetworkBuilder(28, 28, 256 * 100);
        builder.addConvolutionLayer(8, 5, 1, 0.1, SEED);
        builder.addMaxPoolLayer(3, 2);
        builder.addFullyConnectedLayer(10, 0.1, SEED);

        NeuralNetwork network = builder.buildNetwork();

        // Test the network's performance before training
        float rate = network.test(imagesTest);
        System.out.println("Network pre-training success rate: " + rate);

        // Train the network for a specified number of epochs
        int epochs = 3;

        for (int i = 0; i < epochs; i++) {
            shuffle(imagesTrain);  // Shuffle the training data before each epoch
            network.train(imagesTrain);  // Train the network on the shuffled data
            rate = network.test(imagesTest);  // Test the network after training
            System.out.println("Success Rate after round " + i + ": " + rate);
        }
    }
}

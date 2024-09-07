package network;

import data.DataReader;
import data.Image;

import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.util.List;

import static java.util.Collections.shuffle;

public class Main {

    public static void main(String[] args) {
        long SEED = 123;// Seed for random number generation

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
        NetworkBuilder builder = new NetworkBuilder(28, 28, 256 * 100 );
        builder.addConvolutionLayer(8,5, 1, 0.1, SEED);
        builder.addMaxPoolLayer(3, 2);
        builder.addFullyConnectedLayer(10, 0.1, SEED);

        NeuralNetwork network = builder.buildNetwork();


        // Test the network's performance before training
        float bestRate = network.test(imagesTest);
        System.out.println("Network pre-training success rate: " + bestRate);

        // Train the network for a specified number of epochs
        // Early stopping parameters
        int epochs = 3; // Maximum number of epochs to run
        int patience = 5; // Number of epochs to wait for an improvement before stopping
        int epochsWithoutImprovement = 0; // Counter for epochs without improvement

        // Training loop with early stopping
        for (int i = 0; i < epochs; i++) {
            shuffle(imagesTrain);  // Shuffle the training data before each epoch
            network.train(imagesTrain);  // Train the network on the shuffled data
            float currentRate = network.test(imagesTest);  // Test the network after training
            System.out.println("Success Rate after round " + i + ": " + currentRate);

            // Check if the current model is the best so far
            if (currentRate > bestRate) {
                bestRate = currentRate;
                epochsWithoutImprovement = 0;  // Reset the counter
                saveNetwork(network, "out/trained_networkV5.ser");  // Save the best network
                System.out.println("New best success rate: " + bestRate + ". Model saved.");
            } else {
                epochsWithoutImprovement++;
                if (epochsWithoutImprovement >= patience) {
                    System.out.println("Early stopping triggered. Training stopped.");
                    break;  // Exit the training loop
                }
            }
        }
    }

    // Method to save the trained network to a file
    public static void saveNetwork(NeuralNetwork network, String filepath) {
        try (FileOutputStream fileOut = new FileOutputStream(filepath);
             ObjectOutputStream out = new ObjectOutputStream(fileOut)) {
            out.writeObject(network);
            System.out.println("Neural Network saved to " + filepath);
        } catch (Exception e) {
            System.err.println("Error saving neural network: " + e.getMessage());
            e.printStackTrace();
        }
    }

    // Method to load the trained network from a file
    public static NeuralNetwork loadNetwork(String filepath) {
        try (FileInputStream fileIn = new FileInputStream(filepath);
             ObjectInputStream in = new ObjectInputStream(fileIn)) {
            return (NeuralNetwork) in.readObject();
        } catch (Exception e) {
            System.err.println("Error loading neural network: " + e.getMessage());
            e.printStackTrace();
            return null;
        }
    }
}

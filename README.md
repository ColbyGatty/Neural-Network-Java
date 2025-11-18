# Neural Network Java Project

## Overview

This project is a Java-based implementation of a Convolutional Neural Network (CNN) designed to recognize handwritten digits from the MNIST dataset. The neural network is built from scratch without any machine learning libraries, providing a deep understanding of how each component of a neural network operates.

## Project Structure

The project is organized into several packages, each serving a distinct purpose:

### 1. `data`
This package contains classes responsible for handling the input data.

- **`Image`**: This class represents an image from the MNIST dataset. It stores the pixel data in a 2D array and the label (the actual digit). It also includes methods to retrieve the image data and label, and a `toString()` method for converting the image to a string representation. This class also houses data augmentation methods for translating and diversifying the MNIST Data Set.

- **`DataReader`**: This class reads the MNIST dataset from CSV files and converts it into a list of `Image` objects. The data is parsed into a 2D array of pixel values and an integer label. The `readData()` method handles the file reading and parsing. To enable or disable data augmentations, the data translations at the bottom of the readData() method can be commented out or uncommented to include the translation. I experienced the best results when including at least the translations left 5px and right 5px. Take note that the more translations you include the longer the network will take to train.

- **`MatrixUtility`**: This utility class provides static methods for basic matrix and vector operations, such as addition and scalar multiplication. These operations are fundamental to the neural network's computations.

### 2. `network`
This package contains classes that define the structure and behavior of the neural network.

- **`NeuralNetwork`**: This is the core class representing the neural network. It manages the layers, links them together, and provides methods for training (`train()`), testing (`test()`), and making predictions (`guess()`). The network operates on a list of layers, executing forward passes and backpropagation to adjust the weights.

- **`NetworkBuilder`**: This class is responsible for constructing the neural network. It allows you to sequentially add layers, including convolutional, max-pooling, and fully connected layers. Once all layers are added, the `buildNetwork()` method is called to link the layers and return a `NeuralNetwork` object.

### 3. `Layers`
This package defines the different types of layers used in the neural network.

- **`Layer`**: This is an abstract base class for all layers in the network. It defines the essential methods that each layer must implement, such as `getOutput()` and `backPropagation()`. It also provides utility methods for converting between matrices and vectors.

- **`ConvolutionLayer`**: This class implements a convolutional layer, which applies a series of filters to the input image to extract features. It supports forward passes and backpropagation for learning.

- **`MaxPoolLayer`**: This class implements a max-pooling layer, which reduces the spatial dimensions of the input by taking the maximum value over a window. This helps to reduce the complexity of the network and prevents overfitting.

- **`FullyConnectedLayer`**: This class implements a fully connected layer, which connects every neuron in the input to every neuron in the output. It is typically used at the end of the network to combine features extracted by previous layers and make the final prediction.

### 4. `network.Main`
This is the entry point of the application. It loads the data, constructs the neural network, and trains it on the MNIST dataset.

- **`main()`**: The main method performs the following steps:
    1. **Data Loading**: It loads the MNIST training and test datasets from CSV files.
    2. **Network Construction**: It builds the neural network using the `NetworkBuilder` class, adding convolutional, max-pooling, and fully connected layers.
    3. **Pre-Training Test**: It tests the network on the test dataset before any training to establish a baseline performance.
    4. **Training**: It trains the network over a specified number of epochs, shuffling the training data before each epoch.
    5. **Post-Training Test**: After each epoch, it tests the network again to evaluate its performance and prints the success rate.

 ### 5. `ui.DigitDrawUI`
After Main has completed execution and the network has completed the specified number of epochs in the Main class, the network should be saved as output in the format of a .ser file. Next make sure the main method at the bottom of the DigitDrawUI class loads the .ser file from the correct filepath then run the DigitDrawUI class. This should load the saved neural network and open a simple UI where a user can draw any digit between 0 through 9 and after hitting submit, will recieve the networks guess. You should notice that it gets it right very often but there will still be a small margin of incorrect output. This can be tailored by shifting the metrics in the network builder class in Main or by adding additional layers to handle more complexity. 

## Requirements

### Prerequisites

- **Java Development Kit (JDK)**: Ensure you have JDK installed (version 8 or higher is recommended).
- **MNIST Dataset**: You need to download the MNIST training and test dataset CSV files.

### Directory Setup

1. **Create a "Data" Directory**:
    - In the root directory of the project (`Neural-network-java`), create a folder named `Data`.

2. **Download MNIST Dataset**:
    - Download the following files:
        - `mnist_train.csv`
        - `mnist_test.csv`
    - Place these files into the `Data` directory you just created.

## Running the Project

### Steps to Run

1. **Compile the Project**:
    - Open a terminal and navigate to the project directory.
    - Run the following command to compile the Java files:
      ```sh
      javac -d out -sourcepath src src/network.Main.java
      ```

2. **Run the network.Main Class**:
    - After compilation, run the project using the following command:
      ```sh
      java -cp out network.Main
      ```

3. **Observe the Output**:
    - The program will start by loading the MNIST dataset.
    - It will then construct the neural network, test its initial performance, train it over several epochs, and print the success rate after each epoch.

### Expected Output

- You should see output indicating the size of the training and test datasets.
- The initial (pre-training) success rate will be low, as the network hasn't learned yet.
- After each epoch, the success rate should improve as the network learns from the training data.
- After the specified number of epochs set in Main have run the network should be saved as output in the form of a .ser file.
- Next ensure the DigitDrawUI class is pointing to the correct .ser file path for your saved network. This can be found toward the botom of the class.
- Run the DigitDrawUI class and the UI should pop up. Try drawing a 7 and hit submit to recieve the networks guess.
- NOTE: The MNIST data set is only for hand drawn numbers 0 through 9 so the network will only be able to properly guess drawn digits within that range.

### Product Dashboard & Governance

A simple governance dashboard can be launched after training so stakeholders can explore the MNIST samples, monitor accuracy, and access decision support without modifying the core network or drawing UI. The dashboard reads the same collected datasets (`Data/mnist_train.csv` and `Data/mnist_test.csv`) and wires in the persisted model (`out/trained_networkV6.ser` or the V5 fallback).

Build and run the dashboard with:

```sh
javac -d out -sourcepath src src/product/ProductDashboard.java
NN_DASHBOARD_TOKEN=mnist-dashboard-key java -cp out product.ProductDashboard
```

If the environment variable is not set, the dashboard will prompt for the same token before it opens; the SHA-256 guard is implemented in `product.SecurityGuard`.

The dashboard serves the remaining product requirements:

1. **Descriptive vs. predictive insights** – `DataWrangler.describeDataset(...)` provides the descriptive summary while `ModelEvaluator.predictTrainingOutcome(...)` offers a predictive look at the next epoch.
2. **Featurizing, cleaning, parsing, and wrangling** – `product.DataWrangler` exposes helpers such as `cleanSparsity`, `extractFeatureVectors`, and `normalize` so the samples can be prepped before exploration.
3. **Decision support & accuracy monitoring** – The central panel displays decision guidance from `ModelEvaluator.generateDecisionSupport`, while accuracy evaluation is wired to the trained `NeuralNetwork` via `ModelEvaluator.evaluateAccuracy` and surfaced through `MonitoringTool`.
4. **Interactive queries & visualization** – Use the digit count query field to inspect label frequencies, explore the two analytics charts (bar chart for label distribution and line chart for accuracy trends), and draw a digit directly in the embedded sketch pad panel to see the model’s live prediction inside the dashboard. If the prediction is incorrect, type the correct digit into the correction box below the sketch pad and click “Save Correction”; the sample is written to `data/user_corrections.csv` and will automatically join the next training run so the model improves from your corrections.
5. **Monitoring & maintenance tooling** – `MonitoringTool` captures dataset loads, queries, and health checks, and the UI exposes refresh and health-check buttons so operators can keep the product in shape.
6. **Machine learning & evaluation** – The dashboard calls the existing `NeuralNetwork.test(...)` method and shows the result alongside recent accuracy, giving visibility into the machine-learning behavior without modifying the core network logic.

These additions keep the neural network and draw UI untouched while satisfying the governance, visualization, security, and monitoring expectations for the capstone product.

## Code Explanation

### Key Concepts

- **Convolutional Neural Networks (CNNs)**: CNNs are designed to process data that has a grid-like topology, such as images. They are particularly effective for image recognition tasks.
- **Layers**: Layers in a neural network transform the input data into something more abstract and useful for making predictions. Convolutional layers extract features, max-pooling layers reduce dimensionality, and fully connected layers make final predictions.
- **Training and Backpropagation**: Training involves feeding the network with input data and adjusting the weights using backpropagation to minimize the difference between the predicted and actual output.

## Additional Notes

- **Performance Considerations**: This implementation is for educational purposes and is simply an exploration of creating a convolution neural network from scratch.
- **Scalability**: While the project handles a small dataset like MNIST well, more complex tasks or larger datasets may require additional optimizations and enhancements.

## Conclusion

This project provides a hands-on understanding of how a convolutional neural network is structured and trained from scratch in Java. By following the steps outlined in this `README`, you should be able to load the MNIST dataset, train the network, and observe its performance as it learns to recognize handwritten digits. You should then be able to load the saved Model into the Ui, Draw digits between 0-9, and then recieve the networks prediction.

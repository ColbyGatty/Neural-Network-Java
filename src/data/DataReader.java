package data;

import java.io.BufferedReader;
import java.io.FileReader;
import java.util.ArrayList;
import java.util.List;

public class DataReader {

    private final int rows = 28;   // Number of rows in the image (28x28 for MNIST dataset)
    private final int columns = 28; // Number of columns in the image (28x28 for MNIST dataset)

    /**
     * Reads image data from a CSV file and returns a list of Image objects.
     *
     * @param path The path to the CSV file containing the image data.
     * @return List of Image objects with pixel data and labels.
     * @throws IllegalArgumentException If the file cannot be found or read.
     */
    public List<Image> readData(String path) {
        List<Image> images = new ArrayList<>();

        try (BufferedReader dataReader = new BufferedReader(new FileReader(path))) {
            String line;

            while ((line = dataReader.readLine()) != null) {
                String[] lineItems = line.split(",");

                double[][] data = new double[rows][columns];
                int label = Integer.parseInt(lineItems[0]);  // The first element is the label

                int i = 1;

                for (int row = 0; row < rows; row++) {
                    for (int col = 0; col < columns; col++) {
                        try {
                            data[row][col] = Double.parseDouble(lineItems[i]);
                            i++;
                        } catch (NumberFormatException e) {
                            System.err.println("Error parsing integer from CSV at row " + row + ", col " + col + ": " + e.getMessage());
                            e.printStackTrace();
                        }
                    }
                }

                images.add(new Image(data, label));  // Add the parsed Image object to the list
            }

        } catch (Exception e) {
            throw new IllegalArgumentException("File not found or error reading file at path: " + path, e);
        }

        return images;
    }
}

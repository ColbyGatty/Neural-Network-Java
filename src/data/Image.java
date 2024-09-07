package data;

import java.io.Serializable;

public class Image implements Serializable {
    private static final long serialVersionUID = 1L;
    private double[][] data;  // 2D array to hold the pixel data for the image
    private int label;  // Label representing the class of the image (e.g., digit 0-9)

    /**
     * Constructor to initialize the Image object with data and label.
     *
     * @param data 2D array representing the pixel data of the image.
     * @param label Integer label representing the class of the image.
     */
    public Image(double[][] data, int label) {
        this.data = data;
        this.label = label;
    }

    /**
     * Gets the pixel data of the image.
     *
     * @return 2D array of pixel values.
     */
    public double[][] getData() {
        return data;
    }

    /**
     * Gets the label of the image.
     *
     * @return Integer label of the image.
     */
    public int getLabel() {
        return label;
    }
    //Data augmentation for translations left, right, up, and down. This will vastly increase the training and test set for the network
    public Image translateLeft(int pixels) {
        double[][] translatedData = new double[data.length][data[0].length];

        for (int row = 0; row < data.length; row++) {
            for (int col = 0; col < data[row].length; col++) {
                if (col + pixels < data[row].length) {
                    translatedData[row][col] = data[row][col + pixels];
                } else {
                    translatedData[row][col] = 0; // Fill in the new columns with zeros
                }
            }
        }

        return new Image(translatedData, label);
    }

    public Image translateRight(int pixels) {
        double[][] translatedData = new double[data.length][data[0].length];

        for (int row = 0; row < data.length; row++) {
            for (int col = data[row].length - 1; col >= 0; col--) {
                if (col - pixels >= 0) {
                    // Shift pixels to the right by the specified amount
                    translatedData[row][col] = data[row][col - pixels];
                } else {
                    // Fill new columns that moved in from the left with zeros
                    translatedData[row][col] = 0;
                }
            }
        }

        return new Image(translatedData, label);
    }


    public Image translateUp(int pixels) {
        double[][] translatedData = new double[data.length][data[0].length];

        for (int row = 0; row < data.length; row++) {
            for (int col = 0; col < data[row].length; col++) {
                if (row + pixels < data.length) {
                    translatedData[row][col] = data[row + pixels][col];
                } else {
                    translatedData[row][col] = 0; // Fill in the new rows with zeros
                }
            }
        }

        return new Image(translatedData, label);
    }

    public Image translateDown(int pixels) {
        double[][] translatedData = new double[data.length][data[0].length];

        for (int row = data.length - 1; row >= 0; row--) {
            for (int col = 0; col < data[row].length; col++) {
                if (row - pixels >= 0) {
                    translatedData[row][col] = data[row - pixels][col];
                } else {
                    translatedData[row][col] = 0; // Fill in the new rows with zeros
                }
            }
        }

        return new Image(translatedData, label);
    }

    public Image rotate(double angle) {
        int n = data.length;
        double[][] rotatedData = new double[n][n];

        int centerX = n / 2;
        int centerY = n / 2;

        double radians = Math.toRadians(angle);

        for (int row = 0; row < n; row++) {
            for (int col = 0; col < n; col++) {
                int x = row - centerX;
                int y = col - centerY;

                int newX = (int) (x * Math.cos(radians) - y * Math.sin(radians)) + centerX;
                int newY = (int) (x * Math.sin(radians) + y * Math.cos(radians)) + centerY;

                // Ensure newX and newY are within bounds
                if (newX >= 0 && newX < n && newY >= 0 && newY < n) {
                    rotatedData[newX][newY] = data[row][col];
                } else {
                    // Set out-of-bound pixels to 0 (or another default value)
                    rotatedData[row][col] = 0;
                }
            }
        }

        return new Image(rotatedData, label);
    }
    //end of data augmentation
    /**
     * Converts the image data and label to a string representation.
     *
     * @return String representation of the image.
     */
    @Override
    public String toString() {
        StringBuilder s = new StringBuilder(label + ", \n");

        try {
            for (int i = 0; i < data.length; i++) {
                for (int j = 0; j < data[0].length; j++) {
                    s.append(data[i][j]).append(", ");
                }
                s.append("\n");
            }
        } catch (Exception e) {
            System.err.println("Error converting image to string: " + e.getMessage());
            e.printStackTrace();
        }

        return s.toString();
    }
}

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

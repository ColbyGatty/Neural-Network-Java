package data;

public class MatrixUtility {

    /**
     * Performs element-wise addition of two matrices.
     *
     * @param a The first matrix.
     * @param b The second matrix.
     * @return A new matrix where each element is the sum of the corresponding elements in a and b.
     */
    public static double[][] add(double[][] a, double[][] b) {
        // Create a new matrix to store the result of the addition
        double[][] out = new double[a.length][a[0].length];

        // Iterate through each element of the matrices
        for (int i = 0; i < a.length; i++) {
            for (int j = 0; j < a[0].length; j++) {
                // Perform element-wise addition and store the result in the output matrix
                out[i][j] = a[i][j] + b[i][j];
            }
        }

        return out;  // Return the resulting matrix
    }

    /**
     * Performs element-wise addition of two vectors.
     *
     * @param a The first vector.
     * @param b The second vector.
     * @return A new vector where each element is the sum of the corresponding elements in a and b.
     */
    public static double[] add(double[] a, double[] b) {
        // Create a new vector to store the result of the addition
        double[] out = new double[a.length];

        // Iterate through each element of the vectors
        for (int i = 0; i < a.length; i++) {
            // Perform element-wise addition and store the result in the output vector
            out[i] = a[i] + b[i];
        }

        return out;  // Return the resulting vector
    }

    /**
     * Multiplies each element of a matrix by a scalar value.
     *
     * @param a The matrix to be multiplied.
     * @param scalar The scalar value to multiply each element by.
     * @return A new matrix where each element is the product of the corresponding element in a and the scalar.
     */
    public static double[][] multiply(double[][] a, double scalar) {
        // Create a new matrix to store the result of the multiplication
        double[][] out = new double[a.length][a[0].length];

        // Iterate through each element of the matrix
        for (int i = 0; i < a.length; i++) {
            for (int j = 0; j < a[0].length; j++) {
                // Multiply each element by the scalar and store the result in the output matrix
                out[i][j] = a[i][j] * scalar;
            }
        }

        return out;  // Return the resulting matrix
    }

    /**
     * Multiplies each element of a vector by a scalar value.
     *
     * @param a The vector to be multiplied.
     * @param scalar The scalar value to multiply each element by.
     * @return A new vector where each element is the product of the corresponding element in a and the scalar.
     */
    public static double[] multiply(double[] a, double scalar) {
        // Create a new vector to store the result of the multiplication
        double[] out = new double[a.length];

        // Iterate through each element of the vector
        for (int i = 0; i < a.length; i++) {
            // Multiply each element by the scalar and store the result in the output vector
            out[i] = a[i] * scalar;
        }

        return out;  // Return the resulting vector
    }

}

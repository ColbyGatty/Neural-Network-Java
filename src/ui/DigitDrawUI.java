package ui;

import javax.swing.*;
import java.awt.*;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.image.BufferedImage;

import network.Main;
import network.NeuralNetwork;
import data.Image;
import product.ModelLocator;

public class DigitDrawUI extends JFrame {
    private static final int GRID_SIZE = 28;
    private JPanel drawingPanel;
    private BufferedImage drawingImage;
    private NeuralNetwork network;

    public DigitDrawUI(NeuralNetwork network) {
        this.network = network;
        setTitle("Draw a number 0-9");
        setSize(400, 400);
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);

        drawingImage = new BufferedImage(GRID_SIZE, GRID_SIZE, BufferedImage.TYPE_BYTE_GRAY);
        drawingPanel = new JPanel() {
            @Override
            protected void paintComponent(Graphics g) {
                super.paintComponent(g);
                g.drawImage(drawingImage, 0, 0, getWidth(), getHeight(), null);
            }
        };

        JButton clearButton = new JButton("Clear");
        clearButton.addActionListener(e -> clearDrawing());

        JButton submitButton = new JButton("Submit");
        submitButton.addActionListener(e -> submitDrawing());

        JPanel controlPanel = new JPanel();
        controlPanel.add(clearButton);
        controlPanel.add(submitButton);

        add(drawingPanel, BorderLayout.CENTER);
        add(controlPanel, BorderLayout.SOUTH);

        drawingPanel.addMouseMotionListener(new MouseAdapter() {
            public void mouseDragged(MouseEvent evt) {
                draw(evt.getX(), evt.getY());
            }
        });
    }

    private void clearDrawing() {
        Graphics2D g2d = drawingImage.createGraphics();
        g2d.setColor(Color.BLACK);
        g2d.fillRect(0, 0, drawingImage.getWidth(), drawingImage.getHeight());
        g2d.dispose();
        repaint();
    }

    private void draw(int x, int y) {
        int cellWidth = drawingPanel.getWidth() / GRID_SIZE;
        int cellHeight = drawingPanel.getHeight() / GRID_SIZE;

        int pixelX = x / cellWidth;
        int pixelY = y / cellHeight;

        Graphics2D g2d = drawingImage.createGraphics();

        // Set the drawing color to pure white
        g2d.setColor(Color.WHITE);
        g2d.fillRect(pixelX, pixelY, 1, 1);

        // Apply a more intense feathered effect with a thinner line
        applyFeatherEffect(g2d, pixelX, pixelY);

        g2d.dispose();
        repaint();
    }

    private void applyFeatherEffect(Graphics2D g2d, int x, int y) {
        float[][] featherKernel = {
                {0.05f, 0.15f, 0.05f},
                {0.15f, 0.5f, 0.15f},
                {0.05f, 0.15f, 0.05f}
        }; // A more intense kernel to create a sharper, more focused feathering effect

        for (int i = -1; i <= 1; i++) {
            for (int j = -1; j <= 1; j++) {
                int newX = x + i;
                int newY = y + j;

                // Ensure the coordinates are within the image bounds
                if (newX >= 0 && newX < GRID_SIZE && newY >= 0 && newY < GRID_SIZE) {
                    int currentColor = drawingImage.getRGB(newX, newY);
                    int alpha = (int) (featherKernel[i + 1][j + 1] * 255);

                    // Blend the current pixel's color with white based on the alpha value
                    int existingRed = (currentColor >> 16) & 0xFF;
                    int existingGreen = (currentColor >> 8) & 0xFF;
                    int existingBlue = currentColor & 0xFF;

                    int newRed = Math.min(255, existingRed + alpha);
                    int newGreen = Math.min(255, existingGreen + alpha);
                    int newBlue = Math.min(255, existingBlue + alpha);

                    int blendedColor = (alpha << 24) | (newRed << 16) | (newGreen << 8) | newBlue;

                    drawingImage.setRGB(newX, newY, blendedColor);
                }
            }
        }
    }



    private void submitDrawing() {
        double[][] data = new double[GRID_SIZE][GRID_SIZE];

        // Correct the coordinate mapping to ensure the image is captured as drawn
        for (int i = 0; i < GRID_SIZE; i++) {
            for (int j = 0; j < GRID_SIZE; j++) {
                int pixel = drawingImage.getRGB(j, i); // Notice the switch of i and j
                data[i][j] = (pixel & 0xFF) / 255.0;  // Convert to grayscale (0.0 to 1.0)
            }
        }

        // Print the 2D array to inspect the input data
//        printInputData(data);

        // Create an Image object with the 2D array
        Image userImage = new Image(data, -1); // Label is -1 because it's unknown at this point

        // Pass the data to the neural network to get the prediction
        int prediction = network.guess(userImage);

        // Display the prediction
        JOptionPane.showMessageDialog(this, "This looks like a " + prediction + " to me!");
    }

    // Method to print the 2D array representing the image data
//    private void printInputData(double[][] data) {
//        System.out.println("Input Data:");
//        for (int i = 0; i < data.length; i++) {
//            for (int j = 0; j < data[i].length; j++) {
//                // Print each value with a space, formatted to 1 decimal place
//                System.out.printf("%.1f ", data[i][j]);
//            }
//            System.out.println(); // New line after each row
//        }
//        System.out.println(); // Extra line for better readability
//    }

    public static void main(String[] args) {
        String path = ModelLocator.findLatestModelPath("out");
        if (path == null) {
            JOptionPane.showMessageDialog(null, "No trained model found in out/; train the network first.");
            return;
        }
        NeuralNetwork network = Main.loadNetwork(path);
        if (network == null) {
            JOptionPane.showMessageDialog(null, "Failed to load model at " + path);
            return;
        }
        SwingUtilities.invokeLater(() -> {
            DigitDrawUI ui = new DigitDrawUI(network);
            ui.setVisible(true);
        });
    }
}

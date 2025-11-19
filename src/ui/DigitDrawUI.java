package ui;

import data.Image;
import network.Main;
import network.NeuralNetwork;
import product.ModelLocator;

import javax.swing.*;
import java.awt.*;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.image.BufferedImage;

/**
 * Reusable drawing component that lets a user sketch a digit, predicts via the loaded network,
 * and notifies a listener about the captured sample.
 */
public class DigitDrawUI extends JPanel {
    private static final int GRID_SIZE = 28;
    private final JPanel drawingPanel;
    private final BufferedImage drawingImage = new BufferedImage(GRID_SIZE, GRID_SIZE, BufferedImage.TYPE_BYTE_GRAY);
    private NeuralNetwork network;
    private PredictionListener predictionListener;

    public DigitDrawUI(NeuralNetwork network) {
        this.network = network;
        setLayout(new BorderLayout(4, 4));
        setPreferredSize(new Dimension(320, 320));
        setMinimumSize(new Dimension(320, 320));
        setMaximumSize(new Dimension(340, 360));
        setBorder(BorderFactory.createTitledBorder("Digit Pad"));

        drawingPanel = new JPanel() {
            @Override
            protected void paintComponent(Graphics g) {
                super.paintComponent(g);
                g.drawImage(drawingImage, 0, 0, getWidth(), getHeight(), null);
            }
        };
        drawingPanel.setBackground(Color.BLACK);
        drawingPanel.setPreferredSize(new Dimension(320, 280));
        drawingPanel.addMouseMotionListener(new MouseAdapter() {
            @Override
            public void mouseDragged(MouseEvent e) {
                draw(e.getX(), e.getY());
            }
        });
        drawingPanel.addMouseListener(new MouseAdapter() {
            @Override
            public void mousePressed(MouseEvent e) {
                draw(e.getX(), e.getY());
            }
        });

        JButton clearButton = new JButton("Clear");
        clearButton.addActionListener(e -> clearDrawing());
        JButton submitButton = new JButton("Submit");
        submitButton.addActionListener(e -> submitDrawing());

        JPanel controlPanel = new JPanel(new FlowLayout(FlowLayout.CENTER, 6, 4));
        controlPanel.setBackground(getBackground());
        controlPanel.add(clearButton);
        controlPanel.add(submitButton);

        add(drawingPanel, BorderLayout.CENTER);
        add(controlPanel, BorderLayout.SOUTH);
        clearDrawing();
    }

    public void setNetwork(NeuralNetwork network) {
        this.network = network;
    }

    public void setPredictionListener(PredictionListener listener) {
        this.predictionListener = listener;
    }

    public void resetDrawing() {
        clearDrawing();
    }

    private void clearDrawing() {
        Graphics2D g2d = drawingImage.createGraphics();
        g2d.setColor(Color.BLACK);
        g2d.fillRect(0, 0, GRID_SIZE, GRID_SIZE);
        g2d.dispose();
        repaint();
    }

    private void draw(int x, int y) {
        int cellWidth = Math.max(1, drawingPanel.getWidth() / GRID_SIZE);
        int cellHeight = Math.max(1, drawingPanel.getHeight() / GRID_SIZE);

        int pixelX = Math.min(GRID_SIZE - 1, Math.max(0, x / cellWidth));
        int pixelY = Math.min(GRID_SIZE - 1, Math.max(0, y / cellHeight));

        Graphics2D g2d = drawingImage.createGraphics();
        g2d.setColor(Color.WHITE);
        g2d.fillRect(pixelX, pixelY, 1, 1);
        applyFeatherEffect(g2d, pixelX, pixelY);
        g2d.dispose();
        repaint();
    }

    private void applyFeatherEffect(Graphics2D g2d, int x, int y) {
        float[][] featherKernel = {
                {0.05f, 0.15f, 0.05f},
                {0.15f, 0.5f, 0.15f},
                {0.05f, 0.15f, 0.05f}
        };

        for (int dx = -1; dx <= 1; dx++) {
            for (int dy = -1; dy <= 1; dy++) {
                int newX = x + dx;
                int newY = y + dy;
                if (newX >= 0 && newX < GRID_SIZE && newY >= 0 && newY < GRID_SIZE) {
                    int existing = drawingImage.getRGB(newX, newY) & 0xFF;
                    int alpha = (int) (featherKernel[dx + 1][dy + 1] * 255);
                    int blended = Math.min(255, existing + alpha);
                    int color = (0xFF << 24) | (blended << 16) | (blended << 8) | blended;
                    drawingImage.setRGB(newX, newY, color);
                }
            }
        }
    }

    private void submitDrawing() {
        if (network == null) {
            JOptionPane.showMessageDialog(this, "No trained model loaded yet.");
            return;
        }
        double[][] data = new double[GRID_SIZE][GRID_SIZE];
        for (int row = 0; row < GRID_SIZE; row++) {
            for (int col = 0; col < GRID_SIZE; col++) {
                int pixel = drawingImage.getRGB(col, row) & 0xFF;
                data[row][col] = pixel / 255.0;
            }
        }
        Image sample = new Image(data, -1);
        int prediction = network.guess(sample);
        JOptionPane.showMessageDialog(this, "This looks like a " + prediction + ".");

        if (predictionListener != null) {
            predictionListener.onPrediction(sample, duplicateDrawing(), prediction);
        }
    }

    private BufferedImage duplicateDrawing() {
        BufferedImage copy = new BufferedImage(GRID_SIZE, GRID_SIZE, BufferedImage.TYPE_BYTE_GRAY);
        Graphics2D g2d = copy.createGraphics();
        g2d.drawImage(drawingImage, 0, 0, null);
        g2d.dispose();
        return copy;
    }

    public interface PredictionListener {
        void onPrediction(Image sample, BufferedImage preview, int prediction);
    }

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
            JFrame frame = new JFrame("Draw a number 0-9");
            frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
            frame.add(new DigitDrawUI(network));
            frame.pack();
            frame.setLocationRelativeTo(null);
            frame.setVisible(true);
        });
    }
}

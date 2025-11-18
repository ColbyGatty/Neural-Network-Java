package product;

import data.DataReader;
import data.Image;
import network.NeuralNetwork;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.image.BufferedImage;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.awt.GridBagLayout;
import java.awt.GridBagConstraints;
import java.awt.Insets;

/**
 * Simple dashboard that surfaces data exploration, decision support, query editing, monitoring,
 * and three visualizations that allow the monitoring and use of my neural network.
 */
public class ProductDashboard extends JFrame {
    private final MonitoringTool monitor = new MonitoringTool("Digit Recognizer Dashboard");
    private final ModelEvaluator evaluator = new ModelEvaluator(monitor);
    private static final Dimension DRAW_PANEL_DIMENSION = new Dimension(280, 280);
    private static final Dimension DASHBOARD_DIMENSION = new Dimension(1050, 800);
    private static final Dimension CHART_PANEL_DIMENSION = new Dimension(320, 300);
    private static final int CHART_MARGIN = 16;

    private final JLabel summaryLabel = new JLabel("Loading data...");
    private final JLabel decisionSupportLabel = new JLabel("Decision guidance will appear here.");
    private final JLabel statusLabel = new JLabel("Status pending...");
    private final DefaultListModel<String> logModel = new DefaultListModel<>();
    private final JList<String> logList = new JList<>(logModel);
    private final JTextField queryField = new JTextField(3);
    private final JLabel queryResultLabel = new JLabel("Label query results show here.");

    private final LabelDistributionPanel distributionPanel = new LabelDistributionPanel();
    private final AccuracyTrendPanel accuracyPanel = new AccuracyTrendPanel();
    private final DrawingWidget drawingWidget = new DrawingWidget();
    private final CorrectionPanel correctionPanel = new CorrectionPanel();
    private final UserCorrectionStore correctionStore = new UserCorrectionStore();

    private List<Image> trainingData = Collections.emptyList();
    private List<Image> testData = Collections.emptyList();
    private NeuralNetwork network;

    public ProductDashboard() {
        super("Digit Recognition Product Dashboard");
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setSize(DASHBOARD_DIMENSION);
        setMinimumSize(DASHBOARD_DIMENSION);
        setMaximumSize(DASHBOARD_DIMENSION);
        setResizable(false);
        setLayout(new BorderLayout(12, 12));

        loadResources();
        buildLayout();
        refreshMetrics();
        monitor.record("Dashboard frame opened with size " + getWidth() + "x" + getHeight());
    }

    private void loadResources() {
        trainingData = safelyLoad("Data/mnist_train.csv");
        testData = safelyLoad("Data/mnist_test.csv");
        monitor.record("Training data loaded: " + trainingData.size() + " samples.");
        monitor.record("Test data loaded: " + testData.size() + " samples.");

        String latestModelPath = ModelLocator.findLatestModelPath("out");
        if (latestModelPath != null) {
            network = evaluator.loadSavedNetwork(latestModelPath);
            monitor.record("Loaded model: " + latestModelPath);
            if (network == null) {
                monitor.record("Model file exists but failed to load.");
            }
        } else {
            monitor.record("No serialized model detected; accuracy checks will be skipped.");
        }
    }

    private void buildLayout() {
        JPanel infoPanel = new JPanel(new BorderLayout(4, 4));
        infoPanel.setBorder(BorderFactory.createTitledBorder("Data Overview"));

        summaryLabel.setFont(summaryLabel.getFont().deriveFont(Font.BOLD, 14f));
        infoPanel.add(summaryLabel, BorderLayout.NORTH);

        JTextArea guidanceArea = new JTextArea(2, 6);
        guidanceArea.setEditable(false);
        guidanceArea.setLineWrap(true);
        guidanceArea.setWrapStyleWord(true);
        guidanceArea.setText("This dashboard surfaces dataset statistics, decision guidance, and monitoring insights.");
        guidanceArea.setBackground(infoPanel.getBackground());
        infoPanel.add(guidanceArea, BorderLayout.CENTER);

        JPanel guidancePanel = new JPanel(new BorderLayout(2, 2));
        guidancePanel.add(decisionSupportLabel, BorderLayout.CENTER);
        guidancePanel.add(statusLabel, BorderLayout.SOUTH);
        infoPanel.add(guidancePanel, BorderLayout.SOUTH);

        add(infoPanel, BorderLayout.NORTH);

        JPanel charts = new JPanel(new GridBagLayout());
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.fill = GridBagConstraints.BOTH;
        gbc.gridy = 0;
        gbc.insets = new Insets(0, 0, 0, 12);
        gbc.weighty = 1.0;

        gbc.gridx = 0;
        gbc.weightx = 0.45;
        charts.add(distributionPanel, gbc);

        gbc.gridx = 1;
        charts.add(accuracyPanel, gbc);

        gbc.gridx = 2;
        gbc.insets = new Insets(0, 0, 0, 0);
        gbc.weightx = 0;
        charts.add(drawingWidget, gbc);

        JPanel centerWrapper = new JPanel(new BorderLayout(0, 8));
        centerWrapper.add(charts, BorderLayout.CENTER);
        centerWrapper.add(buildCorrectionRow(), BorderLayout.SOUTH);
        add(centerWrapper, BorderLayout.CENTER);

        JPanel controlPanel = new JPanel(new BorderLayout(6, 6));
        controlPanel.add(buildQueryPanel(), BorderLayout.NORTH);
        controlPanel.add(buildMonitoringPanel(), BorderLayout.CENTER);
        controlPanel.add(buildActionPanel(), BorderLayout.SOUTH);
        add(controlPanel, BorderLayout.SOUTH);
    }

    private JPanel buildCorrectionRow() {
        JPanel row = new JPanel(new FlowLayout(FlowLayout.RIGHT, 6, 0));
        row.add(correctionPanel);
        return row;
    }

    private JPanel buildQueryPanel() {
        JPanel panel = new JPanel(new FlowLayout(FlowLayout.LEFT, 8, 4));
        panel.setBorder(BorderFactory.createTitledBorder("Interactive Queries"));
        panel.add(new JLabel("Enter digit (0-9):"));
        panel.add(queryField);
        JButton queryButton = new JButton("Count");
        queryButton.addActionListener(this::handleQuery);
        panel.add(queryButton);
        panel.add(queryResultLabel);
        return panel;
    }

    private JPanel buildMonitoringPanel() {
        JPanel panel = new JPanel(new BorderLayout(6, 4));
        panel.setBorder(BorderFactory.createTitledBorder("Monitoring & Maintenance"));
        logList.setFont(new Font(Font.MONOSPACED, Font.PLAIN, 12));
        panel.add(new JScrollPane(logList), BorderLayout.CENTER);
        return panel;
    }

    private JPanel buildActionPanel() {
        JPanel panel = new JPanel(new FlowLayout(FlowLayout.RIGHT, 8, 4));
        panel.setBorder(BorderFactory.createTitledBorder("Controls"));
        JButton refreshButton = new JButton("Refresh Metrics");
        refreshButton.addActionListener(e -> refreshMetrics());
        JButton healthButton = new JButton("Health Check");
        healthButton.addActionListener(e -> {
            monitor.record("Health check requested.");
            statusLabel.setText(monitor.getStatusSummary());
            refreshLogView();
        });
        panel.add(healthButton);
        panel.add(refreshButton);
        return panel;
    }

    private void handleQuery(ActionEvent event) {
        String text = queryField.getText();
        try {
            int label = Integer.parseInt(text.trim());
            int count = DataWrangler.countForLabel(trainingData, label);
            queryResultLabel.setText("Found " + count + " samples labeled " + label);
            monitor.record("Interactive query for label " + label + ": " + count + " matches.");
            refreshLogView();
        } catch (NumberFormatException e) {
            queryResultLabel.setText("Enter a digit between 0 and 9.");
        }
    }

    private void refreshMetrics() {
        List<Image> cleaned = DataWrangler.cleanSparsity(trainingData);
        String summary = DataWrangler.describeDataset(cleaned);
        summaryLabel.setText(summary);
        monitor.record("Generated descriptive summary.");

        float accuracy = evaluator.evaluateAccuracy(network, testData);
        float baseline = Math.max(0.4f, accuracy - 0.03f);
        double predicted = evaluator.predictTrainingOutcome(accuracy, baseline);
        decisionSupportLabel.setText(String.format("%s Next epoch: %.1f%%", evaluator.generateDecisionSupport(accuracy, baseline), predicted * 100));

        distributionPanel.updateDistribution(DataWrangler.getLabelDistribution(cleaned));
        accuracyPanel.updateTrend(evaluator.buildAccuracyTrend(baseline, accuracy));

        statusLabel.setText(monitor.getStatusSummary());
        refreshLogView();
    }

    private void refreshLogView() {
        logModel.clear();
        for (String entry : monitor.recentLogs()) {
            logModel.addElement(entry);
        }
    }

    private List<Image> safelyLoad(String path) {
        try {
            return new DataReader().readData(path);
        } catch (IllegalArgumentException e) {
            monitor.record("Failed to load " + path + ": " + e.getMessage());
            return Collections.emptyList();
        }
    }

    /**
     * Launches the dashboard if the security guard is satisfied.
     */
    public static void main(String[] args) {
        SecurityGuard guard = new SecurityGuard();
        boolean authorized = guard.authorizeFromEnvironment();
        if (!authorized) {
            String candidate = JOptionPane.showInputDialog("Enter dashboard token (see README).");
            authorized = guard.authorize(candidate);
        }
        if (!authorized) {
            JOptionPane.showMessageDialog(null, "Access denied. Valid token required.");
            return;
        }

        SwingUtilities.invokeLater(() -> {
            ProductDashboard dashboard = new ProductDashboard();
            dashboard.setVisible(true);
        });
    }

    private static class LabelDistributionPanel extends JPanel {
        private Map<Integer, Integer> distribution = Collections.emptyMap();

        public void updateDistribution(Map<Integer, Integer> distribution) {
            this.distribution = distribution;
            repaint();
        }

        LabelDistributionPanel() {
            setPreferredSize(CHART_PANEL_DIMENSION);
            setMinimumSize(CHART_PANEL_DIMENSION);
            setMaximumSize(CHART_PANEL_DIMENSION);
        }

        @Override
        protected void paintComponent(Graphics g) {
            super.paintComponent(g);
            setBorder(BorderFactory.createTitledBorder("Label Distribution (Bar Chart)"));
            if (distribution.isEmpty()) {
                return;
            }
            Insets insets = getInsets();
            int availableWidth = Math.max(0, getWidth() - CHART_MARGIN - insets.left - insets.right);
            int topOffset = CHART_MARGIN / 2 + insets.top + 10;
            int availableHeight = Math.max(0, getHeight() - topOffset - CHART_MARGIN / 2 - insets.bottom - 20);
            int max = distribution.values().stream().max(Integer::compareTo).orElse(1);
            int barCount = 10;
            int padding = 4;
            int barWidth = Math.max(5, (availableWidth - (barCount - 1) * padding) / barCount);
            int x = CHART_MARGIN / 2;
            for (int label = 0; label <= 9; label++) {
                int value = distribution.getOrDefault(label, 0);
                int barHeight = max == 0 ? 0 : (int) ((double) value / max * availableHeight);
                g.setColor(new Color(70, 130, 180));
                int baseY = topOffset + availableHeight;
                g.fillRect(x, baseY - barHeight, barWidth, barHeight);
                g.setColor(Color.BLACK);
                g.drawString(String.valueOf(label), x + (barWidth / 3), baseY + 15);
                x += barWidth + padding;
            }
        }
    }

    private static class AccuracyTrendPanel extends JPanel {
        private List<Double> trend = Collections.emptyList();

        public void updateTrend(List<Double> trend) {
            this.trend = trend;
            repaint();
        }

        AccuracyTrendPanel() {
            setPreferredSize(CHART_PANEL_DIMENSION);
            setMinimumSize(CHART_PANEL_DIMENSION);
            setMaximumSize(CHART_PANEL_DIMENSION);
        }

        @Override
        protected void paintComponent(Graphics g) {
            super.paintComponent(g);
            setBorder(BorderFactory.createTitledBorder("Accuracy Trend (Line Chart)"));
            if (trend.isEmpty()) {
                return;
            }
            Insets insets = getInsets();
            int margin = CHART_MARGIN;
            int titleSpace = 18;
            int availableWidth = Math.max(0, getWidth() - margin * 2 - insets.left - insets.right);
            int availableHeight = Math.max(0, getHeight() - margin * 2 - insets.top - insets.bottom - titleSpace);
            int xStep = availableWidth / Math.max(1, trend.size() - 1);
            int maxValue = trend.stream().mapToInt(Double::intValue).max().orElse(100);
            int minValue = trend.stream().mapToInt(Double::intValue).min().orElse(0);
            int prevX = margin + insets.left;
            int prevY = margin + insets.top + titleSpace + availableHeight - scaleValue(trend.get(0), availableHeight, minValue, maxValue);

            for (int idx = 1; idx < trend.size(); idx++) {
                int x = margin + insets.left + idx * xStep;
                int y = margin + insets.top + titleSpace + availableHeight - scaleValue(trend.get(idx), availableHeight, minValue, maxValue);
                g.setColor(Color.RED);
                g.drawLine(prevX, prevY, x, y);
                g.fillOval(x - 3, y - 3, 6, 6);
                prevX = x;
                prevY = y;
            }
        }

        private int scaleValue(double value, int height, int minValue, int maxValue) {
            if (maxValue == minValue) {
                return height / 2;
            }
            return (int) ((value - minValue) / (double) (maxValue - minValue) * height);
        }
    }

    private class DrawingWidget extends JPanel {
        private static final int GRID_SIZE = 28;
        private final BufferedImage canvas = new BufferedImage(GRID_SIZE, GRID_SIZE, BufferedImage.TYPE_BYTE_GRAY);
        private final JLabel predictionLabel = new JLabel("Prediction: ");
        private final JButton clearButton = new JButton("Clear");
        private final JButton submitButton = new JButton("Submit");

        DrawingWidget() {
            setLayout(new BorderLayout(4, 4));
            setBorder(BorderFactory.createTitledBorder("Interactive Draw Panel"));
            JPanel drawingArea = new JPanel() {
                @Override
                protected void paintComponent(Graphics g) {
                    super.paintComponent(g);
                    g.drawImage(canvas, 0, 0, getWidth(), getHeight(), null);
                }
            };
            drawingArea.setBackground(Color.BLACK);
            drawingArea.addMouseMotionListener(new MouseAdapter() {
                @Override
                public void mouseDragged(MouseEvent e) {
                    drawPoint(e.getX(), e.getY(), drawingArea);
                }
            });
            drawingArea.addMouseListener(new MouseAdapter() {
                @Override
                public void mousePressed(MouseEvent e) {
                    drawPoint(e.getX(), e.getY(), drawingArea);
                }
            });

            add(drawingArea, BorderLayout.CENTER);

            JPanel controls = new JPanel(new FlowLayout(FlowLayout.CENTER, 8, 4));
            controls.add(clearButton);
            controls.add(submitButton);
            controls.add(predictionLabel);
            add(controls, BorderLayout.SOUTH);

            clearButton.addActionListener(e -> {
                clearCanvas();
                predictionLabel.setText("Prediction: ");
            });
            submitButton.addActionListener(e -> submitDrawing());

            clearCanvas();
            setPreferredSize(DRAW_PANEL_DIMENSION);
            setMinimumSize(DRAW_PANEL_DIMENSION);
            setMaximumSize(DRAW_PANEL_DIMENSION);
        }

        private void drawPoint(int x, int y, Component reference) {
            int cellWidth = Math.max(1, reference.getWidth() / GRID_SIZE);
            int cellHeight = Math.max(1, reference.getHeight() / GRID_SIZE);
            int pixelX = Math.min(GRID_SIZE - 1, Math.max(0, x / cellWidth));
            int pixelY = Math.min(GRID_SIZE - 1, Math.max(0, y / cellHeight));

            Graphics2D g2d = canvas.createGraphics();
            g2d.setColor(Color.WHITE);
            g2d.fillRect(pixelX, pixelY, 1, 1);
            applyFeatherEffect(g2d, pixelX, pixelY);
            g2d.dispose();
            repaint();
        }

        private void clearCanvas() {
            Graphics2D g2d = canvas.createGraphics();
            g2d.setColor(Color.BLACK);
            g2d.fillRect(0, 0, GRID_SIZE, GRID_SIZE);
            g2d.dispose();
            repaint();
        }

        private void submitDrawing() {
            if (network == null) {
                predictionLabel.setText("Prediction: model not loaded");
                monitor.record("Draw widget submit skipped: no model.");
                return;
            }
            int prediction = network.guess(new Image(captureCanvas(), -1));
            predictionLabel.setText("Prediction: " + prediction);
            monitor.record("Draw widget predicted: " + prediction);
        }

        private double[][] captureCanvas() {
            double[][] data = new double[GRID_SIZE][GRID_SIZE];
            for (int row = 0; row < GRID_SIZE; row++) {
                for (int col = 0; col < GRID_SIZE; col++) {
                    int pixel = canvas.getRGB(col, row) & 0xFF;
                    data[row][col] = pixel / 255.0;
                }
            }
            return data;
        }

        public double[][] snapshotCanvasData() {
            return captureCanvas();
        }

        private void applyFeatherEffect(Graphics2D g2d, int x, int y) {
            float[][] kernel = {
                    {0.05f, 0.15f, 0.05f},
                    {0.15f, 0.5f, 0.15f},
                    {0.05f, 0.15f, 0.05f}
            };
            for (int dx = -1; dx <= 1; dx++) {
                for (int dy = -1; dy <= 1; dy++) {
                    int nx = x + dx;
                    int ny = y + dy;
                    if (nx >= 0 && nx < GRID_SIZE && ny >= 0 && ny < GRID_SIZE) {
                        int alpha = (int) (kernel[dx + 1][dy + 1] * 255);
                        int existing = canvas.getRGB(nx, ny) & 0xFF;
                        int blended = Math.min(255, existing + alpha);
                        int color = (255 << 24) | (blended << 16) | (blended << 8) | blended;
                        canvas.setRGB(nx, ny, color);
                    }
                }
            }
        }
    }

    private class CorrectionPanel extends JPanel {
        private final JTextField labelField = new JTextField(2);
        private final JButton saveButton = new JButton("Save Correction");
        private final JLabel statusLabel = new JLabel("Type the true digit and save to retrain.");

        CorrectionPanel() {
            setBorder(BorderFactory.createTitledBorder("Correction Feedback"));
            setLayout(new BorderLayout(4, 4));

            JPanel inputs = new JPanel(new FlowLayout(FlowLayout.LEFT, 6, 4));
            inputs.add(new JLabel("True label:"));
            labelField.setToolTipText("Enter 0-9");
            inputs.add(labelField);
            inputs.add(saveButton);
            add(inputs, BorderLayout.CENTER);
            statusLabel.setFont(statusLabel.getFont().deriveFont(Font.PLAIN, 12f));
            add(statusLabel, BorderLayout.SOUTH);

            saveButton.addActionListener(e -> saveCorrection());
        }

        private void saveCorrection() {
            if (drawingWidget == null) {
                statusLabel.setText("Drawing panel unavailable.");
                return;
            }
            String text = labelField.getText().trim();
            if (!text.matches("[0-9]")) {
                statusLabel.setText("Enter a digit between 0 and 9.");
                return;
            }

            int label = Integer.parseInt(text);
            Image sample = new Image(drawingWidget.snapshotCanvasData(), label);
            correctionStore.appendCorrection(sample);
            statusLabel.setText("Saved correction for label " + label + ".");
            monitor.record("Saved correction for label " + label);
            labelField.setText("");
        }
    }
}

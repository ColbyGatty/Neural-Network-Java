package product;

import data.DataReader;
import data.Image;
import network.NeuralNetwork;
import ui.DigitDrawUI;

import javax.imageio.ImageIO;
import javax.swing.*;
import javax.swing.filechooser.FileNameExtensionFilter;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.image.BufferedImage;
import java.io.File;
import java.util.Collections;
import java.util.List;
import java.util.Map;
/**
 * Simple dashboard that surfaces data exploration, decision support, query editing, monitoring,
 * and three visualizations that allow the monitoring and use of my neural network.
 */
public class ProductDashboard extends JFrame {
    private final MonitoringTool monitor = new MonitoringTool("Digit Recognizer Dashboard");
    private final ModelEvaluator evaluator = new ModelEvaluator(monitor);
    private static final Dimension DASHBOARD_DIMENSION = new Dimension(1050, 800);
    private static final Dimension CHART_PANEL_DIMENSION = new Dimension(320, 300);
    private static final int CHART_FIXED_HEIGHT = 300;
    private static final int CHART_MARGIN = 16;
    private static final double CONTRAST_THRESHOLD = 0.35;
    private static final String LOGIN_USERNAME = "admin";
    private static final String LOGIN_PASSWORD = "wgucapstone";

    private final JLabel summaryLabel = new JLabel("Loading data...");
    private final JLabel decisionSupportLabel = new JLabel("Decision guidance will appear here.");
    private final JLabel statusLabel = new JLabel("Status pending...");
    private final DefaultListModel<String> logModel = new DefaultListModel<>();
    private final JList<String> logList = new JList<>(logModel);
    private final JTextField queryField = new JTextField(3);
    private final JLabel queryResultLabel = new JLabel("Label query results show here.");

    private final LabelDistributionPanel distributionPanel = new LabelDistributionPanel();
    private final ModelExplanationPanel explanationPanel = new ModelExplanationPanel();
    private final ConfusionMatrixPanel confusionPanel = new ConfusionMatrixPanel();
    private DigitDrawUI digitDrawUI;
    private final CorrectionPanel correctionPanel = new CorrectionPanel();
    private final UploadPanel uploadPanel = new UploadPanel();
    private final UserCorrectionStore correctionStore = new UserCorrectionStore();

    private List<Image> trainingData = Collections.emptyList();
    private List<Image> testData = Collections.emptyList();
    private NeuralNetwork network;

    public ProductDashboard() {
        super("Digit Recognition Product Dashboard");
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setSize(DASHBOARD_DIMENSION);
        setMinimumSize(new Dimension(800, 600));
        setResizable(true);
        setLayout(new BorderLayout(12, 12));

        loadResources();
        JPanel layout = buildLayout();
        refreshMetrics();
        monitor.record("Dashboard frame opened with size " + getWidth() + "x" + getHeight());
        add(layout, BorderLayout.CENTER);
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

    private JPanel buildLayout() {
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

        JPanel scrollContent = new JPanel(new BorderLayout(12, 12));
        JPanel mainPanel = new JPanel(new BorderLayout(12, 12));
        mainPanel.add(infoPanel, BorderLayout.NORTH);

        digitDrawUI = new DigitDrawUI(network);
        digitDrawUI.setPredictionListener((sample, preview, prediction) -> {
            correctionPanel.presentCandidate(new SampleResult(preview, sample), String.valueOf(prediction));
            monitor.record("Draw digit UI predicted: " + prediction);
        });

        JPanel topRow = new JPanel(new GridLayout(1, 2, 12, 12));
        topRow.add(distributionPanel);
        topRow.add(confusionPanel);

        JPanel bottomRowCharts = new JPanel(new BorderLayout(12, 0));
        bottomRowCharts.add(explanationPanel, BorderLayout.CENTER);
        JPanel padWrapper = new JPanel(new FlowLayout(FlowLayout.CENTER, 0, 0));
        padWrapper.add(digitDrawUI);
        bottomRowCharts.add(padWrapper, BorderLayout.EAST);

        JPanel centerWrapper = new JPanel();
        centerWrapper.setLayout(new BoxLayout(centerWrapper, BoxLayout.Y_AXIS));
        centerWrapper.setBorder(BorderFactory.createEmptyBorder(6, 6, 6, 6));

        centerWrapper.add(topRow);
        centerWrapper.add(Box.createRigidArea(new Dimension(0, 12)));
        centerWrapper.add(bottomRowCharts);
        centerWrapper.add(Box.createRigidArea(new Dimension(0, 12)));

        JPanel bottomRow = new JPanel(new GridLayout(1, 2, 12, 0));
        bottomRow.add(uploadPanel);
        bottomRow.add(correctionPanel);
        centerWrapper.add(bottomRow);
        scrollContent.add(centerWrapper, BorderLayout.CENTER);
        scrollContent.setPreferredSize(new Dimension(1100, 900));

        JScrollPane scrollPane = new JScrollPane(scrollContent);
        scrollPane.setBorder(null);
        scrollPane.setHorizontalScrollBarPolicy(ScrollPaneConstants.HORIZONTAL_SCROLLBAR_AS_NEEDED);
        scrollPane.setVerticalScrollBarPolicy(ScrollPaneConstants.VERTICAL_SCROLLBAR_AS_NEEDED);
        scrollPane.getVerticalScrollBar().setUnitIncrement(16);

        mainPanel.add(scrollPane, BorderLayout.CENTER);

        JPanel controlPanel = new JPanel(new BorderLayout(6, 6));
        controlPanel.add(buildQueryPanel(), BorderLayout.NORTH);
        controlPanel.add(buildMonitoringPanel(), BorderLayout.CENTER);
        controlPanel.add(buildActionPanel(), BorderLayout.SOUTH);
        mainPanel.add(controlPanel, BorderLayout.SOUTH);

        return mainPanel;
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
        decisionSupportLabel.setText(String.format("%s Predicted Next Epoch: %.1f%%", evaluator.generateDecisionSupport(accuracy, baseline), predicted * 100));

        distributionPanel.updateDistribution(DataWrangler.getLabelDistribution(cleaned));
        explanationPanel.updateStructure(network);
        confusionPanel.updateMatrix(evaluator.buildConfusionMatrix(network, testData, 10));
        if (digitDrawUI != null) {
            digitDrawUI.setNetwork(network);
        }

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
     * Launches the dashboard after the user passes the login dialog.
     */
    public static void main(String[] args) {

        SwingUtilities.invokeLater(() -> {
            if (!showLoginDialog()) {
                return;
            }
            ProductDashboard dashboard = new ProductDashboard();
            dashboard.setVisible(true);
        });
    }

    private static boolean showLoginDialog() {
        JTextField usernameField = new JTextField(12);
        JPasswordField passwordField = new JPasswordField(12);
        JPanel loginPanel = new JPanel(new GridLayout(2, 2, 6, 6));
        loginPanel.add(new JLabel("Username:"));
        loginPanel.add(usernameField);
        loginPanel.add(new JLabel("Password:"));
        loginPanel.add(passwordField);

        while (true) {
            int option = JOptionPane.showConfirmDialog(null, loginPanel, "Login", JOptionPane.OK_CANCEL_OPTION, JOptionPane.PLAIN_MESSAGE);
            if (option != JOptionPane.OK_OPTION) {
                return false;
            }
            String username = usernameField.getText().trim();
            String password = new String(passwordField.getPassword());
            if (LOGIN_USERNAME.equals(username) && LOGIN_PASSWORD.equals(password)) {
                return true;
            }
            JOptionPane.showMessageDialog(null, "Invalid credentials. Please try again.", "Access denied", JOptionPane.ERROR_MESSAGE);
            usernameField.setText("");
            passwordField.setText("");
        }
    }

    private static class LabelDistributionPanel extends JPanel {
        private Map<Integer, Integer> distribution = Collections.emptyMap();
        public void updateDistribution(Map<Integer, Integer> distribution) {
            this.distribution = distribution;
            repaint();
        }

        LabelDistributionPanel() {
            setPreferredSize(new Dimension(CHART_PANEL_DIMENSION.width, CHART_FIXED_HEIGHT));
            setMaximumSize(new Dimension(Integer.MAX_VALUE, CHART_FIXED_HEIGHT));
            setMinimumSize(new Dimension(240, CHART_FIXED_HEIGHT));
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

    private static class ModelExplanationPanel extends JPanel {
        private List<NeuralNetwork.LayerInfo> layers = Collections.emptyList();

        ModelExplanationPanel() {
            setPreferredSize(new Dimension(CHART_PANEL_DIMENSION.width, CHART_FIXED_HEIGHT));
            setMaximumSize(new Dimension(Integer.MAX_VALUE, CHART_FIXED_HEIGHT));
            setMinimumSize(new Dimension(240, CHART_FIXED_HEIGHT));
        }

        public void updateStructure(NeuralNetwork network) {
            layers = network == null ? Collections.emptyList() : network.describeLayers();
            repaint();
        }

        @Override
        protected void paintComponent(Graphics g) {
            super.paintComponent(g);
            setBorder(BorderFactory.createTitledBorder("Model Explanation"));
            Graphics2D g2 = (Graphics2D) g;
            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

            if (layers.isEmpty()) {
                g2.setColor(Color.GRAY);
                String placeholder = "Model structure unavailable.";
                FontMetrics fm = g2.getFontMetrics();
                int x = (getWidth() - fm.stringWidth(placeholder)) / 2;
                int y = getHeight() / 2;
                g2.drawString(placeholder, Math.max(0, x), y);
                return;
            }

            int horizontalPadding = 32;
            int verticalPadding = 32;
            int availableWidth = Math.max(64, getWidth() - horizontalPadding * 2);
            int availableHeight = Math.max(64, getHeight() - verticalPadding * 2 - 20);
            int layerCount = layers.size();
            double step = layerCount == 1 ? 0 : (double) availableWidth / (layerCount - 1);
            List<Point> previousCenters = Collections.emptyList();

            for (int layerIndex = 0; layerIndex < layerCount; layerIndex++) {
                NeuralNetwork.LayerInfo info = layers.get(layerIndex);
                int visualNodes = mapToVisualNodes(Math.max(1, info.outputs));
                double segmentHeight = availableHeight;
                double spacing = segmentHeight / (visualNodes + 1);
                int centerX = horizontalPadding + (int) (layerIndex == 0 ? 0 : Math.round(layerIndex * step));

                List<Point> currentCenters = new java.util.ArrayList<>();
                for (int node = 0; node < visualNodes; node++) {
                    int centerY = verticalPadding + (int) ((node + 1) * spacing);
                    currentCenters.add(new Point(centerX, centerY));
                    g2.setColor(new Color(91, 155, 213));
                    g2.fillOval(centerX - 8, centerY - 8, 16, 16);
                    g2.setColor(Color.DARK_GRAY);
                    g2.drawOval(centerX - 8, centerY - 8, 16, 16);
                }

                for (Point prev : previousCenters) {
                    g2.setColor(new Color(0, 120, 0, 120));
                    for (Point curr : currentCenters) {
                        g2.drawLine(prev.x, prev.y, curr.x, curr.y);
                    }
                }

                previousCenters = currentCenters;

                String typeLabel = info.typeName;
                String detailLabel = info.outputs + " outputs";
                FontMetrics fm = g2.getFontMetrics();
                int textY = getHeight() - verticalPadding / 2;
                int availableRight = getWidth() - horizontalPadding;
                int typeX = centerX - fm.stringWidth(typeLabel) / 2;
                int typeClampedX = Math.min(Math.max(horizontalPadding, typeX), Math.max(horizontalPadding, availableRight - fm.stringWidth(typeLabel)));
                g2.setColor(Color.BLACK);
                g2.drawString(typeLabel, typeClampedX, textY - 14);
                int detailX = centerX - fm.stringWidth(detailLabel) / 2;
                int detailClampedX = Math.min(Math.max(horizontalPadding, detailX), Math.max(horizontalPadding, availableRight - fm.stringWidth(detailLabel)));
                g2.drawString(detailLabel, detailClampedX, textY - 2);
            }
        }

        private int mapToVisualNodes(int outputs) {
            double normalized = Math.min(1.0, outputs / 400.0);
            return 3 + (int) (normalized * 10);
        }
    }

    private static class ConfusionMatrixPanel extends JPanel {
        private int[][] matrix = new int[0][0];

        ConfusionMatrixPanel() {
            setPreferredSize(new Dimension(CHART_PANEL_DIMENSION.width, CHART_FIXED_HEIGHT));
            setMaximumSize(new Dimension(Integer.MAX_VALUE, CHART_FIXED_HEIGHT));
            setMinimumSize(new Dimension(240, CHART_FIXED_HEIGHT));
        }

        public void updateMatrix(int[][] newMatrix) {
            if (newMatrix == null || newMatrix.length == 0) {
                matrix = new int[0][0];
            } else {
                matrix = new int[newMatrix.length][];
                for (int i = 0; i < newMatrix.length; i++) {
                    matrix[i] = java.util.Arrays.copyOf(newMatrix[i], newMatrix[i].length);
                }
            }
            repaint();
        }

        @Override
        protected void paintComponent(Graphics g) {
            super.paintComponent(g);
            setBorder(BorderFactory.createTitledBorder("Confusion Matrix"));
            if (matrix.length == 0) {
                g.setColor(Color.GRAY);
                String placeholder = "No prediction matrix available.";
                FontMetrics fm = g.getFontMetrics();
                int x = (getWidth() - fm.stringWidth(placeholder)) / 2;
                int y = getHeight() / 2;
                g.drawString(placeholder, Math.max(0, x), y);
                return;
            }

            int rows = matrix.length;
            int cols = matrix[0].length;
            int margin = 28;
            int axisLabelArea = 40;
            int width = getWidth();
            int height = getHeight();
            int gridWidth = Math.max(96, width - margin * 2 - axisLabelArea);
            int gridHeight = Math.max(96, height - margin * 2 - axisLabelArea - 24);
            int cellWidth = Math.max(20, gridWidth / cols);
            int cellHeight = Math.max(20, gridHeight / rows);
            int startX = margin + axisLabelArea;
            int startY = margin + axisLabelArea / 2;
            int maxValue = 0;
            for (int[] row : matrix) {
                for (int value : row) {
                    maxValue = Math.max(maxValue, value);
                }
            }

            for (int r = 0; r < rows; r++) {
                for (int c = 0; c < cols; c++) {
                    int x = startX + c * cellWidth;
                    int y = startY + r * cellHeight;
                    float intensity = maxValue == 0 ? 0f : (float) matrix[r][c] / maxValue;
                    int alpha = 80 + (int) (140 * intensity);
                    g.setColor(new Color(70, 130, 180, Math.min(255, alpha)));
                    g.fillRect(x, y, cellWidth, cellHeight);
                    g.setColor(Color.WHITE);
                    g.drawRect(x, y, cellWidth, cellHeight);
                    String text = String.valueOf(matrix[r][c]);
                    FontMetrics fm = g.getFontMetrics();
                    int textX = x + (cellWidth - fm.stringWidth(text)) / 2;
                    int textY = y + (cellHeight + fm.getAscent()) / 2 - 2;
                    g.setColor(Color.BLACK);
                    g.drawString(text, textX, textY);
                }
            }

            g.setColor(Color.DARK_GRAY);
            for (int r = 0; r < rows; r++) {
                String label = String.valueOf(r);
                int y = startY + r * cellHeight + cellHeight / 2 + 5;
                g.drawString(label, 4, y);
            }
            for (int c = 0; c < cols; c++) {
                String label = String.valueOf(c);
                FontMetrics fm = g.getFontMetrics();
                int x = startX + c * cellWidth + (cellWidth - fm.stringWidth(label)) / 2;
                g.drawString(label, x, startY + rows * cellHeight + 18);
            }

            g.setColor(Color.BLACK);
            g.drawString("Actual ↓", 4, startY - 12);
            int predictedY = Math.min(height - margin / 2, startY + rows * cellHeight + 32);
            g.drawString("Predicted →", startX + (cols * cellWidth) / 2 - 18, predictedY);
        }
    }

    private class CorrectionPanel extends JPanel {
        private final JLabel questionLabel = new JLabel("Draw or upload to generate a prediction.");
        private final JLabel previewLabel = new JLabel("Awaiting guess", SwingConstants.CENTER);
        private final JButton yesButton = new JButton("Yes");
        private final JButton noButton = new JButton("No");
        private final JButton clearButton = new JButton("Clear All");
        private final JTextField labelField = new JTextField(2);
        private final JButton saveButton = new JButton("Save Correction");
        private final JLabel statusLabel = new JLabel("Click on yes if I guessed right or no to save a correction.");
        private final JPanel correctionEntryPanel = new JPanel(new FlowLayout(FlowLayout.LEFT, 6, 4));
        private data.Image pendingSample;

        CorrectionPanel() {
            setBorder(BorderFactory.createTitledBorder("Correction Feedback"));
            setLayout(new BorderLayout(6, 6));

            questionLabel.setFont(questionLabel.getFont().deriveFont(Font.BOLD, 12f));
            JPanel questionBar = new JPanel(new BorderLayout(6, 4));
            questionBar.add(questionLabel, BorderLayout.CENTER);
            JPanel decisionButtons = new JPanel(new FlowLayout(FlowLayout.RIGHT, 4, 0));
            yesButton.setEnabled(false);
            noButton.setEnabled(false);
            decisionButtons.add(yesButton);
            decisionButtons.add(noButton);
            JPanel controls = new JPanel(new BorderLayout());
            controls.add(clearButton, BorderLayout.WEST);
            controls.add(decisionButtons, BorderLayout.EAST);
            questionBar.add(controls, BorderLayout.EAST);
            add(questionBar, BorderLayout.NORTH);

            previewLabel.setPreferredSize(new Dimension(140, 140));
            previewLabel.setOpaque(true);
            previewLabel.setBackground(Color.WHITE);
            previewLabel.setBorder(BorderFactory.createLineBorder(Color.LIGHT_GRAY));
            add(buildPreviewCard("Current Guess", previewLabel), BorderLayout.CENTER);

            correctionEntryPanel.add(new JLabel("True label:"));
            labelField.setToolTipText("Enter 0-9");
            correctionEntryPanel.add(labelField);
            correctionEntryPanel.add(saveButton);
            correctionEntryPanel.setVisible(false);

            statusLabel.setFont(statusLabel.getFont().deriveFont(Font.PLAIN, 12f));
            JPanel footer = new JPanel(new BorderLayout());
            footer.add(correctionEntryPanel, BorderLayout.NORTH);
            footer.add(statusLabel, BorderLayout.SOUTH);
            add(footer, BorderLayout.SOUTH);

            yesButton.addActionListener(e -> handleAffirmative());
            noButton.addActionListener(e -> handleNegative());
            saveButton.addActionListener(e -> saveCorrection());
            clearButton.addActionListener(e -> handleClearAll());
            resetPanel();
        }

        private void handleAffirmative() {
            statusLabel.setText("Hooray! I am on a roll.");
            correctionEntryPanel.setVisible(false);
        }

        private void handleNegative() {
            if (pendingSample == null) {
                statusLabel.setText("Provide a drawing or upload to correct.");
                return;
            }
            statusLabel.setText("Enter the true digit and save the correction.");
            correctionEntryPanel.setVisible(true);
            labelField.requestFocusInWindow();
        }

        private void saveCorrection() {
            if (pendingSample == null) {
                statusLabel.setText("No prediction available to correct.");
                return;
            }
            String text = labelField.getText().trim();
            if (!text.matches("[0-9]")) {
                statusLabel.setText("Enter a digit between 0 and 9.");
                return;
            }

            int label = Integer.parseInt(text);
            Image labeledSample = new Image(pendingSample.getData(), label);
            correctionStore.appendCorrection(labeledSample);
            statusLabel.setText("Saved correction for label " + label + ".");
            monitor.record("Saved correction for label " + label);
            labelField.setText("");
            correctionEntryPanel.setVisible(false);
        }

        void presentCandidate(SampleResult candidate, String prediction) {
            if (candidate != null) {
                pendingSample = candidate.sample;
                questionLabel.setText("Did I guess right? Prediction: " + prediction);
                previewLabel.setIcon(toPreviewIcon(candidate.processedPreview));
                previewLabel.setText("");
                statusLabel.setText("Click yes if correct or no to enter the true label.");
                yesButton.setEnabled(true);
                noButton.setEnabled(true);
            } else {
                pendingSample = null;
                questionLabel.setText("Draw or upload to generate a prediction.");
                previewLabel.setIcon(null);
                previewLabel.setText("Awaiting guess");
                statusLabel.setText("Click on yes if I guessed right or no to save a correction.");
                yesButton.setEnabled(false);
                noButton.setEnabled(false);
            }
            correctionEntryPanel.setVisible(false);
        }

        void resetPanel() {
            pendingSample = null;
            questionLabel.setText("Draw or upload to generate a prediction.");
            previewLabel.setIcon(null);
            previewLabel.setText("Awaiting guess");
            yesButton.setEnabled(false);
            noButton.setEnabled(false);
            statusLabel.setText("Click on yes if I guessed right or no to save a correction.");
            correctionEntryPanel.setVisible(false);
        }

        private void handleClearAll() {
            if (digitDrawUI != null) {
                digitDrawUI.resetDrawing();
            }
            uploadPanel.resetUploadState();
            resetPanel();
        }
    }

    private class UploadPanel extends JPanel {
        private final JButton uploadButton = new JButton("Upload Digit Photo");
        private final JLabel statusLabel = new JLabel("Upload sharpie digit on white paper.");
        private final JLabel originalPreview = new JLabel();
        private final JLabel processedPreview = new JLabel();
        private final FileNameExtensionFilter filter =
                new FileNameExtensionFilter("Image files", "png", "jpg", "jpeg", "bmp");

        UploadPanel() {
            setBorder(BorderFactory.createTitledBorder("Photo Upload"));
            setLayout(new BorderLayout(4, 4));

            JPanel previewPanel = new JPanel(new GridLayout(1, 2, 12, 8));
            previewPanel.add(buildPreviewCard("Uploaded", originalPreview));
            previewPanel.add(buildPreviewCard("Processed", processedPreview));
            add(previewPanel, BorderLayout.CENTER);

            JPanel topPanel = new JPanel(new FlowLayout(FlowLayout.LEFT, 8, 4));
            topPanel.add(uploadButton);
            topPanel.add(statusLabel);
            add(topPanel, BorderLayout.NORTH);

            uploadButton.addActionListener(e -> openFile());
        }

        private void openFile() {
            JFileChooser chooser = new JFileChooser();
            chooser.setFileFilter(filter);
            if (chooser.showOpenDialog(ProductDashboard.this) != JFileChooser.APPROVE_OPTION) {
                return;
            }

            File file = chooser.getSelectedFile();
            try {
                BufferedImage image = ImageIO.read(file);
                if (image == null) {
                    statusLabel.setText("Could not read image.");
                    return;
                }
                originalPreview.setIcon(toPreviewIcon(image));
                SampleResult sampleResult = createSampleResult(image);
                processedPreview.setIcon(toPreviewIcon(sampleResult.processedPreview));

                if (network == null) {
                    statusLabel.setText("Prediction skipped: model missing.");
                    monitor.record("Upload skipped, model unavailable.");
                    return;
                }
                int prediction = network.guess(sampleResult.sample);
                String predictionText = String.valueOf(prediction);
                statusLabel.setText("Prediction: " + predictionText);
                monitor.record("Uploaded image predicted: " + predictionText);
                correctionPanel.presentCandidate(sampleResult, predictionText);
            } catch (Exception ex) {
                statusLabel.setText("Error loading image.");
                monitor.record("Upload failed: " + ex.getMessage());
            }
        }

        void resetUploadState() {
            originalPreview.setIcon(null);
            processedPreview.setIcon(null);
            statusLabel.setText("Upload sharpie digit on white paper.");
        }

    }

    private JPanel buildPreviewCard(String title, JLabel preview) {
        preview.setHorizontalAlignment(SwingConstants.CENTER);
        preview.setBackground(Color.WHITE);
        preview.setOpaque(true);
        preview.setPreferredSize(new Dimension(140, 140));
        JPanel wrapper = new JPanel(new BorderLayout(4, 4));
        JLabel heading = new JLabel(title, SwingConstants.CENTER);
        heading.setFont(heading.getFont().deriveFont(Font.BOLD, 12f));
        wrapper.add(heading, BorderLayout.NORTH);
        wrapper.add(preview, BorderLayout.CENTER);
        return wrapper;
    }

    private ImageIcon toPreviewIcon(BufferedImage source) {
        if (source == null) {
            return null;
        }
        return new ImageIcon(source.getScaledInstance(140, 140, java.awt.Image.SCALE_SMOOTH));
    }

    private static class SampleResult {
        final BufferedImage processedPreview;
        final data.Image sample;

        SampleResult(BufferedImage processedPreview, data.Image sample) {
            this.processedPreview = processedPreview;
            this.sample = sample;
        }
    }

    private static final double MIN_BACKGROUND_THRESHOLD = 0.05;

    private SampleResult createSampleResult(BufferedImage original) {
        BufferedImage gray = new BufferedImage(28, 28, BufferedImage.TYPE_BYTE_GRAY);
        Graphics2D g2d = gray.createGraphics();
        g2d.drawImage(original, 0, 0, 28, 28, null);
        g2d.dispose();

        BufferedImage processed = new BufferedImage(28, 28, BufferedImage.TYPE_BYTE_GRAY);
        double[][] data = new double[28][28];
        for (int row = 0; row < 28; row++) {
            for (int col = 0; col < 28; col++) {
                int pixel = gray.getRGB(col, row) & 0xFF;
                double inverted = (255 - pixel) / 255.0;
                data[row][col] = inverted > CONTRAST_THRESHOLD ? 1.0 : 0.0;
            }
        }

        float[][] kernel = {
                {0.02f, 0.05f, 0.02f},
                {0.05f, 0.76f, 0.05f},
                {0.02f, 0.05f, 0.02f}
        };
        double[][] feathered = new double[28][28];
        for (int row = 0; row < 28; row++) {
            for (int col = 0; col < 28; col++) {
                double sum = 0;
                for (int kr = -1; kr <= 1; kr++) {
                    for (int kc = -1; kc <= 1; kc++) {
                        int nr = row + kr;
                        int nc = col + kc;
                        if (nr >= 0 && nr < 28 && nc >= 0 && nc < 28) {
                            sum += data[nr][nc] * kernel[kr + 1][kc + 1];
                        }
                    }
                }
                feathered[row][col] = Math.min(1.0, sum);
            }
        }

        for (int row = 0; row < 28; row++) {
            for (int col = 0; col < 28; col++) {
                double value = feathered[row][col];
                if (value < MIN_BACKGROUND_THRESHOLD) {
                    value = 0.0;
                } else if (value > CONTRAST_THRESHOLD) {
                    value = 1.0;
                } else {
                    value = (value - MIN_BACKGROUND_THRESHOLD) / (CONTRAST_THRESHOLD - MIN_BACKGROUND_THRESHOLD);
                    value = Math.min(1.0, Math.max(0.0, value));
                }
                int display = (int) (value * 255);
                int color = (0xFF << 24) | (display << 16) | (display << 8) | display;
                processed.setRGB(col, row, color);
                data[row][col] = value;
            }
        }

        return new SampleResult(processed, new data.Image(data, -1));
    }
}

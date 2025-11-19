package product;

import data.DataReader;
import data.Image;
import network.NeuralNetwork;

import javax.imageio.ImageIO;
import javax.swing.*;
import javax.swing.filechooser.FileNameExtensionFilter;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.image.BufferedImage;
import java.io.File;
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
    private final AccuracyTrendPanel accuracyPanel = new AccuracyTrendPanel();
    private final DrawingWidget drawingWidget = new DrawingWidget();
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

        Dimension chartsDim = new Dimension(Integer.MAX_VALUE, CHART_FIXED_HEIGHT);
        charts.setPreferredSize(new Dimension(760, CHART_FIXED_HEIGHT));
        charts.setMinimumSize(new Dimension(500, CHART_FIXED_HEIGHT));
        charts.setMaximumSize(chartsDim);

        JPanel centerWrapper = new JPanel();
        centerWrapper.setLayout(new BoxLayout(centerWrapper, BoxLayout.Y_AXIS));
        centerWrapper.setBorder(BorderFactory.createEmptyBorder(6, 6, 6, 6));

        JPanel chartHolder = new JPanel(new BorderLayout());
        chartHolder.setPreferredSize(new Dimension(760, CHART_FIXED_HEIGHT));
        chartHolder.setMinimumSize(new Dimension(760, CHART_FIXED_HEIGHT));
        chartHolder.setMaximumSize(new Dimension(Integer.MAX_VALUE, CHART_FIXED_HEIGHT));
        chartHolder.add(charts, BorderLayout.CENTER);

        centerWrapper.add(chartHolder);
        centerWrapper.add(Box.createRigidArea(new Dimension(0, 12)));

        JPanel bottomRow = new JPanel(new GridLayout(1, 2, 12, 0));
        bottomRow.add(uploadPanel);
        bottomRow.add(correctionPanel);
        centerWrapper.add(bottomRow);
        scrollContent.add(centerWrapper, BorderLayout.CENTER);
        scrollContent.setPreferredSize(new Dimension(1100, 640));

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

    private static class AccuracyTrendPanel extends JPanel {
        private List<Double> trend = Collections.emptyList();
        public void updateTrend(List<Double> trend) {
            this.trend = trend;
            repaint();
        }

        AccuracyTrendPanel() {
            setPreferredSize(new Dimension(CHART_PANEL_DIMENSION.width, CHART_FIXED_HEIGHT));
            setMaximumSize(new Dimension(Integer.MAX_VALUE, CHART_FIXED_HEIGHT));
            setMinimumSize(new Dimension(240, CHART_FIXED_HEIGHT));
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
        private static final Dimension DRAW_WIDGET_DIMENSION = new Dimension(320, 320);
        private final BufferedImage canvas = new BufferedImage(GRID_SIZE, GRID_SIZE, BufferedImage.TYPE_BYTE_GRAY);
        private final JLabel predictionLabel = new JLabel("Prediction: ");
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
            controls.add(submitButton);
            controls.add(predictionLabel);
            add(controls, BorderLayout.SOUTH);

            submitButton.addActionListener(e -> submitDrawing());

            clearCanvas();
            setPreferredSize(DRAW_WIDGET_DIMENSION);
            setMinimumSize(DRAW_WIDGET_DIMENSION);
            setMaximumSize(DRAW_WIDGET_DIMENSION);
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

        public void resetDrawing() {
            clearCanvas();
            predictionLabel.setText("Prediction: ");
        }

        private void submitDrawing() {
            if (network == null) {
                predictionLabel.setText("Prediction: model not loaded");
                monitor.record("Draw widget submit skipped: no model.");
                return;
            }
            double[][] snapshot = captureCanvas();
            Image sample = new Image(snapshot, -1);
            int prediction = network.guess(sample);
            String predictionText = String.valueOf(prediction);
            predictionLabel.setText("Prediction: " + predictionText);
            monitor.record("Draw widget predicted: " + predictionText);
            correctionPanel.presentCandidate(new SampleResult(duplicateCanvasImage(), sample), predictionText);
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

        private BufferedImage duplicateCanvasImage() {
            BufferedImage copy = new BufferedImage(GRID_SIZE, GRID_SIZE, BufferedImage.TYPE_BYTE_GRAY);
            Graphics2D g2d = copy.createGraphics();
            g2d.drawImage(canvas, 0, 0, null);
            g2d.dispose();
            return copy;
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
            drawingWidget.resetDrawing();
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

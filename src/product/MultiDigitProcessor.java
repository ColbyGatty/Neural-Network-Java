package product;

import data.Image;
import network.NeuralNetwork;

import java.awt.*;
import java.awt.image.BufferedImage;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Lightweight helper that detects whether an uploaded photo contains multiple
 * uniformly spaced digits, slices them into individual 28x28 images, and
 * stitches their predictions into a single string.
 */
public final class MultiDigitProcessor {
    private static final int TARGET_SIZE = 28;
    private static final int MARGIN = 4;
    private static final double GAP_THRESHOLD_RATIO = 0.05;
    private static final int MIN_SEGMENT_WIDTH = 3;
    private static final int MIN_GAP_WIDTH = 3;

    private MultiDigitProcessor() {
        // utility class
    }

    /**
     * Attempts to slice a processed sample matrix into separate digit images.
     *
     * @param normalized normalized 2D matrix (typically the processed 28×28 preview)
     * @return list of centered {@link Image} instances representing each digit
     */
    public static List<Image> extractDigits(double[][] normalized) {
        if (normalized == null || normalized.length == 0) {
            return Collections.emptyList();
        }
        int width = normalized[0].length;
        List<int[]> spans = findDigitSpans(normalized);
        if (spans.isEmpty()) {
            spans.add(new int[]{0, width - 1});
        }

        List<Image> digits = new ArrayList<>();
        for (int[] span : spans) {
            int start = span[0];
            int end = span[1];
            int segmentWidth = end - start + 1;
            if (segmentWidth < MIN_SEGMENT_WIDTH) {
                continue;
            }
            Image digit = buildCenteredDigit(normalized, start, end);
            if (digit != null) {
                digits.add(digit);
            }
        }
        if (digits.isEmpty()) {
            digits.add(normalizeToTarget(normalized));
        }
        return digits;
    }

    /**
     * Runs the existing network across the segmented digits and concatenates their guesses.
     */
    public static String predictCombined(NeuralNetwork network, List<Image> digits) {
        if (network == null || digits == null || digits.isEmpty()) {
            return "";
        }
        StringBuilder builder = new StringBuilder(digits.size());
        for (Image digit : digits) {
            builder.append(network.guess(digit));
        }
        return builder.toString();
    }

    private static List<int[]> findDigitSpans(double[][] normalized) {
        int width = normalized[0].length;
        int height = normalized.length;
        double gapThreshold = GAP_THRESHOLD_RATIO * height;

        List<int[]> spans = new ArrayList<>();
        boolean capturing = false;
        int start = 0;
        int gapWidth = 0;
        for (int col = 0; col < width; col++) {
            double columnSum = 0;
            for (int row = 0; row < height; row++) {
                columnSum += normalized[row][col];
            }
            boolean active = columnSum >= gapThreshold;
            if (active) {
                if (!capturing) {
                    start = col;
                    capturing = true;
                }
                gapWidth = 0;
            } else if (capturing) {
                gapWidth++;
                if (gapWidth >= MIN_GAP_WIDTH) {
                    spans.add(new int[]{start, col - gapWidth});
                    capturing = false;
                    gapWidth = 0;
                }
            }
        }
        if (capturing) {
            spans.add(new int[]{start, width - 1});
        }
        return spans;
    }

    private static Image buildCenteredDigit(double[][] normalized, int startCol, int endCol) {
        int height = normalized.length;
        int width = endCol - startCol + 1;
        int minRow = height;
        int maxRow = -1;
        for (int row = 0; row < height; row++) {
            for (int col = startCol; col <= endCol; col++) {
                if (normalized[row][col] > 0) {
                    minRow = Math.min(minRow, row);
                    maxRow = Math.max(maxRow, row);
                }
            }
        }
        if (maxRow < minRow) {
            minRow = 0;
            maxRow = height - 1;
        }

        int segmentHeight = maxRow - minRow + 1;
        double[][] segment = new double[segmentHeight][width];
        for (int row = 0; row < segmentHeight; row++) {
            for (int col = 0; col < width; col++) {
                segment[row][col] = normalized[minRow + row][startCol + col];
            }
        }
        return normalizeToTarget(segment);
    }

    private static Image normalizeToTarget(double[][] segment) {
        int segmentWidth = segment[0].length;
        int segmentHeight = segment.length;
        BufferedImage raw = new BufferedImage(segmentWidth, segmentHeight, BufferedImage.TYPE_BYTE_GRAY);
        for (int row = 0; row < segmentHeight; row++) {
            for (int col = 0; col < segmentWidth; col++) {
                int shade = segment[row][col] > 0 ? 255 : 0;
                int rgb = (0xFF << 24) | (shade << 16) | (shade << 8) | shade;
                raw.setRGB(col, row, rgb);
            }
        }

        int available = TARGET_SIZE - MARGIN * 2;
        double widthRatio = available / (double) segmentWidth;
        double heightRatio = available / (double) segmentHeight;
        double scale = Math.min(widthRatio, heightRatio);
        scale = Math.max(0.1, scale);
        int scaledWidth = Math.max(1, (int) Math.round(segmentWidth * scale));
        int scaledHeight = Math.max(1, (int) Math.round(segmentHeight * scale));

        BufferedImage scaled = new BufferedImage(scaledWidth, scaledHeight, BufferedImage.TYPE_BYTE_GRAY);
        Graphics2D scaler = scaled.createGraphics();
        scaler.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BILINEAR);
        scaler.setColor(Color.BLACK);
        scaler.fillRect(0, 0, scaledWidth, scaledHeight);
        scaler.drawImage(raw, 0, 0, scaledWidth, scaledHeight, null);
        scaler.dispose();

        BufferedImage framed = new BufferedImage(TARGET_SIZE, TARGET_SIZE, BufferedImage.TYPE_BYTE_GRAY);
        Graphics2D g2d = framed.createGraphics();
        g2d.setColor(Color.BLACK);
        g2d.fillRect(0, 0, TARGET_SIZE, TARGET_SIZE);
        int offsetX = (TARGET_SIZE - scaledWidth) / 2;
        int offsetY = (TARGET_SIZE - scaledHeight) / 2;
        g2d.drawImage(scaled, offsetX, offsetY, null);
        g2d.dispose();

        double[][] data = new double[TARGET_SIZE][TARGET_SIZE];
        for (int row = 0; row < TARGET_SIZE; row++) {
            for (int col = 0; col < TARGET_SIZE; col++) {
                data[row][col] = (framed.getRGB(col, row) & 0xFF) / 255.0;
            }
        }
        return new Image(data, -1);
    }
}

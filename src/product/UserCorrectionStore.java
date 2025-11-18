package product;

import data.DataReader;
import data.Image;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.Collections;
import java.util.List;

/**
 * Persists user-corrected drawings so they can be reused in training.
 */
public class UserCorrectionStore {
    private static final String CORRECTION_PATH = "data/user_corrections.csv";

    public void appendCorrection(Image image) {
        if (image == null) {
            return;
        }
        File file = new File(CORRECTION_PATH);
        File parent = file.getParentFile();
        if (parent != null && !parent.exists()) {
            parent.mkdirs();
        }

        try (BufferedWriter writer = new BufferedWriter(new FileWriter(file, true))) {
            writer.write(serialize(image));
            writer.newLine();
        } catch (IOException e) {
            throw new IllegalStateException("Unable to persist correction: " + e.getMessage(), e);
        }
    }

    public List<Image> loadCorrections() {
        File file = new File(CORRECTION_PATH);
        if (!file.exists()) {
            return Collections.emptyList();
        }

        try {
            return new DataReader().readData(CORRECTION_PATH);
        } catch (IllegalArgumentException e) {
            return Collections.emptyList();
        }
    }

    private String serialize(Image image) {
        StringBuilder sb = new StringBuilder();
        sb.append(image.getLabel());
        for (double[] row : image.getData()) {
            for (double value : row) {
                sb.append(',').append(value);
            }
        }
        return sb.toString();
    }
}

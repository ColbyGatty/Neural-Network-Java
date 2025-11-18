package product;

import java.io.File;
import java.util.Arrays;
import java.util.Comparator;

/**
 * Shared helper to resolve the most recent serialized model.
 */
public class ModelLocator {
    public static String findLatestModelPath(String directoryPath) {
        File dir = new File(directoryPath);
        if (!dir.exists() || !dir.isDirectory()) {
            return null;
        }

        File[] serFiles = dir.listFiles((d, name) -> name.endsWith(".ser"));
        if (serFiles == null || serFiles.length == 0) {
            return null;
        }

        Arrays.sort(serFiles, Comparator.comparingLong(File::lastModified).reversed());
        return serFiles[0].getAbsolutePath();
    }
}

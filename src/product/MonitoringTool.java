package product;

import java.time.Duration;
import java.time.Instant;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Simple tool capturing timestamped health messages for the product.
 */
public class MonitoringTool {
    private final String productName;
    private final List<String> logs = new ArrayList<>();
    private final Instant startTime = Instant.now();
    private static final DateTimeFormatter FORMATTER =
            DateTimeFormatter.ofPattern("HH:mm:ss").withZone(ZoneId.systemDefault());

    public MonitoringTool(String productName) {
        this.productName = productName;
        record("Monitoring started for " + productName);
    }

    public void record(String message) {
        String entry = FORMATTER.format(Instant.now()) + " | " + message;
        synchronized (logs) {
            logs.add(entry);
            if (logs.size() > 20) {
                logs.remove(0);
            }
        }
    }

    public void recordMetric(String metricName, double value) {
        record(String.format("%s: %.2f", metricName, value));
    }

    public String getStatusSummary() {
        Duration uptime = Duration.between(startTime, Instant.now());
        long minutes = uptime.toMinutes();
        return String.format("%s | Uptime: %d min %d sec | Entries: %d",
                productName, minutes, uptime.getSeconds() % 60, logs.size());
    }

    public List<String> recentLogs() {
        synchronized (logs) {
            return Collections.unmodifiableList(new ArrayList<>(logs));
        }
    }
}

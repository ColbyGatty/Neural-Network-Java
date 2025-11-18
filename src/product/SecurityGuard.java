package product;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

/**
 * Minimal token-based guard to gate access to the dashboard.
 */
public class SecurityGuard {
    public static final String ENV_TOKEN_KEY = "NN_DASHBOARD_TOKEN";
    private static final String SHARED_SECRET_HASH = "a25cd17a7b14325b73f5bbbf59180cfef68ace147d34420f3a867f8d7fc4bd19";

    public boolean authorize(String token) {
        if (token == null || token.isBlank()) {
            return false;
        }
        return digest(token).equals(SHARED_SECRET_HASH);
    }

    public boolean authorizeFromEnvironment() {
        return authorize(System.getenv(ENV_TOKEN_KEY));
    }

    private String digest(String candidate) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hashed = digest.digest(candidate.getBytes(StandardCharsets.UTF_8));
            StringBuilder builder = new StringBuilder();
            for (byte b : hashed) {
                builder.append(String.format("%02x", b));
            }
            return builder.toString();
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException("SHA-256 is not available", e);
        }
    }
}

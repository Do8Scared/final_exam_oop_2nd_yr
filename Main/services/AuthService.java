package services;

import java.security.MessageDigest;
import java.nio.charset.StandardCharsets;

/**
 * Handles admin authentication with brute-force protection.
 * Uses constant-time PIN comparison to prevent timing attacks.
 *
 * Lockout state is currently in-memory. The class is isolated and ready
 * for persistence (database or file) if needed in the future.
 */
public class AuthService {

    private static final int MAX_ATTEMPTS = 3;
    private static final long LOCKOUT_DURATION_MS = 30_000;

    private int failedPinAttempts = 0;
    private long lockoutEndTime = 0;

    /**
     * Checks if the admin console is currently locked out.
     *
     * @return true if locked out, false if access is allowed
     */
    public boolean isLockedOut() {
        return System.currentTimeMillis() < lockoutEndTime;
    }

    /**
     * Returns the remaining lockout time in seconds.
     *
     * @return remaining seconds, or 0 if not locked out
     */
    public long getRemainingLockoutSeconds() {
        if (!isLockedOut()) return 0;
        return (lockoutEndTime - System.currentTimeMillis()) / 1000;
    }

    /**
     * Retrieves the configured admin PIN from environment variables or system properties.
     * Returns null if not configured (fail-secure design — no PIN means no admin access).
     *
     * @return the configured PIN string, or null if not configured
     */
    public String getConfiguredPin() {
        String configuredPin = System.getenv("POS_ADMIN_PIN");
        if (configuredPin == null || configuredPin.isBlank()) {
            configuredPin = System.getProperty("pos.admin.pin");
        }
        if (configuredPin == null || configuredPin.isBlank()) {
            return null;
        }
        return configuredPin;
    }

    /**
     * Validates a PIN attempt against the configured PIN using constant-time comparison.
     * Tracks failed attempts and triggers lockout after MAX_ATTEMPTS consecutive failures.
     *
     * @param enteredPin   the PIN entered by the user
     * @param configuredPin the correct PIN from configuration
     * @return true if the PIN matches, false otherwise
     */
    public boolean validatePin(String enteredPin, String configuredPin) {
        boolean match = constantTimeEquals(enteredPin, configuredPin);

        if (match) {
            failedPinAttempts = 0;
            return true;
        } else {
            failedPinAttempts++;
            if (failedPinAttempts >= MAX_ATTEMPTS) {
                lockoutEndTime = System.currentTimeMillis() + LOCKOUT_DURATION_MS;
            }
            return false;
        }
    }

    /**
     * Returns the number of failed PIN attempts since the last successful authentication.
     *
     * @return the current failed attempt count
     */
    public int getFailedAttempts() {
        return failedPinAttempts;
    }

    /**
     * Returns the maximum number of allowed PIN attempts before lockout.
     *
     * @return the max attempts threshold
     */
    public int getMaxAttempts() {
        return MAX_ATTEMPTS;
    }

    /**
     * Performs a constant-time string comparison to prevent timing attacks.
     * Uses MessageDigest.isEqual on the UTF-8 byte arrays of both strings.
     *
     * @param a the first string
     * @param b the second string
     * @return true if the strings are equal, false otherwise
     */
    private boolean constantTimeEquals(String a, String b) {
        if (a == null || b == null) {
            return a == b;
        }
        byte[] aBytes = a.getBytes(StandardCharsets.UTF_8);
        byte[] bBytes = b.getBytes(StandardCharsets.UTF_8);
        return MessageDigest.isEqual(aBytes, bBytes);
    }
}

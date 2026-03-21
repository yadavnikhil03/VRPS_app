package com.example.verticalparking.utils;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

/**
 * Utility class for PIN hashing using SHA-256.
 * Never store plain PINs — always hash them first.
 */
public final class PinUtils {

    private PinUtils() {
    }

    /**
     * Hashes a plain text PIN using SHA-256.
     *
     * @param pin the raw 4-digit PIN
     * @return hex-encoded SHA-256 hash, or empty string on failure
     */
    public static String hashPin(String pin) {
        if (pin == null || pin.isEmpty()) {
            return "";
        }
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hashBytes = digest.digest(pin.getBytes(StandardCharsets.UTF_8));
            StringBuilder hex = new StringBuilder();
            for (byte b : hashBytes) {
                hex.append(String.format("%02x", b));
            }
            return hex.toString();
        } catch (NoSuchAlgorithmException e) {
            return "";
        }
    }

    /**
     * Verifies a plain PIN against a stored hash.
     *
     * @param pin        the raw PIN entered by the user
     * @param storedHash the previously stored SHA-256 hash
     * @return true if they match
     */
    public static boolean verifyPin(String pin, String storedHash) {
        if (pin == null || storedHash == null) {
            return false;
        }
        String inputHash = hashPin(pin);
        return !inputHash.isEmpty() && inputHash.equals(storedHash);
    }

    /**
     * Validates that a PIN meets our requirements (exactly 4 digits).
     */
    public static boolean isValidPin(String pin) {
        return pin != null && pin.matches("\\d{4}");
    }
}

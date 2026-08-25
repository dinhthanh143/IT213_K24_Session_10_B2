package com.rikkeipay.util;

import java.text.NumberFormat;
import java.util.Locale;

/**
 * Utility class for Masking Personally Identifiable Information (PII)
 * and financial sensitive data before exporting telemetry traces.
 */
public final class PiiMaskingUtils {

    private PiiMaskingUtils() {
        // Utility class
    }

    /**
     * Masks bank account numbers or card numbers.
     * Example: "190356789012" -> "1903****9012"
     */
    public static String maskAccountNumber(String accountNo) {
        if (accountNo == null || accountNo.trim().isEmpty()) {
            return "[ANONYMOUS_ACCOUNT]";
        }
        String clean = accountNo.trim();
        if (clean.length() <= 6) {
            return "****" + clean.substring(Math.max(0, clean.length() - 2));
        }
        String prefix = clean.substring(0, 4);
        String suffix = clean.substring(clean.length() - 4);
        return prefix + "****" + suffix;
    }

    /**
     * Masks customer usernames or full names.
     * Example: "nguyen_van_a" -> "ngu***_a"
     */
    public static String maskUsername(String username) {
        if (username == null || username.trim().isEmpty()) {
            return "user_anonymous";
        }
        String clean = username.trim();
        if (clean.length() <= 4) {
            return clean.charAt(0) + "***";
        }
        return clean.substring(0, 3) + "***" + clean.substring(clean.length() - 2);
    }

    /**
     * Formats amount safely for tracing logs.
     */
    public static String formatCurrency(double amount) {
        NumberFormat currencyFormat = NumberFormat.getCurrencyInstance(new Locale("vi", "VN"));
        return currencyFormat.format(amount);
    }
}

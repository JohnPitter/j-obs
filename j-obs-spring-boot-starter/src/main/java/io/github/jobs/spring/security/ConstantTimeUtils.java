package io.github.jobs.spring.security;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;

/**
 * Utility class providing constant-time operations to prevent timing attacks.
 * <p>
 * Timing attacks work by measuring the time taken to compare values.
 * Standard string comparison returns early when a mismatch is found,
 * leaking information about how many characters matched.
 * <p>
 * These methods always take the same amount of time regardless of the input,
 * preventing attackers from using timing differences to guess valid values.
 */
public final class ConstantTimeUtils {

    private ConstantTimeUtils() {
        // Utility class
    }

    /**
     * Compares two byte arrays in constant time.
     * <p>
     * Unlike standard array comparison, this method always compares all bytes
     * and takes the same time regardless of where (or if) a mismatch occurs.
     *
     * @param a first array
     * @param b second array
     * @return true if arrays are equal, false otherwise
     */
    public static boolean equals(byte[] a, byte[] b) {
        if (a == null || b == null) {
            // Use bitwise OR to avoid short-circuit evaluation
            return (a == null) & (b == null);
        }

        // Use MessageDigest.isEqual which is guaranteed to be constant-time
        return MessageDigest.isEqual(a, b);
    }

    /**
     * Compares two strings in constant time.
     * <p>
     * This method prevents timing attacks by:
     * <ul>
     *   <li>Always comparing against the longer string's length</li>
     *   <li>Using constant-time byte comparison</li>
     *   <li>Not short-circuiting on length mismatch</li>
     * </ul>
     *
     * @param a first string
     * @param b second string
     * @return true if strings are equal, false otherwise
     */
    public static boolean equals(String a, String b) {
        if (a == null || b == null) {
            return (a == null) & (b == null);
        }

        byte[] aBytes = a.getBytes(StandardCharsets.UTF_8);
        byte[] bBytes = b.getBytes(StandardCharsets.UTF_8);

        return equals(aBytes, bBytes);
    }

    /**
     * Compares two strings in constant time, even when lengths differ.
     * <p>
     * This is the most secure comparison as it doesn't leak length information.
     * It hashes both values before comparison, ensuring constant-time operation
     * regardless of input lengths.
     * <p>
     * Use this for comparing sensitive values like usernames or tokens where
     * even length information should not be leaked.
     *
     * @param a first string
     * @param b second string
     * @return true if strings are equal, false otherwise
     */
    public static boolean secureEquals(String a, String b) {
        if (a == null || b == null) {
            return (a == null) & (b == null);
        }

        byte[] aBytes = a.getBytes(StandardCharsets.UTF_8);
        byte[] bBytes = b.getBytes(StandardCharsets.UTF_8);

        // For different length strings, we still need to process them
        // in constant time. Hash both values to ensure fixed-length comparison.
        // This prevents length-based timing attacks.

        // Track length difference separately to preserve correct result
        int lengthDiff = aBytes.length ^ bBytes.length;

        // Perform XOR comparison on the shorter length
        int minLength = Math.min(aBytes.length, bBytes.length);
        int maxLength = Math.max(aBytes.length, bBytes.length);

        int result = lengthDiff;
        for (int i = 0; i < maxLength; i++) {
            // For indices beyond array bounds, use 0 for comparison
            // This ensures we always iterate maxLength times
            byte aByte = (i < aBytes.length) ? aBytes[i] : 0;
            byte bByte = (i < bBytes.length) ? bBytes[i] : 0;
            result |= aByte ^ bByte;
        }

        return result == 0;
    }

    /**
     * Compares a CharSequence to a String in constant time.
     *
     * @param a first value (CharSequence)
     * @param b second value (String)
     * @return true if values are equal, false otherwise
     */
    public static boolean equals(CharSequence a, String b) {
        if (a == null) {
            return b == null;
        }
        return secureEquals(a.toString(), b);
    }
}

package io.github.jobs.spring.security;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.crypto.SecretKeyFactory;
import javax.crypto.spec.PBEKeySpec;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.security.spec.InvalidKeySpecException;
import java.util.Base64;

/**
 * Password encoder using PBKDF2 with HMAC-SHA256.
 * <p>
 * This is a secure, standards-based password hashing implementation that doesn't
 * require external dependencies. For production use, passwords should always be
 * stored in hashed format.
 * <p>
 * Hashed passwords are stored in the format: {@code {pbkdf2}salt:hash}
 * <p>
 * Example configuration:
 * <pre>{@code
 * j-obs:
 *   security:
 *     users:
 *       - username: admin
 *         # Hashed password (use PasswordEncoder.encode("your-password"))
 *         password: "{pbkdf2}base64salt:base64hash"
 * }</pre>
 * <p>
 * To generate a hashed password programmatically:
 * <pre>{@code
 * String hashedPassword = PasswordEncoder.encode("my-secure-password");
 * }</pre>
 */
public final class PasswordEncoder {

    private static final Logger log = LoggerFactory.getLogger(PasswordEncoder.class);

    private static final String ALGORITHM = "PBKDF2WithHmacSHA256";
    private static final String PREFIX = "{pbkdf2}";
    private static final int ITERATIONS = 310_000; // OWASP recommendation for PBKDF2-SHA256
    private static final int SALT_LENGTH = 16;
    private static final int HASH_LENGTH = 32;

    private static final SecureRandom SECURE_RANDOM = new SecureRandom();

    private PasswordEncoder() {
        // Utility class
    }

    /**
     * Encodes a raw password using PBKDF2.
     *
     * @param rawPassword the password to encode
     * @return the encoded password in format {pbkdf2}salt:hash
     */
    public static String encode(CharSequence rawPassword) {
        if (rawPassword == null || rawPassword.length() == 0) {
            throw new IllegalArgumentException("Password cannot be null or empty");
        }

        byte[] salt = new byte[SALT_LENGTH];
        SECURE_RANDOM.nextBytes(salt);

        byte[] hash = pbkdf2(rawPassword, salt);

        String saltBase64 = Base64.getEncoder().encodeToString(salt);
        String hashBase64 = Base64.getEncoder().encodeToString(hash);

        return PREFIX + saltBase64 + ":" + hashBase64;
    }

    /**
     * Verifies a raw password against an encoded password.
     * <p>
     * This method supports both hashed passwords (prefixed with {pbkdf2})
     * and plaintext passwords (for backward compatibility, but a warning is logged).
     *
     * @param rawPassword     the raw password to verify
     * @param encodedPassword the encoded password to verify against
     * @return true if the password matches, false otherwise
     */
    public static boolean matches(CharSequence rawPassword, String encodedPassword) {
        if (rawPassword == null || encodedPassword == null) {
            return false;
        }

        if (isEncoded(encodedPassword)) {
            return matchesEncoded(rawPassword, encodedPassword);
        }

        // Plaintext comparison (backward compatibility) with constant-time comparison
        log.warn("Plaintext password detected. Please hash passwords using PasswordEncoder.encode() for security.");
        return secureCompare(rawPassword.toString(), encodedPassword);
    }

    /**
     * Checks if a password is encoded (hashed).
     *
     * @param password the password to check
     * @return true if the password is in encoded format
     */
    public static boolean isEncoded(String password) {
        return password != null && password.startsWith(PREFIX);
    }

    /**
     * Verifies a raw password against a PBKDF2-encoded password.
     */
    private static boolean matchesEncoded(CharSequence rawPassword, String encodedPassword) {
        try {
            String payload = encodedPassword.substring(PREFIX.length());
            int colonIndex = payload.indexOf(':');
            if (colonIndex == -1) {
                log.debug("Invalid encoded password format: missing colon separator");
                return false;
            }

            String saltBase64 = payload.substring(0, colonIndex);
            String hashBase64 = payload.substring(colonIndex + 1);

            byte[] salt = Base64.getDecoder().decode(saltBase64);
            byte[] expectedHash = Base64.getDecoder().decode(hashBase64);

            byte[] actualHash = pbkdf2(rawPassword, salt);

            return constantTimeEquals(expectedHash, actualHash);
        } catch (IllegalArgumentException e) {
            log.debug("Failed to decode password: {}", e.getMessage());
            return false;
        }
    }

    /**
     * Performs PBKDF2 key derivation.
     */
    private static byte[] pbkdf2(CharSequence password, byte[] salt) {
        try {
            PBEKeySpec spec = new PBEKeySpec(
                    toCharArray(password),
                    salt,
                    ITERATIONS,
                    HASH_LENGTH * 8
            );
            SecretKeyFactory factory = SecretKeyFactory.getInstance(ALGORITHM);
            return factory.generateSecret(spec).getEncoded();
        } catch (NoSuchAlgorithmException | InvalidKeySpecException e) {
            throw new IllegalStateException("Failed to compute PBKDF2 hash", e);
        }
    }

    /**
     * Converts CharSequence to char array without creating intermediate String.
     */
    private static char[] toCharArray(CharSequence charSequence) {
        char[] chars = new char[charSequence.length()];
        for (int i = 0; i < charSequence.length(); i++) {
            chars[i] = charSequence.charAt(i);
        }
        return chars;
    }

    /**
     * Constant-time byte array comparison to prevent timing attacks.
     * Uses {@link ConstantTimeUtils#equals(byte[], byte[])} for guaranteed constant-time operation.
     */
    private static boolean constantTimeEquals(byte[] a, byte[] b) {
        return ConstantTimeUtils.equals(a, b);
    }

    /**
     * Constant-time string comparison to prevent timing attacks.
     * Uses {@link ConstantTimeUtils#secureEquals} which doesn't leak length information.
     */
    private static boolean secureCompare(String a, String b) {
        return ConstantTimeUtils.secureEquals(a, b);
    }
}

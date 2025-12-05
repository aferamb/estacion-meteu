package Utils;

import java.security.SecureRandom;
import java.security.spec.KeySpec;
import java.util.Base64;
import javax.crypto.SecretKeyFactory;
import javax.crypto.spec.PBEKeySpec;

public final class PasswordUtil {
    private static final SecureRandom RAND = new SecureRandom();
    private static final int ITERATIONS = 65536;
    private static final int KEY_LENGTH = 256; // bits

    private PasswordUtil() {}

    public static String generateSalt(int bytes) {
        byte[] salt = new byte[bytes];
        RAND.nextBytes(salt);
        return Base64.getEncoder().encodeToString(salt);
    }

    public static String hashPassword(char[] password, String saltBase64) throws Exception {
        byte[] salt = Base64.getDecoder().decode(saltBase64);
        KeySpec spec = new PBEKeySpec(password, salt, ITERATIONS, KEY_LENGTH);
        SecretKeyFactory f = SecretKeyFactory.getInstance("PBKDF2WithHmacSHA256");
        byte[] hash = f.generateSecret(spec).getEncoded();
        return Base64.getEncoder().encodeToString(hash);
    }

    public static boolean verifyPassword(char[] password, String saltBase64, String expectedHashBase64) throws Exception {
        String computed = hashPassword(password, saltBase64);
        return constantTimeEquals(computed, expectedHashBase64);
    }

    private static boolean constantTimeEquals(String a, String b) {
        if (a == null || b == null) return false;
        byte[] xa = a.getBytes(java.nio.charset.StandardCharsets.UTF_8);
        byte[] yb = b.getBytes(java.nio.charset.StandardCharsets.UTF_8);
        if (xa.length != yb.length) {
            return false;
        }
        int result = 0;
        for (int i = 0; i < xa.length; i++) {
            result |= xa[i] ^ yb[i];
        }
        return result == 0;
    }
}

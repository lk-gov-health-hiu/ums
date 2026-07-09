package lk.gov.health.ums.bean;

import org.jasypt.util.password.BasicPasswordEncryptor;

/**
 * Same Jasypt-based hashing dmis/fmis use (BasicPasswordEncryptor, salted
 * bcrypt-like output) — kept for continuity rather than introducing a
 * second hashing library for one small system.
 *
 * @author Dr M H B Ariyaratne, buddhika.ari@gmail.com
 */
public final class PasswordUtil {

    private PasswordUtil() {
    }

    public static String hash(String plainPassword) {
        return new BasicPasswordEncryptor().encryptPassword(plainPassword);
    }

    public static boolean matches(String plainPassword, String hash) {
        return new BasicPasswordEncryptor().checkPassword(plainPassword, hash);
    }

}

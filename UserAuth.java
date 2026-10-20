
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

import java.security.SecureRandom;
import java.security.MessageDigest;

import javax.crypto.SecretKeyFactory;
import javax.crypto.spec.PBEKeySpec;

import java.util.Base64;

public class UserAuth {

    // =========================================
    // 1. CREATE USERS TABLE
    // =========================================

    public static void initializeTable() throws SQLException {

        String sql = """
            CREATE TABLE IF NOT EXISTS users (
                user_id INTEGER PRIMARY KEY AUTOINCREMENT,
                name TEXT NOT NULL,
                email TEXT NOT NULL UNIQUE,
                phone TEXT NOT NULL,
                password_hash TEXT NOT NULL,
                role TEXT NOT NULL CHECK(role IN ('ADMIN', 'USER')),
                created_at DATETIME DEFAULT CURRENT_TIMESTAMP
            )
            """;

        try (Connection con = DBConnection.getConnection();
             PreparedStatement ps = con.prepareStatement(sql)) {

            ps.executeUpdate();

            System.out.println("Users table created successfully!");
        }
    }

    // =========================================
    // 2. USER REGISTRATION
    // =========================================

    public static boolean registerUser(
            String name,
            String email,
            String phone,
            String password) throws Exception {

        if (name == null || name.trim().isEmpty()
                || email == null
                || !email.matches("^[^\\s@]+@[^\\s@]+\\.[^\\s@]+$")
                || phone == null
                || !phone.matches("[0-9]{10,15}")
                || password == null
                || password.length() < 8) {

            return false;
        }

        String sql = """
            INSERT INTO users
            (name, email, phone, password_hash, role)
            VALUES (?, ?, ?, ?, 'USER')
            """;

        try (Connection con = DBConnection.getConnection();
             PreparedStatement ps = con.prepareStatement(sql)) {

            ps.setString(1, name.trim());
            ps.setString(2, email.trim().toLowerCase());
            ps.setString(3, phone.trim());
            ps.setString(4, hashPassword(password));

            return ps.executeUpdate() == 1;

        } catch (SQLException e) {

            if (e.getMessage() != null
                    && e.getMessage().toLowerCase().contains("unique")) {
                return false;
            }

            throw e;
        }
    }

    // =========================================
    // 3. ADMIN AND USER LOGIN
    // =========================================

    public static AuthUser login(
            String email,
            String password) throws Exception {

        if (email == null || password == null) {
            return null;
        }

        String sql = """
            SELECT user_id, name, email, phone,
                   password_hash, role
            FROM users
            WHERE email = ?
            """;

        try (Connection con = DBConnection.getConnection();
             PreparedStatement ps = con.prepareStatement(sql)) {

            ps.setString(1, email.trim().toLowerCase());

            try (ResultSet rs = ps.executeQuery()) {

                if (rs.next()) {

                    String storedHash =
                            rs.getString("password_hash");

                    if (verifyPassword(password, storedHash)) {

                        return new AuthUser(
                            rs.getInt("user_id"),
                            rs.getString("name"),
                            rs.getString("email"),
                            rs.getString("phone"),
                            rs.getString("role")
                        );
                    }
                }
            }
        }

        return null;
    }

    // =========================================
    // 4. CREATE ADMIN ACCOUNT
    // =========================================

    // Call this only from a trusted local setup program.
    // Never expose it through public user registration.

    public static boolean createAdmin(
            String name,
            String email,
            String phone,
            String password) throws Exception {

        if (name == null || name.trim().isEmpty()
                || email == null
                || !email.matches("^[^\\s@]+@[^\\s@]+\\.[^\\s@]+$")
                || phone == null
                || !phone.matches("[0-9]{10,15}")
                || password == null
                || password.length() < 8) {

            return false;
        }

        String sql = """
            INSERT INTO users
            (name, email, phone, password_hash, role)
            VALUES (?, ?, ?, ?, 'ADMIN')
            """;

        try (Connection con = DBConnection.getConnection();
             PreparedStatement ps = con.prepareStatement(sql)) {

            ps.setString(1, name.trim());
            ps.setString(2, email.trim().toLowerCase());
            ps.setString(3, phone.trim());
            ps.setString(4, hashPassword(password));

            return ps.executeUpdate() == 1;

        } catch (SQLException e) {

            if (e.getMessage() != null
                    && e.getMessage().toLowerCase().contains("unique")) {
                return false;
            }

            throw e;
        }
    }

    // =========================================
    // 5. PASSWORD HASHING
    // =========================================

    private static String hashPassword(
            String password) throws Exception {

        byte[] salt = new byte[16];

        new SecureRandom().nextBytes(salt);

        PBEKeySpec spec = new PBEKeySpec(
            password.toCharArray(),
            salt,
            120000,
            256
        );

        byte[] hash;

        try {
            hash = SecretKeyFactory
                    .getInstance("PBKDF2WithHmacSHA256")
                    .generateSecret(spec)
                    .getEncoded();
        } finally {
            spec.clearPassword();
        }

        return Base64.getEncoder().encodeToString(salt)
                + ":"
                + Base64.getEncoder().encodeToString(hash);
    }

    // =========================================
    // 6. PASSWORD VERIFICATION
    // =========================================

    private static boolean verifyPassword(
            String password,
            String storedHash) throws Exception {

        if (storedHash == null) {
            return false;
        }

        String[] parts = storedHash.split(":");

        if (parts.length != 2) {
            return false;
        }

        byte[] salt;
        byte[] expectedHash;

        try {
            salt = Base64.getDecoder().decode(parts[0]);
            expectedHash = Base64.getDecoder().decode(parts[1]);
        } catch (IllegalArgumentException e) {
            return false;
        }

        PBEKeySpec spec = new PBEKeySpec(
            password.toCharArray(),
            salt,
            120000,
            256
        );

        byte[] actualHash;

        try {
            actualHash = SecretKeyFactory
                    .getInstance("PBKDF2WithHmacSHA256")
                    .generateSecret(spec)
                    .getEncoded();
        } finally {
            spec.clearPassword();
        }

        return MessageDigest.isEqual(
                expectedHash,
                actualHash
        );
    }

    // =========================================
    // 7. AUTHENTICATED USER DETAILS
    // =========================================

    public static class AuthUser {

        private final int userId;
        private final String name;
        private final String email;
        private final String phone;
        private final String role;

        public AuthUser(
                int userId,
                String name,
                String email,
                String phone,
                String role) {

            this.userId = userId;
            this.name = name;
            this.email = email;
            this.phone = phone;
            this.role = role;
        }

        public int getUserId() {
            return userId;
        }

        public String getName() {
            return name;
        }

        public String getEmail() {
            return email;
        }

        public String getPhone() {
            return phone;
        }

        public String getRole() {
            return role;
        }

        public boolean isAdmin() {
            return "ADMIN".equals(role);
        }

        public boolean isUser() {
            return "USER".equals(role);
        }
    }
}

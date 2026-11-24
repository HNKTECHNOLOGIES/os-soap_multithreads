package fr.evaloc.support;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.sql.Statement;

/**
 * Support pour tests d'intégration PostgreSQL (skip si DB indisponible).
 */
public final class DbTestSupport {

    public static final String URL = resolve(
            "evaloc.test.db.url",
            "EVALOC_TEST_DB_URL",
            "jdbc:postgresql://localhost:5432/gesloc");
    public static final String USER = resolve(
            "evaloc.test.db.username",
            "DB_USERNAME",
            "evaloc");
    public static final String PASSWORD = resolve(
            "evaloc.test.db.password",
            "DB_PASSWORD",
            "evaloc");

    private static String resolve(String sysProp, String envName, String defaultValue) {
        String fromSys = System.getProperty(sysProp);
        if (fromSys != null && !fromSys.trim().isEmpty() && !fromSys.contains("${")) {
            return fromSys.trim();
        }
        String fromEnv = System.getenv(envName);
        if (fromEnv != null && !fromEnv.trim().isEmpty()) {
            return fromEnv.trim();
        }
        return defaultValue;
    }

    private DbTestSupport() {
    }

    public static boolean isDatabaseAvailable() {
        try (Connection c = DriverManager.getConnection(URL, USER, PASSWORD);
             Statement st = c.createStatement()) {
            st.execute("SELECT 1");
            return true;
        } catch (SQLException e) {
            return false;
        }
    }
}

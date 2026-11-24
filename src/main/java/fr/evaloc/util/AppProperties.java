package fr.evaloc.util;

import java.io.IOException;
import java.io.InputStream;
import java.util.Objects;
import java.util.Properties;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Charge {@code application.properties} et résout les variables d'environnement
 * au format {@code ${ENV}} ou {@code ${ENV:default}}.
 */
public final class AppProperties {

    private static final Pattern ENV_PATTERN =
            Pattern.compile("\\$\\{([A-Za-z0-9_.]+)(?::([^}]*))?}");

    private final Properties properties;

    public AppProperties(Properties properties) {
        this.properties = Objects.requireNonNull(properties, "properties");
    }

    /**
     * Charge le fichier classpath {@code application.properties}.
     */
    public static AppProperties loadDefault() {
        return loadFromClasspath("application.properties");
    }

    public static AppProperties loadFromClasspath(String resourceName) {
        Properties props = new Properties();
        try (InputStream in = AppProperties.class.getClassLoader().getResourceAsStream(resourceName)) {
            if (in == null) {
                throw new IllegalStateException("Ressource introuvable: " + resourceName);
            }
            props.load(in);
        } catch (IOException e) {
            throw new IllegalStateException("Impossible de charger " + resourceName, e);
        }
        return new AppProperties(props);
    }

    public String get(String key) {
        String raw = properties.getProperty(key);
        if (raw == null) {
            return null;
        }
        return resolve(raw);
    }

    public String getRequired(String key) {
        String value = get(key);
        if (value == null || value.trim().isEmpty()) {
            throw new IllegalStateException("Propriété obligatoire manquante: " + key);
        }
        return value;
    }

    public String get(String key, String defaultValue) {
        String value = get(key);
        return value == null ? defaultValue : value;
    }

    public int getInt(String key, int defaultValue) {
        String value = get(key);
        if (value == null || value.trim().isEmpty()) {
            return defaultValue;
        }
        return Integer.parseInt(value.trim());
    }

    public long getLong(String key, long defaultValue) {
        String value = get(key);
        if (value == null || value.trim().isEmpty()) {
            return defaultValue;
        }
        return Long.parseLong(value.trim());
    }

    public boolean getBoolean(String key, boolean defaultValue) {
        String value = get(key);
        if (value == null || value.trim().isEmpty()) {
            return defaultValue;
        }
        return Boolean.parseBoolean(value.trim());
    }

    /**
     * Résout récursivement les placeholders {@code ${ENV}} et {@code ${ENV:default}}.
     */
    static String resolve(String raw) {
        Matcher matcher = ENV_PATTERN.matcher(raw);
        StringBuffer sb = new StringBuffer();
        while (matcher.find()) {
            String envName = matcher.group(1);
            String defaultValue = matcher.group(2);
            String envValue = System.getenv(envName);
            if (envValue == null) {
                envValue = System.getProperty(envName);
            }
            String replacement = envValue != null ? envValue
                    : (defaultValue != null ? defaultValue : "");
            matcher.appendReplacement(sb, Matcher.quoteReplacement(replacement));
        }
        matcher.appendTail(sb);
        return sb.toString();
    }
}

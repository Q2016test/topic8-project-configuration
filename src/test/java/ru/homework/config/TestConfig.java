package ru.homework.config;

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.math.BigDecimal;
import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Locale;
import java.util.Properties;

public record TestConfig(String uiUrl, String apiUrl, long elementTimeoutMs,
                         LoggingMode loggingMode, Credentials admin,
                         String productName, BigDecimal productPrice) {
    public static TestConfig load() {
        Properties properties = new Properties();
        String path = System.getProperty("test.config");
        try (Reader reader = path == null ? defaultReader()
                : Files.newBufferedReader(Path.of(path), StandardCharsets.UTF_8)) {
            properties.load(reader);
        } catch (IOException e) {
            throw new IllegalStateException("Не удалось прочитать конфигурацию", e);
        }
        long timeout = Long.parseLong(required(properties, "element.timeout.ms").trim());
        if (timeout <= 0) {
            throw new IllegalArgumentException("element.timeout.ms должен быть положительным");
        }
        BigDecimal price = new BigDecimal(required(properties, "product.price").trim());
        if (price.signum() < 0) {
            throw new IllegalArgumentException("product.price не может быть отрицательной");
        }
        return new TestConfig(
                httpUrl(properties, "ui.url"),
                httpUrl(properties, "api.url"),
                timeout,
                LoggingMode.valueOf(required(properties, "logging.mode").trim().toUpperCase(Locale.ROOT)),
                new Credentials(required(properties, "admin.login"), required(properties, "admin.password")),
                required(properties, "product.name"),
                price);
    }

    private static Reader defaultReader() {
        InputStream stream = TestConfig.class.getResourceAsStream("/test.properties");
        if (stream == null) {
            throw new IllegalStateException("Не найден test.properties");
        }
        return new InputStreamReader(stream, StandardCharsets.UTF_8);
    }

    private static String required(Properties properties, String key) {
        String value = properties.getProperty(key);
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException("Не задан параметр " + key);
        }
        return value;
    }

    private static String httpUrl(Properties properties, String key) {
        String value = required(properties, key).trim();
        URI uri = URI.create(value);
        if (!("http".equalsIgnoreCase(uri.getScheme()) || "https".equalsIgnoreCase(uri.getScheme()))
                || uri.getHost() == null || uri.getUserInfo() != null) {
            throw new IllegalArgumentException(key + " должен содержать HTTP(S)-URL без Credentials");
        }
        return value;
    }

    public void printPublicValues() {
        System.out.println("ui.url=" + uiUrl);
        System.out.println("api.url=" + apiUrl);
        System.out.println("element.timeout.ms=" + elementTimeoutMs);
        System.out.println("logging.mode=" + loggingMode);
        System.out.println("product.name=" + productName);
        System.out.println("product.price=" + productPrice.toPlainString());
    }

    public String priceText() {
        return productPrice.stripTrailingZeros().toPlainString();
    }

    public enum LoggingMode {
        OFF, INFO, DEBUG
    }

    public record Credentials(String login, String password) {
        @Override
        public String toString() {
            return "Credentials[hidden]";
        }
    }
}

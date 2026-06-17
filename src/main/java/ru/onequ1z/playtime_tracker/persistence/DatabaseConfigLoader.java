package ru.onequ1z.playtime_tracker.persistence;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Properties;

public class DatabaseConfigLoader {
    private static final String DEFAULT_CONFIG = """
    host=localhost
    port=5432
    database=playtime_tracker
    username=postgres
    password=change_me
    """;

    private static final Path CONFIG_DIRECTORY =
            Paths.get("config", "playtime_tracker");

    private static final Path CONFIG_FILE =
            CONFIG_DIRECTORY.resolve("database.properties");
    public DatabaseProperties load() {
        try {
            createDefaultConfigIfMissing();
            return loadProperties();
        } catch (IOException e) {
            throw new RuntimeException("Failed to load database configuration", e);
        }
    }
    private void createDefaultConfigIfMissing() throws IOException {
        if (Files.exists(CONFIG_FILE)) {
            return;
        }
        Files.createDirectories(CONFIG_DIRECTORY);
        Files.writeString(CONFIG_FILE, DEFAULT_CONFIG);

    }
    private DatabaseProperties loadProperties() {
        Properties properties = new Properties();
        try (InputStream inputStream = Files.newInputStream(CONFIG_FILE)) {
            properties.load(inputStream);
            String host = properties.getProperty("host");
            int port = Integer.parseInt(properties.getProperty("port"));
            String database = properties.getProperty("database");
            String username = properties.getProperty("username");
            String password = properties.getProperty("password");
            return new DatabaseProperties(host, port, database, username, password);

        } catch (IOException | NumberFormatException e) {
            throw new RuntimeException("Failed to load database configuration", e);
        }
    }
}

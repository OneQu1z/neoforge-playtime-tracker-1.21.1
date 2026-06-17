package ru.onequ1z.playtime_tracker.persistence;

import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;
import ru.onequ1z.playtime_tracker.PlayTimeTrackerMod;

import java.sql.Connection;
import java.sql.SQLException;

public class ConnectionManager {
    private final HikariDataSource dataSource;

    public ConnectionManager(DatabaseProperties properties) {
        HikariConfig config = new HikariConfig();
        PlayTimeTrackerMod.LOGGER.info(
                "Connecting to {}",
                properties.getJdbcUrl()
        );
        config.setJdbcUrl(properties.getJdbcUrl());
        config.setUsername(properties.getUsername());
        config.setPassword(properties.getPassword());

        this.dataSource = new HikariDataSource(config);
        try (Connection ignored = dataSource.getConnection()) {
            PlayTimeTrackerMod.LOGGER.info("Connected to PostgreSQL");
        } catch (SQLException e) {
            throw new RuntimeException("Failed to connect to PostgreSQL", e);
        }
    }

    public Connection getConnection() throws SQLException {
        return dataSource.getConnection();
    }

    public void close() {
        dataSource.close();
    }
}



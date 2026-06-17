package ru.onequ1z.playtime_tracker.persistence;

public class DatabaseProperties {
    private final String host;
    private final int port;
    private final String database;
    private final String username;
    private final String password;

    public DatabaseProperties(
            String host,
            int port,
            String database,
            String username,
            String password
    ) {
        this.host = host;
        this.port = port;
        this.database = database;
        this.username = username;
        this.password = password;
    }

    public String getHost() {
        return host;
    }

    public int getPort() {
        return port;
    }

    public String getDatabase() {
        return database;
    }

    public String getUsername() {
        return username;
    }

    public String getPassword() {
        return password;
    }

    public String getJdbcUrl() {
        return String.format(
                "jdbc:postgresql://%s:%d/%s",
                host,
                port,
                database
        );
    }
}

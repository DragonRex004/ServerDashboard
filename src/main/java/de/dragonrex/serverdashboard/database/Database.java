package de.dragonrex.serverdashboard.database;

import com.zaxxer.hikari.HikariConfig;
import lombok.Getter;

import java.util.Optional;

@Getter
public class Database {
    private final String jdbcUrl;
    private final Optional<String> username;
    private final Optional<String> password;
    private final Optional<Boolean> noSQL;
    private HikariConfig config;

    public Database(String jdbcUrl, Optional<String> username, Optional<String> password, Optional<Boolean> noSQL) {
        this.jdbcUrl = jdbcUrl;
        this.username = username;
        this.password = password;
        this.noSQL = noSQL;
    }

    public Database(String jdbcUrl) {
        this(jdbcUrl, Optional.empty(), Optional.empty(), Optional.empty());
    }

    public void configure() {
        this.config = new HikariConfig();
        config.setConnectionTimeout(10000);
        config.setMaximumPoolSize(8);
        config.setMinimumIdle(1);
        config.setJdbcUrl(this.jdbcUrl);
        this.username.ifPresent(username -> config.setUsername(username));
        this.password.ifPresent(password -> config.setUsername(password));
    }
}

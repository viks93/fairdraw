package com.fairdraw;

import com.zaxxer.hikari.HikariDataSource;
import java.net.URI;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import javax.sql.DataSource;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;

@Configuration
public class DataSourceConfig {

    private static final Logger log = LoggerFactory.getLogger(DataSourceConfig.class);

    @Bean
    @Primary
    public DataSource dataSource() {
        String databaseUrl = System.getenv("DATABASE_URL");
        if (databaseUrl == null || databaseUrl.isBlank()) {
            return h2DataSource();
        }
        String trimmed = databaseUrl.trim();
        if (trimmed.startsWith("jdbc:")) {
            log.info("Using PostgreSQL from DATABASE_URL (JDBC)");
            return jdbcUrlDataSource(trimmed);
        }
        if (trimmed.startsWith("postgres://") || trimmed.startsWith("postgresql://")) {
            log.info("Using PostgreSQL from DATABASE_URL (URI)");
            return postgresUriDataSource(trimmed);
        }
        throw new IllegalStateException(
                "DATABASE_URL must be a postgres:// or postgresql:// URI, or a full jdbc: URL");
    }

    /**
     * File-backed H2 so restarts keep room data when DATABASE_URL is not set (local dev). For
     * production (e.g. Render), set DATABASE_URL to Postgres (e.g. Neon); the filesystem there is
     * usually ephemeral anyway.
     */
    private static DataSource h2DataSource() {
        try {
            Path dir = Path.of(System.getProperty("user.home"), ".fairdraw");
            Files.createDirectories(dir);
            Path dbFile = dir.resolve("fairdraw");
            String pathForH2 = dbFile.toAbsolutePath().normalize().toString().replace('\\', '/');
            String jdbcUrl =
                    "jdbc:h2:file:"
                            + pathForH2
                            + ";DB_CLOSE_DELAY=-1;DATABASE_TO_LOWER=TRUE";
            log.info("Using file-backed H2 at {} (set DATABASE_URL for Postgres in production)", dbFile);
            HikariDataSource ds = new HikariDataSource();
            ds.setJdbcUrl(jdbcUrl);
            ds.setUsername("sa");
            ds.setPassword("");
            ds.setDriverClassName("org.h2.Driver");
            return ds;
        } catch (Exception e) {
            throw new IllegalStateException("Could not create H2 file database under ~/.fairdraw", e);
        }
    }

    private static DataSource jdbcUrlDataSource(String jdbcUrl) {
        String user = envOrNull("SPRING_DATASOURCE_USERNAME");
        String pass = envOrNull("SPRING_DATASOURCE_PASSWORD");
        HikariDataSource ds = new HikariDataSource();
        ds.setJdbcUrl(jdbcUrl);
        if (user != null) {
            ds.setUsername(user);
        }
        if (pass != null) {
            ds.setPassword(pass);
        }
        ds.setDriverClassName("org.postgresql.Driver");
        return ds;
    }

    private static String envOrNull(String key) {
        String v = System.getenv(key);
        return v == null || v.isBlank() ? null : v;
    }

    private static DataSource postgresUriDataSource(String databaseUrl) {
        try {
            URI uri = URI.create(databaseUrl.replace("postgres://", "postgresql://"));
            String host = uri.getHost();
            int port = uri.getPort() == -1 ? 5432 : uri.getPort();
            String path = uri.getPath();
            if (path == null || path.length() <= 1) {
                throw new IllegalArgumentException("Missing database name in DATABASE_URL path");
            }
            String database = path.substring(1);

            String userInfo = uri.getUserInfo();
            if (userInfo == null || userInfo.isEmpty()) {
                throw new IllegalArgumentException("Missing user info in DATABASE_URL");
            }
            int colon = userInfo.indexOf(':');
            String user;
            String password;
            if (colon >= 0) {
                user = URLDecoder.decode(userInfo.substring(0, colon), StandardCharsets.UTF_8);
                password =
                        URLDecoder.decode(userInfo.substring(colon + 1), StandardCharsets.UTF_8);
            } else {
                user = URLDecoder.decode(userInfo, StandardCharsets.UTF_8);
                password = "";
            }

            StringBuilder jdbc = new StringBuilder();
            jdbc.append("jdbc:postgresql://").append(host).append(":").append(port).append("/")
                    .append(database);
            if (uri.getQuery() != null && !uri.getQuery().isEmpty()) {
                jdbc.append("?").append(uri.getQuery());
            } else {
                jdbc.append("?sslmode=require");
            }

            HikariDataSource ds = new HikariDataSource();
            ds.setJdbcUrl(jdbc.toString());
            ds.setUsername(user);
            ds.setPassword(password);
            ds.setDriverClassName("org.postgresql.Driver");
            return ds;
        } catch (RuntimeException e) {
            throw new IllegalStateException("Invalid DATABASE_URL", e);
        }
    }
}

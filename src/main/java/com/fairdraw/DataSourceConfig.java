package com.fairdraw;

import com.zaxxer.hikari.HikariDataSource;
import java.net.URI;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import javax.sql.DataSource;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;

@Configuration
public class DataSourceConfig {

    @Bean
    @Primary
    public DataSource dataSource() {
        String databaseUrl = System.getenv("DATABASE_URL");
        if (databaseUrl == null || databaseUrl.isBlank()) {
            return h2DataSource();
        }
        String trimmed = databaseUrl.trim();
        if (trimmed.startsWith("jdbc:")) {
            return jdbcUrlDataSource(trimmed);
        }
        if (trimmed.startsWith("postgres://") || trimmed.startsWith("postgresql://")) {
            return postgresUriDataSource(trimmed);
        }
        throw new IllegalStateException(
                "DATABASE_URL must be a postgres:// or postgresql:// URI, or a full jdbc: URL");
    }

    private static DataSource h2DataSource() {
        HikariDataSource ds = new HikariDataSource();
        ds.setJdbcUrl("jdbc:h2:mem:fairdraw;DB_CLOSE_DELAY=-1;DATABASE_TO_LOWER=TRUE");
        ds.setUsername("sa");
        ds.setPassword("");
        ds.setDriverClassName("org.h2.Driver");
        return ds;
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

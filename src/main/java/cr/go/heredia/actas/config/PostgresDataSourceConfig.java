package cr.go.heredia.actas.config;

import org.springframework.boot.autoconfigure.jdbc.DataSourceProperties;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;
import org.springframework.context.annotation.Profile;

import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;

/**
 * Creates the {@code actas_ambiental} database on first startup if it does not exist yet.
 */
@Configuration
@Profile("postgres & !docker")
public class PostgresDataSourceConfig {

    @Bean
    @Primary
    @ConfigurationProperties("spring.datasource")
    public DataSourceProperties dataSourceProperties() {
        return new DataSourceProperties();
    }

    @Bean
    @Primary
    public DataSource dataSource(DataSourceProperties properties) throws SQLException {
        ensureDatabaseExists(properties);
        return properties.initializeDataSourceBuilder().build();
    }

    private void ensureDatabaseExists(DataSourceProperties properties) throws SQLException {
        String url = properties.getUrl();
        String dbName = extractDatabaseName(url);
        String adminUrl = url.substring(0, url.lastIndexOf('/')) + "/postgres";

        try (Connection conn = DriverManager.getConnection(
                adminUrl, properties.getUsername(), properties.getPassword())) {
            boolean exists;
            try (var ps = conn.prepareStatement("SELECT 1 FROM pg_database WHERE datname = ?")) {
                ps.setString(1, dbName);
                try (ResultSet rs = ps.executeQuery()) {
                    exists = rs.next();
                }
            }
            if (!exists) {
                try (Statement stmt = conn.createStatement()) {
                    stmt.executeUpdate("CREATE DATABASE \"" + dbName + "\"");
                }
            }
        }
    }

    private String extractDatabaseName(String url) {
        String path = url.substring(url.lastIndexOf('/') + 1);
        int queryIndex = path.indexOf('?');
        return queryIndex >= 0 ? path.substring(0, queryIndex) : path;
    }
}

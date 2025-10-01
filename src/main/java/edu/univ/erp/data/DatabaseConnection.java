package edu.univ.erp.data;

import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;
import edu.univ.erp.util.ConfigUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.sql.Connection;
import java.sql.SQLException;

/**
 * Database connection manager using HikariCP connection pooling.
 */
public class DatabaseConnection {
    private static final Logger logger = LoggerFactory.getLogger(DatabaseConnection.class);

    private static HikariDataSource authDataSource;
    private static HikariDataSource erpDataSource;

    static {
        initializeDataSources();
    }

    private static void initializeDataSources() {
        try {
            // Auth DB Connection Pool
            HikariConfig authConfig = new HikariConfig();
            authConfig.setJdbcUrl(ConfigUtil.getAuthDbUrl());
            authConfig.setUsername(ConfigUtil.getAuthDbUsername());
            authConfig.setPassword(ConfigUtil.getAuthDbPassword());
            authConfig.setMaximumPoolSize(ConfigUtil.getIntProperty("db.pool.size", 10));
            authConfig.setConnectionTimeout(ConfigUtil.getIntProperty("db.pool.connectionTimeout", 30000));
            authConfig.setPoolName("AuthDB-Pool");
            authDataSource = new HikariDataSource(authConfig);
            logger.info("Auth database connection pool initialized");

            // ERP DB Connection Pool
            HikariConfig erpConfig = new HikariConfig();
            erpConfig.setJdbcUrl(ConfigUtil.getErpDbUrl());
            erpConfig.setUsername(ConfigUtil.getErpDbUsername());
            erpConfig.setPassword(ConfigUtil.getErpDbPassword());
            erpConfig.setMaximumPoolSize(ConfigUtil.getIntProperty("db.pool.size", 10));
            erpConfig.setConnectionTimeout(ConfigUtil.getIntProperty("db.pool.connectionTimeout", 30000));
            erpConfig.setPoolName("ErpDB-Pool");
            erpDataSource = new HikariDataSource(erpConfig);
            logger.info("ERP database connection pool initialized");

        } catch (Exception e) {
            logger.error("Failed to initialize database connection pools", e);
            throw new RuntimeException("Database initialization failed", e);
        }
    }

    /**
     * Get connection to Auth DB (for authentication).
     */
    public static Connection getAuthConnection() throws SQLException {
        return authDataSource.getConnection();
    }

    /**
     * Get connection to ERP DB (for main data).
     */
    public static Connection getErpConnection() throws SQLException {
        return erpDataSource.getConnection();
    }

    /**
     * Close all connection pools.
     */
    public static void closeAll() {
        if (authDataSource != null && !authDataSource.isClosed()) {
            authDataSource.close();
            logger.info("Auth database connection pool closed");
        }
        if (erpDataSource != null && !erpDataSource.isClosed()) {
            erpDataSource.close();
            logger.info("ERP database connection pool closed");
        }
    }

    /**
     * Test database connections.
     */
    public static boolean testConnections() {
        try (Connection authConn = getAuthConnection();
             Connection erpConn = getErpConnection()) {
            return authConn != null && erpConn != null;
        } catch (SQLException e) {
            logger.error("Database connection test failed", e);
            return false;
        }
    }
}

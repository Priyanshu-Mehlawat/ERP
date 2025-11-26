package edu.univ.erp.test;

import edu.univ.erp.data.DatabaseConnection;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.AfterEach;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.sql.Connection;
import java.sql.SQLException;
import java.sql.Statement;

/**
 * Base class for DAO unit tests providing database setup and cleanup.
 */
public abstract class BaseDAOTest {
    private static final Logger logger = LoggerFactory.getLogger(BaseDAOTest.class);

    @BeforeAll
    static void setUpDatabase() {
        // Verify database connections are available
        if (!DatabaseConnection.testConnections()) {
            throw new RuntimeException("Database connections not available. Please ensure MySQL is running and databases are created.");
        }
        logger.info("Database connections verified for testing");
    }

    @BeforeEach
    void setUp() {
        // Each test starts with a clean state
        // Note: In a real testing environment, we would use a separate test database
        // or transactions that are rolled back after each test
        logger.debug("Setting up test case");
    }

    @AfterEach
    void tearDown() {
        // Clean up any test data
        logger.debug("Tearing down test case");
    }

    /**
     * Helper method to execute cleanup SQL statements.
     * 
     * @param sql The SQL statement to execute
     */
    protected void executeCleanupSQL(String sql) {
        try (Connection conn = DatabaseConnection.getErpConnection();
             Statement stmt = conn.createStatement()) {
            stmt.execute(sql);
        } catch (SQLException e) {
            logger.warn("Cleanup SQL failed (may be expected): {}", e.getMessage());
        }
    }

    /**
     * Helper method to execute cleanup SQL on auth database.
     * 
     * @param sql The SQL statement to execute
     */
    protected void executeAuthCleanupSQL(String sql) {
        try (Connection conn = DatabaseConnection.getAuthConnection();
             Statement stmt = conn.createStatement()) {
            stmt.execute(sql);
        } catch (SQLException e) {
            logger.warn("Auth cleanup SQL failed (may be expected): {}", e.getMessage());
        }
    }
}
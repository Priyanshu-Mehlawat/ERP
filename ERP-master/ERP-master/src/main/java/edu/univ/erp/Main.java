package edu.univ.erp;

import com.formdev.flatlaf.FlatLightLaf;
import edu.univ.erp.ui.auth.LoginFrame;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.swing.*;

/**
 * Main entry point for the University ERP System.
 */
public class Main {
    private static final Logger logger = LoggerFactory.getLogger(Main.class);

    public static void main(String[] args) {
        logger.info("Starting University ERP System...");

        // Set the FlatLaf Look and Feel
        try {
            UIManager.setLookAndFeel(new FlatLightLaf());
            logger.info("FlatLaf Look and Feel applied successfully");
        } catch (UnsupportedLookAndFeelException e) {
            logger.warn("Failed to apply FlatLaf, using default Look and Feel", e);
        }

        // Initialize the application on the Event Dispatch Thread
        SwingUtilities.invokeLater(() -> {
            try {
                LoginFrame loginFrame = new LoginFrame();
                loginFrame.setVisible(true);
                logger.info("Login frame displayed");
            } catch (Exception e) {
                logger.error("Failed to start application", e);
                JOptionPane.showMessageDialog(null,
                        "Failed to start application: " + e.getMessage(),
                        "Error",
                        JOptionPane.ERROR_MESSAGE);
                System.exit(1);
            }
        });
    }
}

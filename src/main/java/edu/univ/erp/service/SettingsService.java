package edu.univ.erp.service;

import edu.univ.erp.data.SettingsDAO;
import edu.univ.erp.domain.Settings;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Map;

public class SettingsService {
    private static final Logger logger = LoggerFactory.getLogger(SettingsService.class);
    private final SettingsDAO settingsDAO = new SettingsDAO();

    public Map<String,String> all() { return settingsDAO.findAll(); }
    public String get(String key) { return settingsDAO.get(key); }
    public boolean set(String key, String value) { return settingsDAO.upsert(key, value); }

    /**
     * Check if the system is in maintenance mode.
     * 
     * @return true if maintenance mode is enabled, false otherwise
     */
    public boolean isMaintenanceMode() { 
        try {
            Settings settings = settingsDAO.getSettings();
            return settings.isMaintenanceMode();
        } catch (Exception e) {
            logger.warn("Error checking maintenance mode, defaulting to false", e);
            return false;
        }
    }

    /**
     * Check if student registration is enabled.
     * 
     * @return true if registration is enabled, false otherwise
     */
    public boolean isRegistrationEnabled() {
        try {
            Settings settings = settingsDAO.getSettings();
            return settings.isRegistrationEnabled();
        } catch (Exception e) {
            logger.warn("Error checking registration status, defaulting to true", e);
            return true;
        }
    }

    /**
     * Get all system settings as a Settings object.
     * 
     * @return Settings object with all system settings
     */
    public Settings getSettings() {
        return settingsDAO.getSettings();
    }

    /**
     * Update all system settings.
     * 
     * @param settings Settings object with updated values
     */
    public void updateSettings(Settings settings) {
        settingsDAO.updateSettings(settings);
    }
}

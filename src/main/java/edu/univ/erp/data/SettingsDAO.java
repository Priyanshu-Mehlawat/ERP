package edu.univ.erp.data;

import edu.univ.erp.domain.Settings;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.sql.*;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.HashMap;
import java.util.Map;

public class SettingsDAO {
    private static final Logger logger = LoggerFactory.getLogger(SettingsDAO.class);
    
    private static final DateTimeFormatter DATETIME_FORMAT = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    public Map<String,String> findAll() {
        Map<String,String> map = new HashMap<>();
        String sql = "SELECT setting_key, setting_value FROM settings";
        try (Connection conn = DatabaseConnection.getErpConnection(); 
             Statement st = conn.createStatement(); 
             ResultSet rs = st.executeQuery(sql)) {
            while (rs.next()) {
                map.put(rs.getString(1), rs.getString(2));
            }
        } catch (SQLException e) {
            logger.error("Error loading settings", e);
        }
        return map;
    }

    public String get(String key) {
        String sql = "SELECT setting_value FROM settings WHERE setting_key = ?";
        try (Connection conn = DatabaseConnection.getErpConnection(); 
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, key);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    return rs.getString(1);
                }
            }
        } catch (SQLException e) {
            logger.error("Error getting setting {}", key, e);
        }
        return null;
    }

    public boolean upsert(String key, String value) {
        String sql = "INSERT INTO settings (setting_key, setting_value) VALUES (?, ?) " +
                     "ON DUPLICATE KEY UPDATE setting_value = VALUES(setting_value)";
        try (Connection conn = DatabaseConnection.getErpConnection(); 
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, key);
            ps.setString(2, value);
            return ps.executeUpdate() >= 1;
        } catch (SQLException e) {
            logger.error("Error upserting setting {}", key, e);
        }
        return false;
    }
    
    /**
     * Get all settings as a Settings object with convenience fields
     */
    public Settings getSettings() {
        Map<String, String> map = findAll();
        Settings settings = new Settings();
        
        // Parse individual settings
        settings.setMaintenanceMode("true".equalsIgnoreCase(map.get("maintenance_mode")));
        settings.setRegistrationEnabled(!"false".equalsIgnoreCase(map.get("registration_enabled")));
        settings.setCurrentSemester(map.getOrDefault("current_semester", "Fall"));
        
        String yearStr = map.get("current_year");
        settings.setCurrentYear(yearStr != null ? Integer.parseInt(yearStr) : 2025);
        
        String addDropStr = map.get("add_drop_deadline");
        if (addDropStr != null && !addDropStr.isEmpty()) {
            try {
                settings.setAddDropDeadline(LocalDateTime.parse(addDropStr, DATETIME_FORMAT));
            } catch (Exception e) {
                logger.warn("Failed to parse add_drop_deadline: {}", addDropStr, e);
            }
        }
        
        String withdrawalStr = map.get("withdrawal_deadline");
        if (withdrawalStr != null && !withdrawalStr.isEmpty()) {
            try {
                settings.setWithdrawalDeadline(LocalDateTime.parse(withdrawalStr, DATETIME_FORMAT));
            } catch (Exception e) {
                logger.warn("Failed to parse withdrawal_deadline: {}", withdrawalStr, e);
            }
        }
        
        settings.setAnnouncement(map.get("announcement"));
        
        return settings;
    }
    
    /**
     * Update all settings from a Settings object
     */
    public void updateSettings(Settings settings) {
        upsert("maintenance_mode", String.valueOf(settings.isMaintenanceMode()));
        upsert("registration_enabled", String.valueOf(settings.isRegistrationEnabled()));
        upsert("current_semester", settings.getCurrentSemester());
        upsert("current_year", String.valueOf(settings.getCurrentYear()));
        
        if (settings.getAddDropDeadline() != null) {
            upsert("add_drop_deadline", settings.getAddDropDeadline().format(DATETIME_FORMAT));
        } else {
            upsert("add_drop_deadline", "");
        }
        
        if (settings.getWithdrawalDeadline() != null) {
            upsert("withdrawal_deadline", settings.getWithdrawalDeadline().format(DATETIME_FORMAT));
        } else {
            upsert("withdrawal_deadline", "");
        }
        
        if (settings.getAnnouncement() != null) {
            upsert("announcement", settings.getAnnouncement());
        } else {
            upsert("announcement", "");
        }
    }
}

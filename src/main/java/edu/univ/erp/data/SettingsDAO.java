package edu.univ.erp.data;

import org.slf4j.Logger;import org.slf4j.LoggerFactory;

import java.sql.*;import java.util.HashMap;import java.util.Map;

public class SettingsDAO {
    private static final Logger logger = LoggerFactory.getLogger(SettingsDAO.class);

    public Map<String,String> findAll() {
        Map<String,String> map = new HashMap<>();
        String sql = "SELECT setting_key, setting_value FROM settings";
        try (Connection conn = DatabaseConnection.getErpConnection(); Statement st = conn.createStatement(); ResultSet rs = st.executeQuery(sql)) {
            while (rs.next()) map.put(rs.getString(1), rs.getString(2));
        } catch (SQLException e) { logger.error("Error loading settings", e);} return map;
    }

    public String get(String key) {
        String sql = "SELECT setting_value FROM settings WHERE setting_key = ?";
        try (Connection conn = DatabaseConnection.getErpConnection(); PreparedStatement ps = conn.prepareStatement(sql)) { ps.setString(1, key); try (ResultSet rs = ps.executeQuery()) { if (rs.next()) return rs.getString(1);} } catch (SQLException e) { logger.error("Error getting setting {}", key, e);} return null;
    }

    public boolean upsert(String key, String value) {
        String sql = "INSERT INTO settings (setting_key, setting_value) VALUES (?, ?) ON DUPLICATE KEY UPDATE setting_value = VALUES(setting_value)";
        try (Connection conn = DatabaseConnection.getErpConnection(); PreparedStatement ps = conn.prepareStatement(sql)) { ps.setString(1, key); ps.setString(2, value); return ps.executeUpdate() >= 1; } catch (SQLException e) { logger.error("Error upserting setting {}", key, e);} return false;
    }
}

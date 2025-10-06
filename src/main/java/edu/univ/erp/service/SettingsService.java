package edu.univ.erp.service;

import edu.univ.erp.data.SettingsDAO;

import java.util.Map;

public class SettingsService {
    private final SettingsDAO settingsDAO = new SettingsDAO();

    public Map<String,String> all() { return settingsDAO.findAll(); }
    public String get(String key) { return settingsDAO.get(key); }
    public boolean set(String key, String value) { return settingsDAO.upsert(key, value); }

    public boolean isMaintenanceMode() { return "ON".equalsIgnoreCase(get("maintenance_mode")); }
}

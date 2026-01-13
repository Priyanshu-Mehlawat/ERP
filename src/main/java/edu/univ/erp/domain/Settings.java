package edu.univ.erp.domain;

import java.time.LocalDateTime;

/**
 * Settings entity for system configuration.
 */
public class Settings {
    private String key;
    private String value;
    
    // Convenience fields for structured settings
    private boolean maintenanceMode;
    private boolean registrationEnabled;
    private String currentSemester;
    private int currentYear;
    private LocalDateTime addDropDeadline;
    private LocalDateTime withdrawalDeadline;
    private String announcement;

    public Settings() {
    }

    public Settings(String key, String value) {
        this.key = key;
        this.value = value;
    }

    // Getters and Setters
    public String getKey() {
        return key;
    }

    public void setKey(String key) {
        this.key = key;
    }

    public String getValue() {
        return value;
    }

    public void setValue(String value) {
        this.value = value;
    }
    
    public boolean isMaintenanceMode() {
        return maintenanceMode;
    }
    
    public void setMaintenanceMode(boolean maintenanceMode) {
        this.maintenanceMode = maintenanceMode;
    }
    
    public boolean isRegistrationEnabled() {
        return registrationEnabled;
    }
    
    public void setRegistrationEnabled(boolean registrationEnabled) {
        this.registrationEnabled = registrationEnabled;
    }
    
    public String getCurrentSemester() {
        return currentSemester;
    }
    
    public void setCurrentSemester(String currentSemester) {
        this.currentSemester = currentSemester;
    }
    
    public int getCurrentYear() {
        return currentYear;
    }
    
    public void setCurrentYear(int currentYear) {
        this.currentYear = currentYear;
    }
    
    public LocalDateTime getAddDropDeadline() {
        return addDropDeadline;
    }
    
    public void setAddDropDeadline(LocalDateTime addDropDeadline) {
        this.addDropDeadline = addDropDeadline;
    }
    
    public LocalDateTime getWithdrawalDeadline() {
        return withdrawalDeadline;
    }
    
    public void setWithdrawalDeadline(LocalDateTime withdrawalDeadline) {
        this.withdrawalDeadline = withdrawalDeadline;
    }
    
    public String getAnnouncement() {
        return announcement;
    }
    
    public void setAnnouncement(String announcement) {
        this.announcement = announcement;
    }

    @Override
    public String toString() {
        return "Settings{" +
                "key='" + key + '\'' +
                ", value='" + value + '\'' +
                '}';
    }
}

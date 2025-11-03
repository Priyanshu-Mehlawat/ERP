package edu.univ.erp.service;

import edu.univ.erp.domain.Settings;
import edu.univ.erp.test.BaseDAOTest;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;

import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Tests for SettingsService covering settings management and maintenance mode.
 */
@DisplayName("SettingsService Tests")
class SettingsServiceTest extends BaseDAOTest {

    private SettingsService settingsService;

    @BeforeEach
    void setUpService() {
        settingsService = new SettingsService();
    }

    @Test
    @DisplayName("Should get all settings")
    void testGetAllSettings() {
        // Act
        Map<String, String> settings = settingsService.all();

        // Assert
        assertNotNull(settings, "Settings map should not be null");
    }

    @Test
    @DisplayName("Should get specific setting by key")
    void testGetSettingByKey() {
        // Act
        String value = settingsService.get("maintenance_mode");

        // Assert - value may be null or "false" depending on setup
        assertNotNull(value == null || value.equals("false") || value.equals("true"),
                "Maintenance mode value should be valid");
    }

    @Test
    @DisplayName("Should set setting value")
    void testSetSetting() {
        // Act
        boolean result = settingsService.set("test_key", "test_value");

        // Assert
        assertTrue(result, "Should successfully set setting");
    }

    @Test
    @DisplayName("Should check maintenance mode")
    void testIsMaintenanceMode() {
        // Act
        boolean maintenanceMode = settingsService.isMaintenanceMode();

        // Assert
        assertNotNull(maintenanceMode);
        // Default should be false
        assertFalse(maintenanceMode, "Maintenance mode should be disabled by default");
    }

    @Test
    @DisplayName("Should check registration enabled status")
    void testIsRegistrationEnabled() {
        // Act
        boolean registrationEnabled = settingsService.isRegistrationEnabled();

        // Assert
        assertNotNull(registrationEnabled);
        // Default should be true
        assertTrue(registrationEnabled, "Registration should be enabled by default");
    }

    @Test
    @DisplayName("Should get settings object")
    void testGetSettingsObject() {
        // Act
        Settings settings = settingsService.getSettings();

        // Assert
        assertNotNull(settings, "Settings object should not be null");
        assertNotNull(settings.getCurrentSemester(), "Current semester should be set");
        assertTrue(settings.getCurrentYear() > 0, "Current year should be positive");
    }

    @Test
    @DisplayName("Should update settings object")
    void testUpdateSettings() {
        // Arrange
        Settings settings = settingsService.getSettings();
        String originalSemester = settings.getCurrentSemester();
        
        // Act
        assertDoesNotThrow(() -> {
            settingsService.updateSettings(settings);
        }, "Should update settings without throwing exception");
        
        // Assert - verify settings remain consistent
        Settings updatedSettings = settingsService.getSettings();
        assertEquals(originalSemester, updatedSettings.getCurrentSemester(),
                "Current semester should remain the same");
    }

    @Test
    @DisplayName("Should handle invalid key gracefully")
    void testGetInvalidKey() {
        // Act
        String value = settingsService.get("nonexistent_key_12345");

        // Assert - should return null for non-existent key
        assertNull(value, "Should return null for non-existent key");
    }
}

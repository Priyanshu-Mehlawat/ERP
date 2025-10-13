package edu.univ.erp.data;

import edu.univ.erp.domain.Settings;
import edu.univ.erp.test.BaseDAOTest;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Order;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

@DisplayName("SettingsDAO Tests")
class SettingsDAOTest extends BaseDAOTest {

    @Test
    @Order(1)
    @DisplayName("Upsert and get a single setting")
    void testUpsertAndGet() {
        SettingsDAO dao = new SettingsDAO();
        String key = "test_key";
        String value = "test_value";

        boolean upserted = dao.upsert(key, value);
        assertTrue(upserted, "Upsert should return true");

        String fetched = dao.get(key);
        assertEquals(value, fetched);

        // cleanup
        executeCleanupSQL("DELETE FROM settings WHERE setting_key = '" + key + "'");
    }

    @Test
    @Order(2)
    @DisplayName("Find all returns a map")
    void testFindAll() {
        SettingsDAO dao = new SettingsDAO();
        Map<String,String> all = dao.findAll();
        assertNotNull(all);
        // No hard assertions on contents since it depends on seed, just ensure call works
    }

    @Test
    @Order(3)
    @DisplayName("Update and retrieve Settings object")
    void testUpdateAndGetSettings() {
        SettingsDAO dao = new SettingsDAO();
        Settings s = new Settings();
        s.setMaintenanceMode(true);
        s.setRegistrationEnabled(false);
        s.setCurrentSemester("Spring");
        s.setCurrentYear(2026);
        s.setAddDropDeadline(LocalDateTime.of(2026, 2, 15, 17, 0));
        s.setWithdrawalDeadline(LocalDateTime.of(2026, 3, 15, 17, 0));
        s.setAnnouncement("Test Announcement");

        dao.updateSettings(s);

        Settings loaded = dao.getSettings();
        assertNotNull(loaded);
        assertTrue(loaded.isMaintenanceMode());
        assertFalse(loaded.isRegistrationEnabled());
        assertEquals("Spring", loaded.getCurrentSemester());
        assertEquals(2026, loaded.getCurrentYear());
        assertNotNull(loaded.getAddDropDeadline());
        assertNotNull(loaded.getWithdrawalDeadline());
        assertEquals("Test Announcement", loaded.getAnnouncement());

        // cleanup: reset to defaults to avoid impacting other tests/users
        Settings reset = new Settings();
        reset.setMaintenanceMode(false);
        reset.setRegistrationEnabled(true);
        reset.setCurrentSemester("Fall");
        reset.setCurrentYear(2025);
        reset.setAddDropDeadline(null);
        reset.setWithdrawalDeadline(null);
        reset.setAnnouncement("");
        dao.updateSettings(reset);
    }
}

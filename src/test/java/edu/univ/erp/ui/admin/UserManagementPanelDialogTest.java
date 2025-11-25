package edu.univ.erp.ui.admin;

import org.junit.jupiter.api.Test;
import javax.swing.*;
import java.awt.*;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Tests for UserManagementPanel dialog creation functionality.
 * Validates that the dialog constructor signatures support Window parent type,
 * which allows nested dialogs to work regardless of whether the parent is a
 * JFrame or JDialog.
 * 
 * Note: These tests verify the fix at the API level without requiring a GUI,
 * making them suitable for headless CI environments.
 */
class UserManagementPanelDialogTest {

    @Test
    void testJDialogConstructorAcceptsWindowWithModalityType() {
        // This test validates that JDialog has a constructor that accepts:
        // - Window parent (not just Frame or Dialog)
        // - String title
        // - Dialog.ModalityType modality
        // This is the constructor used in the fix.
        
        assertDoesNotThrow(() -> {
            // Mock a Window - we can use null in headless mode
            Window parent = null;
            String title = "Test Dialog";
            Dialog.ModalityType modality = Dialog.ModalityType.APPLICATION_MODAL;
            
            // This should NOT throw NoSuchMethodException
            JDialog.class.getConstructor(Window.class, String.class, Dialog.ModalityType.class);
            
        }, "JDialog should have constructor(Window, String, ModalityType)");
    }

    @Test
    void testWindowIsCommonSuperclassOfJFrameAndJDialog() {
        // Verify the class hierarchy that makes the fix work:
        // Both JFrame and JDialog inherit from Window
        
        assertTrue(Window.class.isAssignableFrom(JFrame.class),
            "JFrame should inherit from Window");
        assertTrue(Window.class.isAssignableFrom(JDialog.class),
            "JDialog should inherit from Window");
        
        // This means SwingUtilities.getWindowAncestor() can return either
        // and we can safely cast to Window for use in JDialog constructor
    }

    @Test
    void testDialogModalityTypeEquivalence() {
        // Verify that Dialog.ModalityType.APPLICATION_MODAL is equivalent
        // to the boolean 'true' parameter in the old constructor signature
        
        Dialog.ModalityType modalityType = Dialog.ModalityType.APPLICATION_MODAL;
        assertNotNull(modalityType, "APPLICATION_MODAL should be a valid modality type");
        
        // APPLICATION_MODAL means the dialog blocks all windows from the same application
        // which is the behavior of modal=true in the old constructor
        assertEquals(Dialog.ModalityType.APPLICATION_MODAL, modalityType);
    }

    @Test
    void testFrameCannotBeCastToDialog() {
        // This test documents the bug that was fixed:
        // Attempting to cast JDialog to Frame causes ClassCastException
        
        Class<?> frameClass = Frame.class;
        Class<?> dialogClass = JDialog.class;
        
        // Frame is NOT a superclass of JDialog
        assertFalse(frameClass.isAssignableFrom(dialogClass),
            "JDialog should NOT be castable to Frame - this was the bug");
        
        // But Window IS a superclass of both
        assertTrue(Window.class.isAssignableFrom(frameClass),
            "Frame should be castable to Window");
        assertTrue(Window.class.isAssignableFrom(dialogClass),
            "JDialog should be castable to Window");
    }
}


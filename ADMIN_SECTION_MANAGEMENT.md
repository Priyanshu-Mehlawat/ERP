# Admin Section Management Feature

## Overview
Complete implementation of Section Management Panel for administrators to perform CRUD operations on course sections.

## Implementation Date
October 13, 2025

## Components Created/Modified

### New Files
1. **SectionManagementPanel.java** (779 lines)
   - Full-featured admin interface for section management
   - Search and filter capabilities
   - CRUD operations with background processing
   - Instructor assignment functionality

### Modified Files
1. **SectionDAO.java**
   - Added `findAll()` - Get all sections with course/instructor details
   - Added `save(Section)` - Insert new section (returns generated ID)
   - Added `update(Section)` - Update existing section
   - Added `delete(Long)` - Delete section by ID
   - Added `assignInstructor(Long sectionId, Long instructorId)` - Assign/unassign instructor

2. **InstructorDAO.java**
   - Added `findAll()` - Get all instructors for dropdown population

3. **SectionService.java**
   - Added `listAllSections()` - Business logic wrapper for finding all sections

4. **AdminDashboard.java**
   - Updated `openSectionManagement()` to launch SectionManagementPanel dialog

## Features Implemented

### 1. Section Listing
- **Table View**: Displays all sections with columns:
  - Section ID
  - Course Code
  - Course Title
  - Section Number (e.g., "001", "A", "B")
  - Semester + Year (e.g., "Fall 2025")
  - Instructor Name (or "Not Assigned")
  - Schedule (formatted as "Monday,Wednesday 10:00-11:30" or "TBA")
  - Room
  - Capacity
  - Enrolled Count
- **Auto-refresh**: Loads on panel initialization
- **Background Loading**: Uses SwingWorker to prevent UI freezing

### 2. Search and Filter
- **Text Search**: Searches by course code, course title, or room number
- **Semester Filter**: Dropdown with "All", "Fall", "Spring", "Summer"
- **Real-time Filtering**: Applies filters in background thread
- **Refresh Button**: Reloads all sections from database

### 3. Add Section
- **Modal Dialog**: Opens form for new section creation
- **Form Fields**:
  - Course Selection (dropdown with code + title)
  - Section Number (text, e.g., "001", "A")
  - Semester (dropdown: Fall/Spring/Summer)
  - Year (spinner: 2024-2030)
  - Day of Week (text, e.g., "Monday,Wednesday")
  - Start Time (hour/minute spinners)
  - End Time (hour/minute spinners)
  - Room (text, e.g., "Building A - Room 101")
  - Capacity (spinner: 1-200)
- **Validation**: The following business rules are enforced **before background save**:
  - **Time Sequence**: End time must be strictly after start time (no zero-duration or negative-duration classes)
  - **Instructor Conflicts**: Selected instructor must not have another section at the same day/time (prevents double-booking)
  - **Room Conflicts**: Selected room must not be occupied by another section at the same day/time (prevents double-booking)
  - **Course Active Status**: Selected course must be in active status before creating a section (inactive courses cannot have sections)
  - **Capacity Constraint (Edit only)**: New capacity must be >= current enrolled student count (cannot reduce capacity below enrolled)
  - All validations must **prevent submission** with clear, user-facing error messages (JOptionPane dialogs)
  - Validation checks run **before** SwingWorker background save to provide immediate feedback
- **Background Save**: Uses SwingWorker to prevent UI blocking
- **Success Feedback**: Shows confirmation message and refreshes table

### 4. Edit Section
- **Selection Required**: Shows error if no section selected
- **Pre-populated Form**: Loads current section data into form fields
- **Same Fields as Add**: Section number, semester, year, schedule, room, capacity
- **Validation**: Applies all business rules from Add Section (time sequence, instructor conflicts, room conflicts, course active status, capacity >= enrolled count)
- **Background Update**: Uses SwingWorker
- **Auto-refresh**: Reloads table after successful update

### 5. Delete Section
- **Selection Required**: Shows error if no section selected
- **Enrollment Check**: Prevents deletion if students are enrolled
  - Shows warning: "Cannot delete section with enrolled students"
- **Confirmation Dialog**: Asks user to confirm with section details
- **Background Deletion**: Uses SwingWorker
- **Success Feedback**: Shows confirmation and refreshes table

### 6. Assign Instructor
- **Selection Required**: Shows error if no section selected
- **Instructor Dropdown**: Shows all instructors with full name + ID
- **Unassign Option**: "-- No Instructor --" option to remove assignment
- **Background Operation**: Uses SwingWorker
- **Flexible Messages**: Shows "assigned" or "unassigned" based on action

### 7. View Details
- **Selection Required**: Shows error if no section selected
- **Detailed View**: Modal dialog with formatted details:
  - Section ID
  - Course (code + title)
  - Section Number
  - Semester + Year
  - Schedule (formatted)
  - Room
  - Instructor Name
  - Capacity
  - Enrolled Count
  - Available Seats (calculated)
  - Fill Rate (percentage)
- **Read-only**: Text area with monospaced font for alignment

## Technical Implementation

### Schedule Handling
The Section domain model stores schedule as three fields:
- `dayOfWeek` (String): "Monday", "Tuesday,Thursday", etc.
- `startTime` (LocalTime): Start time of class
- `endTime` (LocalTime): End time of class

**Helper Method** `formatSchedule(Section)`:
- Combines dayOfWeek + startTime + endTime
- Returns formatted string like "Monday,Wednesday 10:00-11:30"
- Returns "TBA" if schedule not set

### Data Access
- **SectionDAO** uses JOIN queries to fetch course and instructor details
- **findAll()** returns sections with `courseCode`, `courseTitle`, `instructorName` populated
- **save()** and **update()** handle nullable instructor ID and time fields
- **delete()** is blocked by FK constraints (enrollments FK: ON DELETE RESTRICT) when enrollments exist

### UI Patterns
1. **Table-based Listing**: Main content area with JTable
2. **Toolbar**: Search field, filters, and refresh button at top
3. **Action Buttons**: Row of buttons below table (Add, Edit, Delete, Assign, View)
4. **Modal Dialogs**: For add/edit/assign operations
5. **SwingWorker**: All database operations run in background threads
6. **Error Handling**: Try-catch with user-friendly error messages

### Thread Safety
- All database calls use SwingWorker for background execution
- UI updates happen in Event Dispatch Thread (EDT) via `done()` method
- Prevents UI freezing during long operations

### Transaction Management

#### Operations Requiring Transactions
The following multi-step operations **MUST** be wrapped in database transactions:
- **Create Section**: Insert section + update related audit fields
- **Update Section**: Modify section + update lastModified timestamp + handle capacity constraints
- **Delete Section**: Remove section + update audit trail (Note: deletion is blocked by ON DELETE RESTRICT FK constraint if enrollments exist; student enrollments are never automatically deleted)
- **Assign Instructor**: Update section.instructorId + validate instructor availability + log assignment
- **Unassign Instructor**: Clear section.instructorId + validate no active enrollments + log unassignment
- Any operation that modifies multiple related entities or audit fields

#### Transaction Boundaries
All transactional operations must use explicit transaction control:

```java
Connection conn = null;
try {
    conn = DatabaseConnection.getErpConnection();
    conn.setAutoCommit(false);
    conn.setTransactionIsolation(Connection.TRANSACTION_READ_COMMITTED);
    
    // Perform multi-step operation
    // 1. Read current state (with version)
    // 2. Validate business rules
    // 3. Execute updates
    // 4. Update version/lastModified
    
    conn.commit();
} catch (SQLException e) {
    if (conn != null) {
        try {
            conn.rollback();
            logger.warn("Transaction rolled back due to: {}", e.getMessage());
        } catch (SQLException rollbackEx) {
            logger.error("Rollback failed", rollbackEx);
        }
    }
    throw e; // Re-throw for handling at higher layer
} finally {
    if (conn != null) {
        try {
            conn.setAutoCommit(true); // Restore default
        } catch (SQLException e) {
            logger.error("Failed to restore auto-commit", e);
        }
    }
}
```

**Isolation Level**: Use `TRANSACTION_READ_COMMITTED` for most operations to balance consistency and concurrency.

#### Optimistic Locking
To prevent lost updates in concurrent scenarios, implement optimistic locking:

**1. Database Schema**: Add version control fields to Section (and related entities):
```sql
ALTER TABLE sections ADD COLUMN version INT NOT NULL DEFAULT 1;
ALTER TABLE sections ADD COLUMN last_modified TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP;
```

**2. DAO Update Pattern**: Check version before update:
```java
public void update(Section section) throws SQLException, StaleObjectStateException {
    String sql = "UPDATE sections SET course_id=?, section_number=?, day_of_week=?, " +
                 "start_time=?, end_time=?, room=?, semester=?, capacity=?, instructor_id=?, " +
                 "version=version+1, last_modified=NOW() " +
                 "WHERE id=? AND version=?";
    
    try (Connection conn = DatabaseConnection.getErpConnection();
         PreparedStatement ps = conn.prepareStatement(sql)) {
        // Set parameters...
        ps.setLong(10, section.getId());
        ps.setInt(11, section.getVersion()); // Optimistic lock check
        
        int rowsAffected = ps.executeUpdate();
        if (rowsAffected == 0) {
            // Version mismatch - data was modified by another user/process
            throw new StaleObjectStateException(
                "Section was modified by another user. Please reload and try again.");
        }
        
        section.setVersion(section.getVersion() + 1); // Update in-memory version
    }
}
```

**3. Custom Exception**: Define `StaleObjectStateException`:
```java
public class StaleObjectStateException extends SQLException {
    public StaleObjectStateException(String message) {
        super(message);
    }
}
```

#### Retry Strategy for Transient Failures
Implement exponential backoff for transient database errors (deadlocks, timeouts, connection issues):

```java
private static final int MAX_RETRY_ATTEMPTS = 3;
private static final long INITIAL_BACKOFF_MS = 100;

public <T> T executeWithRetry(Callable<T> operation) throws Exception {
    int attempt = 0;
    long backoffMs = INITIAL_BACKOFF_MS;
    
    while (true) {
        try {
            return operation.call();
        } catch (SQLException e) {
            attempt++;
            
            // Check if error is transient (deadlock, timeout, connection)
            boolean isTransient = isTransientError(e);
            
            if (!isTransient || attempt >= MAX_RETRY_ATTEMPTS) {
                logger.error("Operation failed after {} attempts", attempt, e);
                throw e;
            }
            
            logger.warn("Transient error on attempt {}, retrying after {}ms: {}", 
                       attempt, backoffMs, e.getMessage());
            
            Thread.sleep(backoffMs);
            backoffMs *= 2; // Exponential backoff
        }
    }
}

private boolean isTransientError(SQLException e) {
    int errorCode = e.getErrorCode();
    String sqlState = e.getSQLState();
    
    // MySQL deadlock: 1213, lock timeout: 1205
    // Connection errors: 08xxx
    return errorCode == 1213 || errorCode == 1205 || 
           (sqlState != null && sqlState.startsWith("08"));
}
```

#### Conflict Handling
When optimistic lock failures occur:

**1. Service Layer**: Catch and handle gracefully:
```java
public void updateSection(Section section) throws SectionServiceException {
    try {
        executeWithRetry(() -> {
            sectionDAO.update(section);
            return null;
        });
    } catch (StaleObjectStateException e) {
        // Don't retry optimistic lock failures - user must resolve
        throw new SectionServiceException(
            "Section was modified by another user. Please reload the latest data and reapply your changes.", 
            e);
    } catch (Exception e) {
        throw new SectionServiceException("Failed to update section", e);
    }
}
```

**2. UI Layer (SwingWorker)**: Translate exceptions to user actions:
```java
SwingWorker<Void, Void> worker = new SwingWorker<>() {
    @Override
    protected Void doInBackground() throws Exception {
        sectionService.updateSection(section); // May throw exceptions
        return null;
    }
    
    @Override
    protected void done() {
        try {
            get(); // Retrieve result, throws exception if failed
            JOptionPane.showMessageDialog(panel, "Section updated successfully!");
            refreshTable(); // Reload data
        } catch (ExecutionException e) {
            Throwable cause = e.getCause();
            
            if (cause instanceof SectionServiceException) {
                String message = cause.getMessage();
                
                if (message.contains("modified by another user")) {
                    // Optimistic lock conflict
                    int response = JOptionPane.showConfirmDialog(panel,
                        message + "\n\nReload latest data now?",
                        "Conflict Detected",
                        JOptionPane.YES_NO_OPTION,
                        JOptionPane.WARNING_MESSAGE);
                    
                    if (response == JOptionPane.YES_OPTION) {
                        reloadAndShowEditDialog(section.getId()); // Refresh and retry
                    }
                } else {
                    // Other errors
                    JOptionPane.showMessageDialog(panel,
                        "Error updating section: " + message,
                        "Error",
                        JOptionPane.ERROR_MESSAGE);
                }
            } else {
                logger.error("Unexpected error in background task", cause);
                JOptionPane.showMessageDialog(panel,
                    "An unexpected error occurred. Please try again.",
                    "Error",
                    JOptionPane.ERROR_MESSAGE);
            }
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            logger.warn("Operation interrupted", e);
        }
    }
};
worker.execute(); // Runs in background, off EDT
```

#### Critical Requirements
1. **All DB operations MUST remain off EDT** - always use SwingWorker or background threads
2. **Version field MUST be included** in all UPDATE WHERE clauses for entities supporting optimistic locking
3. **Transient failures MUST be retried** with exponential backoff (max 2-3 attempts)
4. **Optimistic lock failures MUST NOT be retried** automatically - require user intervention
5. **All exceptions MUST be translated** to user-friendly messages in UI layer
6. **Transaction isolation MUST be set** explicitly for multi-step operations
7. **Rollback MUST be performed** in catch blocks for any transaction failure
8. **Auto-commit MUST be restored** in finally blocks after manual transaction control

## Integration
- **AdminDashboard**: "Manage Sections" button opens SectionManagementPanel in 1000x600 dialog
- **Service Layer**: Uses SectionService for business logic
- **DAO Layer**: Enhanced SectionDAO with full CRUD operations

## Testing Status
✅ Compilation successful  
✅ Application starts without errors  
✅ Panel integrated into AdminDashboard  

## Dependencies
- Section domain model (with dayOfWeek, startTime, endTime fields)
- Course domain model (with getCode(), getTitle())
- Instructor domain model (with getFullName())
- SectionDAO, CourseDAO, InstructorDAO
- SectionService
- MigLayout for flexible layouts
- SwingWorker for background operations

## Next Steps

### Manual Testing
- Manual testing: Create, edit, delete, assign instructor
- Verify search/filter functionality
- Test with enrolled students (delete prevention)
- UI/UX improvements based on testing

### Audit Logging Implementation
Implement comprehensive audit logging for all admin operations to ensure compliance and incident investigation:

#### Required Audit Information
Every section management operation **MUST** record:
- **Who**: User ID and username of the administrator performing the action
- **When**: Precise timestamp (yyyy-MM-dd HH:mm:ss.SSS with timezone)
- **What**: Operation type (CREATE_SECTION, UPDATE_SECTION, DELETE_SECTION, ASSIGN_INSTRUCTOR, UNASSIGN_INSTRUCTOR)
- **Which**: Affected entity IDs (section ID, course ID, instructor ID)
- **Before/After State**: For updates, capture:
  - Previous values (capacity, room, schedule, instructor)
  - New values (capacity, room, schedule, instructor)
  - Changed fields only (delta tracking)
- **Reason**: Optional free-text field for admin to explain the action (especially for deletions and capacity reductions)
- **Context**: Request source (IP address, session ID), client info

#### Operations Requiring Audit Logs
1. **Create Section**:
   - Record: Admin ID, timestamp, section ID, course ID, initial capacity, room, schedule
   - Before state: N/A (new record)
   - After state: Complete section details
   
2. **Update Section**:
   - Record: Admin ID, timestamp, section ID, changed fields
   - Before state: Previous values of changed fields
   - After state: New values of changed fields
   - Example: `capacity: 30 → 40`, `room: "A101" → "B202"`
   
3. **Delete Section**:
   - Record: Admin ID, timestamp, section ID, reason (mandatory)
   - Before state: Complete section details (snapshot before deletion)
   - After state: DELETED flag
   - Verify: No enrolled students (log enrollment count = 0)
   
4. **Assign Instructor**:
   - Record: Admin ID, timestamp, section ID, instructor ID
   - Before state: Previous instructor ID (or NULL if unassigned)
   - After state: New instructor ID
   
5. **Unassign Instructor**:
   - Record: Admin ID, timestamp, section ID, instructor ID, reason (recommended)
   - Before state: Previous instructor ID
   - After state: NULL

#### Audit Log Storage
- **Table**: `audit_log` or `section_audit_trail`
- **Schema**:
  ```sql
  CREATE TABLE section_audit_log (
      id BIGINT PRIMARY KEY AUTO_INCREMENT,
      user_id BIGINT NOT NULL,
      username VARCHAR(100) NOT NULL,
      action VARCHAR(50) NOT NULL,
      entity_type VARCHAR(50) NOT NULL DEFAULT 'SECTION',
      entity_id BIGINT NOT NULL,
      timestamp TIMESTAMP(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3),
      before_state TEXT,  -- JSON snapshot
      after_state TEXT,   -- JSON snapshot
      changed_fields TEXT, -- CSV or JSON of changed field names
      reason TEXT,
      ip_address VARCHAR(45),
      session_id VARCHAR(100),
      INDEX idx_entity (entity_type, entity_id),
      INDEX idx_user (user_id),
      INDEX idx_timestamp (timestamp),
      INDEX idx_action (action)
  );
  ```
- **Immutability**: No UPDATE or DELETE operations allowed (insert-only table)
- **Retention**: Define retention policy (e.g., keep indefinitely or 7 years for compliance)

#### Implementation Pattern
```java
public void auditLog(String action, Long entityId, String beforeState, String afterState, String reason) {
    String sql = "INSERT INTO section_audit_log " +
                 "(user_id, username, action, entity_type, entity_id, before_state, after_state, reason, ip_address) " +
                 "VALUES (?, ?, ?, 'SECTION', ?, ?, ?, ?, ?)";
    
    try (Connection conn = DatabaseConnection.getErpConnection();
         PreparedStatement ps = conn.prepareStatement(sql)) {
        
        ps.setLong(1, currentUser.getId());
        ps.setString(2, currentUser.getUsername());
        ps.setString(3, action);
        ps.setLong(4, entityId);
        ps.setString(5, beforeState); // JSON or null
        ps.setString(6, afterState);  // JSON or null
        ps.setString(7, reason);      // null if not provided
        ps.setString(8, getCurrentIPAddress());
        
        ps.executeUpdate();
        logger.info("Audit log recorded: {} by {} on section {}", action, currentUser.getUsername(), entityId);
        
    } catch (SQLException e) {
        // Audit logging failure should not block operations, but must be logged
        logger.error("CRITICAL: Failed to record audit log for action={}, user={}, entity={}", 
                    action, currentUser.getUsername(), entityId, e);
        // Consider alerting admins about audit log failures
    }
}
```

#### Audit Verification Steps
Test that audit logs meet compliance requirements:

1. **Record Completeness**:
   - ✅ Create a section → Verify audit log contains admin ID, timestamp, section ID, full initial state
   - ✅ Update section capacity → Verify audit log shows before (30) and after (40) values
   - ✅ Delete section → Verify audit log captures full snapshot before deletion + reason
   - ✅ Assign instructor → Verify audit log records previous instructor (or NULL) and new instructor

2. **Immutability**:
   - ✅ Attempt to UPDATE audit_log table → Should fail (permissions should prevent)
   - ✅ Attempt to DELETE from audit_log → Should fail (permissions should prevent)
   - ✅ Verify table has no UPDATE or DELETE triggers that could modify/remove records

3. **Access Control**:
   - ✅ Admin role: Can insert audit logs, can query own audit logs
   - ✅ Auditor role: Can query all audit logs (read-only), cannot modify
   - ✅ Regular users: Cannot access audit_log table
   - ✅ System auditor: Can export audit logs for external compliance systems

4. **Query Performance**:
   - ✅ Query audit logs by entity_id (section) → Should use idx_entity index, < 50ms
   - ✅ Query audit logs by user_id → Should use idx_user index, < 50ms
   - ✅ Query audit logs by date range → Should use idx_timestamp index, < 100ms
   - ✅ Full-text search on reason field → Should be performant for incident investigation

5. **Audit Trail Reconstruction**:
   - ✅ Given a section ID, reconstruct complete history of changes
   - ✅ Verify timeline: Create → Update (capacity) → Assign instructor → Update (room) → Delete
   - ✅ Confirm before/after states allow rollback simulation if needed

6. **Failure Handling**:
   - ✅ Simulate database connection failure during audit logging → Operation completes, error logged
   - ✅ Verify audit logging failures trigger alerts to system administrators
   - ✅ Confirm operations are not blocked by audit log failures (availability > strict consistency)

7. **Compliance Reporting**:
   - ✅ Generate report: All section deletions in last 30 days with reasons
   - ✅ Generate report: All sections modified by specific admin in date range
   - ✅ Generate report: All instructor assignments/unassignments for compliance review
   - ✅ Export audit logs in standard format (CSV, JSON) for external audit tools

#### Critical Requirements
- **Mandatory**: Audit logging MUST be implemented before production deployment
- **Performance**: Audit inserts must not add > 50ms latency to operations
- **Reliability**: Audit log failures must not block operations but must trigger alerts
- **Security**: Audit logs must be immutable and access-restricted to authorized personnel
- **Compliance**: Retention policy must align with institutional/regulatory requirements (FERPA, GDPR, etc.)

## Notes
- Sections cannot be deleted if students are enrolled (enforced at UI level)
- Instructor assignment is optional (can be null)
- Schedule fields (dayOfWeek, times) are optional (can show "TBA")
- Year range: 2024-2030 (easily adjustable)
- Capacity range: 1-200 students
- Time spinners use 15-minute increments for convenience

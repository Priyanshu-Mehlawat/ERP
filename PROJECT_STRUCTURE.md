# Project Structure

This document explains the organization of the University ERP System codebase.

## Package Overview

```
edu.univ.erp/
├── Main.java                 # Application entry point
├── domain/                   # Entity/Model classes (POJOs)
├── auth/                     # Authentication & authorization
├── data/                     # Database access layer
├── service/                  # Business logic layer
├── api/                      # API/Controller layer
├── ui/                       # User interface (Swing)
├── access/                   # Permission/access control
└── util/                     # Utility classes
```

## Layer Responsibilities

### 1. Domain Layer (`edu.univ.erp.domain`)
**Purpose**: Data models / entities

**Classes**:
- `User` - Authentication user
- `Student` - Student profile
- `Instructor` - Instructor profile
- `Course` - Course information
- `Section` - Class section
- `Enrollment` - Student enrollment
- `Grade` - Assessment scores
- `Settings` - System settings

**Rules**:
- No database code
- No UI code
- Only getters/setters and simple logic
- Can have helper methods (e.g., `getFullName()`)

### 2. Auth Layer (`edu.univ.erp.auth`)
**Purpose**: User authentication and session management

**Classes**:
- `AuthService` - Login/logout logic
- `AuthDAO` - Auth DB access
- `PasswordUtil` - Password hashing (BCrypt)
- `SessionManager` - Current user session

**Rules**:
- Only talks to Auth DB
- Never stores plain passwords
- Manages current logged-in user

### 3. Data Layer (`edu.univ.erp.data`)
**Purpose**: Database connections and DAOs

**Classes**:
- `DatabaseConnection` - HikariCP connection pooling
- `StudentDAO` - Student CRUD
- `CourseDAO` - Course CRUD
- `SectionDAO` - Section CRUD
- `EnrollmentDAO` - Enrollment CRUD
- `GradeDAO` - Grade CRUD
- `SettingsDAO` - Settings CRUD

**Rules**:
- Only talks to ERP DB (not Auth DB)
- Returns domain objects
- Handles SQL queries
- Proper resource cleanup (try-with-resources)

### 4. Service Layer (`edu.univ.erp.service`)
**Purpose**: Business logic / "brain" of the application

**Classes**:
- `StudentService` - Student operations
- `InstructorService` - Instructor operations
- `CourseService` - Course operations
- `EnrollmentService` - Enrollment logic
- `GradeService` - Grade calculations
- `MaintenanceService` - Maintenance mode

**Rules**:
- Calls DAOs for data
- Enforces business rules
- Checks permissions
- Validates inputs
- Returns success/error results

**Example**:
```java
public Result registerForSection(Long studentId, Long sectionId) {
    // 1. Check maintenance mode
    // 2. Check if section has seats
    // 3. Check for duplicate enrollment
    // 4. Insert enrollment
    // 5. Update seat count
    // 6. Return success/error
}
```

### 5. API Layer (`edu.univ.erp.api`)
**Purpose**: Coordinate between UI and services

**Packages**:
- `api.auth` - Login/logout/session
- `api.catalog` - Course browsing
- `api.student` - Student operations
- `api.instructor` - Instructor operations
- `api.admin` - Admin operations

**Rules**:
- UI calls API, never calls service/DAO directly
- API validates requests
- API calls services
- API returns clear responses

### 6. Access Layer (`edu.univ.erp.access`)
**Purpose**: Permission checking

**Classes**:
- `AccessControl` - Check if action is allowed
- `MaintenanceChecker` - Check maintenance mode

**Rules**:
- Called before any write operation
- Returns true/false
- No database access (uses session)

### 7. UI Layer (`edu.univ.erp.ui`)
**Purpose**: Swing user interface

**Packages**:
- `ui.auth` - Login screen
- `ui.student` - Student screens
- `ui.instructor` - Instructor screens
- `ui.admin` - Admin screens
- `ui.common` - Shared components

**Rules**:
- Only calls API layer
- Never accesses database directly
- Shows messages to user
- Handles user input

### 8. Util Layer (`edu.univ.erp.util`)
**Purpose**: Helper utilities

**Classes**:
- `ConfigUtil` - Load properties
- `CSVExporter` - Export to CSV
- `PDFExporter` - Export to PDF
- `MessageHelper` - Standard messages
- `ValidationUtil` - Input validation

## Data Flow

### Example: Student Registration

```
1. StudentDashboard (UI)
   ↓ user clicks "Register"
   
2. RegistrationDialog (UI)
   ↓ user selects section
   
3. StudentAPI.registerForSection()
   ↓ validate request
   
4. EnrollmentService.register()
   ↓ check rules
   
5. EnrollmentDAO.create()
   ↓ insert to DB
   
6. Return success ← ← ← ←
   ↓
   
7. Show success message (UI)
```

## Database Separation

### Auth DB (`erp_auth`)
- `users_auth` - Credentials only
- `password_history` - Password changes

### ERP DB (`erp_main`)
- All application data
- Links to Auth DB via `user_id`

**Why separate?**
- Security: Passwords isolated
- Backup: Can backup separately
- Access: Different permissions

## Key Design Patterns

### 1. DAO Pattern
- Data Access Objects handle all SQL
- Services don't write SQL

### 2. Service Layer Pattern
- Business logic in services
- UI doesn't know about database

### 3. Singleton Pattern
- `SessionManager` - one instance
- `DatabaseConnection` - connection pools

### 4. MVC-like Pattern
- Model = Domain classes
- View = UI classes
- Controller = API + Service classes

## File Naming Conventions

- **Entities**: Noun (e.g., `Student`, `Course`)
- **DAOs**: `EntityDAO` (e.g., `StudentDAO`)
- **Services**: `EntityService` (e.g., `StudentService`)
- **UI**: Descriptive (e.g., `StudentDashboard`, `CourseListPanel`)
- **Util**: `PurposeUtil` (e.g., `ValidationUtil`)

## Adding New Features

### Steps:
1. **Domain**: Add/update entity class
2. **Data**: Add DAO methods
3. **Service**: Add business logic
4. **API**: Add API methods
5. **UI**: Add screens/dialogs
6. **Test**: Write tests

### Example: Add "Course Prerequisites"

1. **Domain**: Add `prerequisiteCourseId` to `Course`
2. **Data**: Update `CourseDAO.create()`
3. **Service**: Add `CourseService.checkPrerequisites()`
4. **API**: Update `CatalogAPI.registerForSection()`
5. **UI**: Show prerequisites in catalog
6. **Test**: Test prerequisite validation

## Best Practices

1. **Separation of Concerns**: Each layer has one job
2. **No Skip Layers**: UI → API → Service → DAO → DB
3. **Resource Cleanup**: Always close connections
4. **Logging**: Log important actions
5. **Error Handling**: Catch exceptions, show friendly messages
6. **Validation**: Validate at every layer
7. **Documentation**: Comment complex logic

## Common Mistakes to Avoid

❌ UI accessing database directly
❌ Putting SQL in service layer
❌ Storing passwords as plain text
❌ Skipping permission checks
❌ Not closing database connections
❌ Mixing Auth DB and ERP DB access
❌ Hardcoding values

✅ Follow the layer structure
✅ Use DAOs for all database access
✅ Hash passwords with BCrypt
✅ Check permissions before writes
✅ Use try-with-resources
✅ Keep databases separate
✅ Use configuration files

# University ERP System

A comprehensive desktop application built with Java and Swing for managing university operations including courses, enrollments, grades, and user management.

## Features

### For Students
- Browse course catalog
- Register for sections with seat availability checks
- Drop courses before deadline
- View personal timetable
- Check grades and download transcripts

### For Instructors
- View assigned sections
- Enter and manage assessment scores
- Compute final grades with configurable weighting
- View class statistics
- Export grades to CSV

### For Administrators
- User management (students, instructors, admins)
- Course and section management
- Instructor assignment
- Maintenance mode control
- Database backup/restore (optional)

## Technology Stack

- **Java 17+** - Core programming language
- **Swing + FlatLaf** - Modern UI framework
- **MySQL** - Database (Auth DB + ERP DB)
- **HikariCP** - Connection pooling
- **BCrypt** - Password hashing
- **OpenCSV** - CSV export
- **OpenPDF** - PDF generation
- **SLF4J + Logback** - Logging
- **Maven** - Build tool

## Project Structure

```
src/main/java/edu/univ/erp/
├── Main.java                    # Application entry point
├── domain/                      # Entity classes
│   ├── User.java
│   ├── Student.java
│   ├── Instructor.java
│   ├── Course.java
│   ├── Section.java
│   ├── Enrollment.java
│   ├── Grade.java
│   └── Settings.java
├── auth/                        # Authentication layer
│   ├── AuthService.java
│   ├── AuthDAO.java
│   ├── PasswordUtil.java
│   └── SessionManager.java
├── data/                        # Database access
│   └── DatabaseConnection.java
├── ui/                          # User interface
│   ├── auth/
│   │   └── LoginFrame.java
│   ├── student/
│   │   └── StudentDashboard.java
│   ├── instructor/
│   │   └── InstructorDashboard.java
│   └── admin/
│       └── AdminDashboard.java
└── util/                        # Utilities
    └── ConfigUtil.java
```

## Database Schema

### Auth DB (erp_auth)
- `users_auth` - User credentials and authentication
- `password_history` - Password change history (optional)

### ERP DB (erp_main)
- `students` - Student profiles
- `instructors` - Instructor profiles
- `courses` - Course catalog
- `sections` - Class sections
- `enrollments` - Student enrollments
- `grades` - Assessment scores
- `settings` - System configuration

## Setup Instructions

### Prerequisites
1. Java 17 or higher
2. MySQL 8.0 or higher
3. Maven 3.6+

### Database Setup

1. **Start MySQL server**

2. **Create databases and tables:**
   ```bash
   mysql -u root -p < database/01_auth_schema.sql
   mysql -u root -p < database/02_erp_schema.sql
   ```

3. **Load sample data:**
   ```bash
   mysql -u root -p < database/03_auth_seed.sql
   mysql -u root -p < database/04_erp_seed.sql
   ```

### Configuration

Edit `src/main/resources/application.properties`:

```properties
# Auth Database
auth.db.url=jdbc:mysql://localhost:3306/erp_auth
auth.db.username=root
auth.db.password=your_password

# ERP Database
erp.db.url=jdbc:mysql://localhost:3306/erp_main
erp.db.username=root
erp.db.password=your_password
```

### Build and Run

1. **Build the project:**
   ```bash
   mvn clean package
   ```

2. **Run the application:**
   ```bash
   java -jar target/university-erp-1.0.0-jar-with-dependencies.jar
   ```

   Or using Maven:
   ```bash
   mvn exec:java -Dexec.mainClass="edu.univ.erp.Main"
   ```

## Default Credentials

| Username | Password     | Role       |
|----------|--------------|------------|
| admin1   | password123  | Admin      |
| inst1    | password123  | Instructor |
| stu1     | password123  | Student    |
| stu2     | password123  | Student    |

## Security Features

- **Password Hashing**: BCrypt with salt (never store plain passwords)
- **Separate Auth DB**: Authentication data isolated from application data
- **Session Management**: Track logged-in users
- **Failed Login Protection**: Account lockout after 5 failed attempts
- **Role-Based Access Control**: Enforce permissions by user role
- **Maintenance Mode**: Read-only mode for system updates

## Development Roadmap

### Week 1-2: Foundation
- ✅ Project setup and dependencies
- ✅ Database schemas
- ✅ Domain models
- ✅ Authentication system
- ✅ Basic UI framework

### Week 3-4: Student Features
- ⏳ Course catalog browsing
- ⏳ Section registration/drop
- ⏳ Timetable view
- ⏳ Grade viewing
- ⏳ Transcript export

### Week 5-6: Instructor Features
- ⏳ Section management
- ⏳ Grade entry system
- ⏳ Final grade computation
- ⏳ Class statistics
- ⏳ CSV export

### Week 7: Admin Features
- ⏳ User management
- ⏳ Course/section CRUD
- ⏳ Instructor assignment
- ⏳ Maintenance mode toggle
- ⏳ Backup/restore (bonus)

### Week 8: Testing & Polish
- ⏳ Comprehensive testing
- ⏳ UI refinements
- ⏳ Documentation
- ⏳ Demo video

## Testing

Run unit tests:
```bash
mvn test
```

## Contributing

This is an academic project. Follow the university's academic integrity policy.

## License

Educational use only - IIITD University ERP Project

## Support

For issues or questions, contact your project supervisor or TA.

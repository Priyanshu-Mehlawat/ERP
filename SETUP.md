# University ERP System - Setup Guide

## Quick Start

### 1. Install Prerequisites

#### Java
- Download and install Java 17 or higher
- Verify: `java -version`

#### MySQL
- Download and install MySQL 8.0 or higher
- Start MySQL server
- Verify: `mysql --version`

#### Maven
- Maven should be installed (usually comes with most IDEs)
- Verify: `mvn -version`

### 2. Database Setup

```bash
# Login to MySQL
mysql -u root -p

# Create databases
CREATE DATABASE erp_auth;
CREATE DATABASE erp_main;

# Exit MySQL
exit

# Run schema scripts
mysql -u root -p erp_auth < database/01_auth_schema.sql
mysql -u root -p erp_main < database/02_erp_schema.sql

# Load sample data
mysql -u root -p erp_auth < database/03_auth_seed.sql
mysql -u root -p erp_main < database/04_erp_seed.sql
```

### 3. Configure Database Connection

Edit `src/main/resources/application.properties`:

```properties
auth.db.url=jdbc:mysql://localhost:3306/erp_auth
auth.db.username=root
auth.db.password=YOUR_MYSQL_PASSWORD

erp.db.url=jdbc:mysql://localhost:3306/erp_main
erp.db.username=root
erp.db.password=YOUR_MYSQL_PASSWORD
```

### 4. Build the Project

```bash
# Clean and build
mvn clean package

# This creates:
# - target/university-erp-1.0.0.jar
# - target/university-erp-1.0.0-jar-with-dependencies.jar
```

### 5. Run the Application

```bash
# Option 1: Run with Maven
mvn exec:java -Dexec.mainClass="edu.univ.erp.Main"

# Option 2: Run JAR file
java -jar target/university-erp-1.0.0-jar-with-dependencies.jar
```

### 6. Login

Use the default credentials:

| Username | Password     | Role       |
|----------|--------------|------------|
| admin1   | password123  | Admin      |
| inst1    | password123  | Instructor |
| stu1     | password123  | Student    |
| stu2     | password123  | Student    |

## Troubleshooting

### Database Connection Failed

**Problem**: Cannot connect to database

**Solutions**:
1. Verify MySQL is running: `sudo systemctl status mysql`
2. Check credentials in `application.properties`
3. Ensure databases exist: `SHOW DATABASES;`
4. Check MySQL port (default: 3306)

### Build Errors

**Problem**: Maven build fails

**Solutions**:
1. Clean Maven cache: `mvn clean`
2. Update dependencies: `mvn dependency:resolve`
3. Check Java version: `java -version` (must be 17+)

### Login Errors

**Problem**: "Incorrect username or password"

**Solutions**:
1. Verify seed data loaded: `SELECT * FROM erp_auth.users_auth;`
2. Check password hash format (should start with `$2a$`)
3. Try resetting password in database

### UI Not Displaying Correctly

**Problem**: UI looks wrong or buttons missing

**Solutions**:
1. Ensure FlatLaf is in classpath
2. Try different Look and Feel
3. Check Java Swing compatibility

## Development Setup

### Using IntelliJ IDEA

1. Open project folder
2. Wait for Maven import
3. Set JDK to 17+
4. Run `Main.java`

### Using Eclipse

1. Import as Maven project
2. Update project configuration
3. Set Java compliance to 17
4. Run `Main.java`

### Using VS Code

1. Install Java Extension Pack
2. Open project folder
3. Maven will auto-configure
4. Run `Main.java` or use Maven tasks

## Database Management

### View Data

```sql
-- View users
SELECT * FROM erp_auth.users_auth;

-- View students
SELECT * FROM erp_main.students;

-- View courses
SELECT * FROM erp_main.courses;

-- View enrollments
SELECT * FROM erp_main.enrollments;
```

### Add More Sample Data

```sql
-- Add more courses
INSERT INTO erp_main.courses (code, title, credits, department) 
VALUES ('CSE401', 'Advanced Algorithms', 4, 'Computer Science');

-- Add more sections
INSERT INTO erp_main.sections 
(course_id, instructor_id, section_number, capacity, semester, year) 
VALUES (1, 1, 'C', 40, 'Fall', 2025);
```

### Reset Database

```bash
# Drop and recreate
mysql -u root -p -e "DROP DATABASE erp_auth; DROP DATABASE erp_main;"
mysql -u root -p < database/01_auth_schema.sql
mysql -u root -p < database/02_erp_schema.sql
mysql -u root -p < database/03_auth_seed.sql
mysql -u root -p < database/04_erp_seed.sql
```

## Next Steps

1. ✅ Verify login works for all roles
2. ⏳ Implement course catalog browsing
3. ⏳ Add student registration functionality
4. ⏳ Build grade management system
5. ⏳ Create admin tools

## Getting Help

- Check logs in `logs/erp.log`
- Review project requirements document
- Contact your team members
- Refer to Java/Swing documentation

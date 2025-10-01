# Quick Command Reference

## Database Commands

### Setup Database (One Command)
```bash
./setup-database.sh
```

### Manual Database Setup
```bash
# Create databases and load schemas
mysql -u root -p < database/01_auth_schema.sql
mysql -u root -p < database/02_erp_schema.sql
mysql -u root -p < database/03_auth_seed.sql
mysql -u root -p < database/04_erp_seed.sql
```

### View Database Contents
```bash
# Connect to Auth DB
mysql -u root -p erp_auth

# Connect to ERP DB
mysql -u root -p erp_main
```

### Useful SQL Queries
```sql
-- View all users
SELECT user_id, username, role, status FROM erp_auth.users_auth;

-- View all students
SELECT * FROM erp_main.students;

-- View all courses
SELECT * FROM erp_main.courses;

-- View all sections with course info
SELECT s.*, c.code, c.title 
FROM erp_main.sections s 
JOIN erp_main.courses c ON s.course_id = c.course_id;

-- View enrollments with details
SELECT e.*, s.first_name, s.last_name, c.code, c.title
FROM erp_main.enrollments e
JOIN erp_main.students s ON e.student_id = s.student_id
JOIN erp_main.sections sec ON e.section_id = sec.section_id
JOIN erp_main.courses c ON sec.course_id = c.course_id;

-- Check maintenance mode
SELECT * FROM erp_main.settings WHERE setting_key = 'maintenance_mode';
```

### Reset Database
```bash
# Drop and recreate everything
mysql -u root -p -e "DROP DATABASE IF EXISTS erp_auth; DROP DATABASE IF EXISTS erp_main;"
./setup-database.sh
```

## Maven Commands

### Build
```bash
# Clean and build
mvn clean package

# Build without tests
mvn clean package -DskipTests

# Build with dependencies (fat JAR)
mvn clean package assembly:single
```

### Run
```bash
# Run using Maven
mvn exec:java -Dexec.mainClass="edu.univ.erp.Main"

# Run JAR directly
java -jar target/university-erp-1.0.0-jar-with-dependencies.jar
```

### Testing
```bash
# Run all tests
mvn test

# Run specific test class
mvn test -Dtest=ClassNameTest

# Run with coverage
mvn clean test jacoco:report
```

### Dependencies
```bash
# Update dependencies
mvn dependency:resolve -U

# List dependencies
mvn dependency:tree

# Download sources
mvn dependency:sources
```

### Clean
```bash
# Clean build directory
mvn clean

# Clean and remove IDE files
mvn clean
rm -rf .idea *.iml
```

## Application Commands

### Generate Password Hashes
```bash
mvn exec:java -Dexec.mainClass="edu.univ.erp.util.PasswordHashGenerator"
```

### View Logs
```bash
# Tail logs in real-time
tail -f logs/erp.log

# View last 100 lines
tail -n 100 logs/erp.log

# Search logs
grep "ERROR" logs/erp.log
grep "Login" logs/erp.log
```

## Development Commands

### Check Java Version
```bash
java -version
javac -version
echo $JAVA_HOME
```

### Check Maven Version
```bash
mvn -version
```

### Check MySQL Version
```bash
mysql --version
sudo systemctl status mysql
```

### Start/Stop MySQL
```bash
# Start MySQL
sudo systemctl start mysql

# Stop MySQL
sudo systemctl stop mysql

# Restart MySQL
sudo systemctl restart mysql

# Check status
sudo systemctl status mysql
```

## Git Commands (if using version control)

### Initial Setup
```bash
git init
git add .
git commit -m "Initial commit: University ERP project setup"
```

### Regular Workflow
```bash
# Check status
git status

# Stage changes
git add .

# Commit
git commit -m "Week 3: Implemented student registration"

# View history
git log --oneline
```

### Branching
```bash
# Create feature branch
git checkout -b feature/student-registration

# Switch back to main
git checkout main

# Merge feature
git merge feature/student-registration
```

## IDE Commands

### IntelliJ IDEA
```bash
# Open project
idea .

# From IntelliJ terminal
mvn clean package
mvn exec:java -Dexec.mainClass="edu.univ.erp.Main"
```

### VS Code
```bash
# Open project
code .

# Run Maven task (Ctrl+Shift+P)
# Maven: Execute commands... → clean package
```

### Eclipse
```bash
# Import as Maven project
File → Import → Maven → Existing Maven Projects
```

## Troubleshooting Commands

### Check Dependencies
```bash
# Verify all dependencies downloaded
mvn dependency:tree

# Force update
mvn clean install -U
```

### Check Database Connection
```bash
# Test MySQL connection
mysql -u root -p -e "SELECT 1"

# Check databases exist
mysql -u root -p -e "SHOW DATABASES"

# Check Auth DB tables
mysql -u root -p erp_auth -e "SHOW TABLES"

# Check ERP DB tables
mysql -u root -p erp_main -e "SHOW TABLES"
```

### Check Port Availability
```bash
# Check if MySQL port is open
netstat -an | grep 3306
lsof -i :3306
```

### Clear Maven Cache
```bash
rm -rf ~/.m2/repository
mvn clean install
```

## Quick Testing Scenarios

### Test Login (from MySQL)
```sql
-- Verify user exists
SELECT * FROM erp_auth.users_auth WHERE username = 'admin1';

-- Reset failed attempts
UPDATE erp_auth.users_auth SET failed_login_attempts = 0 WHERE username = 'admin1';

-- Unlock account
UPDATE erp_auth.users_auth SET status = 'ACTIVE' WHERE username = 'admin1';
```

### Add Test Data
```sql
-- Add a new course
INSERT INTO erp_main.courses (code, title, credits, department)
VALUES ('TEST101', 'Test Course', 3, 'Testing');

-- Add a section
INSERT INTO erp_main.sections 
(course_id, instructor_id, section_number, capacity, semester, year)
VALUES (LAST_INSERT_ID(), 1, 'A', 30, 'Fall', 2025);
```

## File Locations

### Configuration
- Main config: `src/main/resources/application.properties`
- Logging config: `src/main/resources/logback.xml`
- Maven config: `pom.xml`

### Logs
- Application logs: `logs/erp.log`
- Maven logs: `target/`

### Database Scripts
- Auth schema: `database/01_auth_schema.sql`
- ERP schema: `database/02_erp_schema.sql`
- Auth seed: `database/03_auth_seed.sql`
- ERP seed: `database/04_erp_seed.sql`

### Documentation
- Main README: `README.md`
- Setup guide: `SETUP.md`
- Getting started: `GETTING_STARTED.md`
- Structure: `PROJECT_STRUCTURE.md`
- Summary: `PROJECT_SUMMARY.md`

## Default Credentials

```
Username: admin1    | Password: password123 | Role: Admin
Username: inst1     | Password: password123 | Role: Instructor
Username: stu1      | Password: password123 | Role: Student
Username: stu2      | Password: password123 | Role: Student
```

## Common One-Liners

```bash
# Full reset and run
mvn clean package && mvn exec:java -Dexec.mainClass="edu.univ.erp.Main"

# Build and show logs
mvn clean package && tail -f logs/erp.log

# Database reset
mysql -u root -p -e "DROP DATABASE IF EXISTS erp_auth; DROP DATABASE IF EXISTS erp_main;" && ./setup-database.sh

# Clean everything
mvn clean && rm -rf logs/ && rm -rf .idea/ && rm -rf *.iml
```

## Performance Testing

```bash
# Time the build
time mvn clean package

# Monitor MySQL connections
watch -n 1 'mysql -u root -p -e "SHOW PROCESSLIST"'

# Check connection pool
grep "HikariPool" logs/erp.log
```

## Backup Commands

```bash
# Backup databases
mysqldump -u root -p erp_auth > backup_auth_$(date +%Y%m%d).sql
mysqldump -u root -p erp_main > backup_erp_$(date +%Y%m%d).sql

# Restore from backup
mysql -u root -p erp_auth < backup_auth_20251001.sql
mysql -u root -p erp_main < backup_erp_20251001.sql
```

---

**Pro Tip**: Create aliases in your `~/.bashrc` for commonly used commands:

```bash
alias erp-run='mvn exec:java -Dexec.mainClass="edu.univ.erp.Main"'
alias erp-build='mvn clean package'
alias erp-test='mvn test'
alias erp-logs='tail -f logs/erp.log'
alias erp-db='./setup-database.sh'
```

Then simply run: `erp-run`, `erp-build`, etc.

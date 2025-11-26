# 🎓 University ERP Management System

[![Java](https://img.shields.io/badge/Java-17+-orange.svg)](https://www.oracle.com/java/)
[![MySQL](https://img.shields.io/badge/MySQL-8.0+-blue.svg)](https://www.mysql.com/)
[![Maven](https://img.shields.io/badge/Maven-3.6+-red.svg)](https://maven.apache.org/)
[![License](https://img.shields.io/badge/License-Educational-green.svg)](LICENSE)

A desktop app built with Java Swing to help manage university tasks like enrolling students, handling courses, tracking grades, and basic admin work.

---

# Table of Contents
- Overview
- Features
- Tech Stack
- System Architecture
- Quick Start
- Setup Instructions
- How to Use
- Security
- Testing
- Docs
- Stats
- Troubleshooting
- License


---

## 🎯 Overview

This app helps manage university tasks for students, teachers, and admins.  
It has separate dashboards for each role and basic security features.

Some things we added:
- Password hashing & account lockouts
- FlatLaf UI for a nicer look
- Two databases with connection pooling

---

## 🚀 Features

### Students
- Browse courses and see how many seats are left
- Register for sections (won’t allow time conflicts)
- View weekly timetable
- Check grades for assignments, midterms, and finals
- Download official PDF transcripts

### Instructors
- See the sections you are teaching
- Enter grades for assignments, quizzes, midterms, finals
- Set assessment weights (must add up to 100%)
- Auto-calc final grades
- Export student performance reports in CSV
- Check student lists with contact info

### Administrators
- Add/edit students, instructors, and other admins
- Add/edit courses and set prerequisites
- Create sections, assign teachers, set schedules
- Configure semester dates and system deadlines
- Enable maintenance mode (read-only)
- Lock/unlock accounts and reset passwords
- View enrollment stats and system health

---

## 🛠️ Technology Stack

| Category        | Technology           | Purpose                         |
|-----------------|--------------------|---------------------------------|
| **Language**    | Java SE             | Core application logic          |
| **UI Framework**| Swing + FlatLaf     | Modern desktop interface        |
| **Database**    | MySQL               | Persistent data storage         |
| **Connection Pool** | HikariCP         | High-performance DB connections |
| **Security**    | BCrypt              | Password hashing (cost 12)     |
| **Build Tool**  | Maven               | Dependency management & build  |
| **Testing**     | JUnit 5             | Unit & integration testing      |
| **Logging**     | SLF4J + Logback     | Application logging             |
| **CSV Export**  | OpenCSV             | Grade report generation         |
| **PDF Generation** | OpenPDF           | Transcript creation             |


---

## 🏗️ System Architecture

### **Three-Tier Architecture**

```
┌─────────────────────────────────────────────────────────┐
│                    Presentation Layer                    │
│                        (UI Package)                      │
│  ┌──────────────┐  ┌──────────────┐  ┌──────────────┐  │
│  │   Student    │  │  Instructor  │  │    Admin     │  │
│  │  Dashboard   │  │  Dashboard   │  │  Dashboard   │  │
│  └──────────────┘  └──────────────┘  └──────────────┘  │
└─────────────────────────────────────────────────────────┘
                            │
                            ▼
┌─────────────────────────────────────────────────────────┐
│                    Business Logic Layer                  │
│                     (Service Package)                    │
│  ┌──────────────────────────────────────────────────┐   │
│  │  CourseService │ SectionService │ GradeService   │   │
│  │  StudentService │ InstructorService │ SettingsService│  │
│  └──────────────────────────────────────────────────┘   │
│  ┌──────────────────────────────────────────────────┐   │
│  │  AuthService │ PermissionChecker │ SessionManager│   │
│  └──────────────────────────────────────────────────┘   │
└─────────────────────────────────────────────────────────┘
                            │
                            ▼
┌─────────────────────────────────────────────────────────┐
│                   Data Access Layer                      │
│                      (DAO Package)                       │
│  ┌──────────────────────────────────────────────────┐   │
│  │  CourseDAO │ SectionDAO │ GradeDAO │ StudentDAO  │   │
│  │  InstructorDAO │ EnrollmentDAO │ SettingsDAO     │   │
│  └──────────────────────────────────────────────────┘   │
│  ┌──────────────────────────────────────────────────┐   │
│  │  AuthDAO │ DatabaseConnection (HikariCP)         │   │
│  └──────────────────────────────────────────────────┘   │
└─────────────────────────────────────────────────────────┘
                            │
                            ▼
┌─────────────────────────────────────────────────────────┐
│                    Database Layer                        │
│  ┌──────────────────────┐  ┌──────────────────────┐    │
│  │   erp_auth (Auth)    │  │  erp_main (ERP Data) │    │
│  │  • users_auth        │  │  • students          │    │
│  │  • password_history  │  │  • instructors       │    │
│  │                      │  │  • courses           │    │
│  │                      │  │  • sections          │    │
│  │                      │  │  • enrollments       │    │
│  │                      │  │  • grades            │    │
│  │                      │  │  • settings          │    │
│  └──────────────────────┘  └──────────────────────┘    │
└─────────────────────────────────────────────────────────┘
```

### **Design Patterns Implemented**
- **Singleton** - SessionManager, DatabaseConnection
- **DAO (Data Access Object)** - All database operations
- **Service Layer** - Business logic separation
- **Factory** - Database connection creation
- **Strategy** - Different permission checking strategies
- **Dependency Injection** - Service and DAO injection

---

## ⚡ Quick Start

### **Prerequisites**
-  Java 17 or higher ([Download](https://www.oracle.com/java/technologies/downloads/))
-  MySQL 8.0 or higher ([Download](https://dev.mysql.com/downloads/))
-  Maven 3.6+

### **One-Command Setup** (Recommended)

```bash
# Clone or navigate to project directory
cd /path/to/ERP_Project/ERP

# Run automated setup script
./run-project.sh
```

This script will:
1.  Check MySQL installation and service status
2.  Create databases (erp_auth, erp_main)
3.  Load schemas and seed data
4.  Build project with Maven
5.  Launch the application

---

## 📦 Detailed Setup

### **Step 1: Install MySQL**

```bash
# Ubuntu/Debian
sudo apt update
sudo apt install mysql-server -y
sudo systemctl start mysql
sudo systemctl enable mysql

# Secure installation
sudo mysql_secure_installation
```

### **Step 2: Create Databases**

```bash
# Navigate to project directory
cd /path/to/ERP_Project/ERP

# Run schema scripts
mysql -u root -p < database/01_auth_schema.sql
mysql -u root -p < database/02_erp_schema.sql

# Load sample data
mysql -u root -p < database/03_auth_seed.sql
mysql -u root -p < database/04_erp_seed.sql
```

### **Step 3: Configure Application**

Edit `src/main/resources/application.properties`:

```properties
# Auth Database Configuration
auth.db.url=jdbc:mysql://localhost:3306/erp_auth
auth.db.username=root
auth.db.password=YOUR_MYSQL_PASSWORD

# ERP Database Configuration
erp.db.url=jdbc:mysql://localhost:3306/erp_main
erp.db.username=root
erp.db.password=YOUR_MYSQL_PASSWORD

# Connection Pool Settings (Optional)
hikari.maximum.pool.size=10
hikari.connection.timeout=30000
```

### **Step 4: Build and Run**

```bash
# Build the project
mvn clean package

# Run the application
mvn exec:java -Dexec.mainClass="edu.univ.erp.Main"

# OR run the JAR file
java -jar target/university-erp-1.0.0-jar-with-dependencies.jar
```

---

## 📖 Usage Guide

### **Login Credentials**

| Username | Password     | Role       | Description |
|----------|--------------|------------|-------------|
| `admin1` | `password123` | Admin      | Full system access |
| `inst1`  | `password123` | Instructor | Teaching & grading |
| `stu1`   | `password123` | Student    | Enrollment & grades |
| `stu2`   | `password123` | Student    | Enrollment & grades |

### **Student Workflow**

1. **Login** → Enter credentials
2. **Browse Catalog** → View available courses
3. **Register** → Enroll in sections (auto conflict detection)
4. **View Timetable** → Check weekly schedule
5. **Check Grades** → View assessment scores
6. **Download Transcript** → Generate PDF transcript

### **Instructor Workflow**

1. **Login** → Enter credentials
2. **View Sections** → See assigned teaching sections
3. **Enter Grades** → Record assessment scores
4. **Configure Weights** → Set component weights (must total 100%)
5. **Compute Finals** → Calculate final grades
6. **Export Reports** → Generate CSV for analysis

### **Administrator Workflow**

1. **Login** → Enter credentials
2. **Manage Users** → Create/edit student, instructor, admin accounts
3. **Manage Courses** → Add/edit courses in catalog
4. **Manage Sections** → Create sections, assign instructors
5. **System Settings** → Configure semester, deadlines, maintenance mode

---

## 🔒 Security Features

### **Authentication & Authorization — Protecting User Access**

-  **BCrypt Password Hashing**  
  Passwords are securely hashed using industry-standard BCrypt (cost factor 12). Plaintext passwords are never stored.

-  **Account Lockout**  
  After 5 failed login attempts, accounts are automatically locked to prevent unauthorized access. Users can reset their passwords if needed.

-  **Separate Auth Database**  
  All login credentials are stored in a dedicated authentication database, separate from the main ERP data, reducing the risk of accidental exposure.

-  **Role-Based Access Control (RBAC)**  
  Users only have access to features allowed by their role. Rules are enforced at the service layer to ensure proper permissions.  
  *Example:* Students cannot modify grades; instructors cannot edit other instructors’ courses.

-  **Session Management**  
  Each user session is tracked securely, allowing only one active session per user to prevent concurrent unauthorized logins.

### **Data Security — Keeping Your Data Safe**

- **SQL Injection Protection**  
  All database operations use prepared statements to prevent SQL injection attacks.

- **Password History**  
  Optionally tracks password changes to prevent reuse of old passwords.

- **Maintenance Mode**  
  During system updates, the application can enter read-only mode to protect data integrity.

- **Transaction Management**  
  All database operations comply with ACID principles to ensure reliable data handling with automatic rollback on errors.

### **Security Best Practices — Code Examples**
```java
// Password hashing example
String hashedPassword = PasswordUtil.hashPassword("plaintext");
boolean isValid = PasswordUtil.verifyPassword("plaintext", hashedPassword);

// Permission checking example
PermissionChecker.requireRole(UserRole.INSTRUCTOR);
PermissionChecker.requireAdmin();

---

## 🐛 Troubleshooting

### **Database Connection Issues**

**Problem**: `SQLException: Access denied for user 'root'@'localhost'`

**Solution**:
```bash
# Verify MySQL is running
sudo systemctl status mysql

# Reset root password
sudo mysql
mysql> ALTER USER 'root'@'localhost' IDENTIFIED WITH mysql_native_password BY 'password';
mysql> FLUSH PRIVILEGES;
mysql> exit

# Update application.properties with new password
```

### **Build Failures**

**Problem**: `Maven build fails with dependency errors`

**Solution**:
```bash
# Clean Maven cache
mvn clean

# Force update dependencies
mvn dependency:resolve -U

# Rebuild
mvn clean package
```

### **UI Display Issues**

**Problem**: `UI looks broken or buttons are missing`

**Solution**:
1. Verify Java version: `java -version` (must be 17+)
2. Check FlatLaf is in classpath (Maven handles this)
3. Try running with: `java -Dswing.defaultlaf=javax.swing.plaf.metal.MetalLookAndFeel -jar target/university-erp-1.0.0-jar-with-dependencies.jar`

### **Login Issues**

**Problem**: `Cannot login with default credentials`

**Solution**:
```bash
# Verify seed data loaded
mysql -u root -p
mysql> USE erp_auth;
mysql> SELECT user_id, username, role, status FROM users_auth;
# Should show admin1, inst1, stu1, stu2 with ACTIVE status

# If empty, reload seed data
mysql> exit
mysql -u root -p < database/03_auth_seed.sql
```

### **Application Logs**

Check logs for detailed error messages:
```bash
tail -f logs/erp.log
```

---

# 🎓 University ERP Management System

[![Java](https://img.shields.io/badge/Java-17+-orange.svg)](https://www.oracle.com/java/)
[![MySQL](https://img.shields.io/badge/MySQL-8.0+-blue.svg)](https://www.mysql.com/)
[![Maven](https://img.shields.io/badge/Maven-3.6+-red.svg)](https://maven.apache.org/)
[![License](https://img.shields.io/badge/License-Educational-green.svg)](LICENSE)

A comprehensive enterprise-grade desktop application built with Java Swing for managing university operations including student enrollment, course management, grade tracking, and administrative functions.

---

## 📋 Table of Contents

- [Overview](#overview)
- [Features](#features)
- [Technology Stack](#technology-stack)
- [System Architecture](#system-architecture)
- [Quick Start](#quick-start)
- [Detailed Setup](#detailed-setup)
- [Usage Guide](#usage-guide)
- [Security Features](#security-features)
- [Testing](#testing)
- [Documentation](#documentation)
- [Project Statistics](#project-statistics)
- [Troubleshooting](#troubleshooting)
- [License](#license)

---

## 🎯 Overview

The University ERP Management System is a production-ready desktop application designed to streamline university operations. It provides role-based interfaces for students, instructors, and administrators with a focus on security, usability, and performance.

**Key Highlights:**
- ✅ **Production Ready** - 97 comprehensive tests with 100% pass rate
- ✅ **Secure** - BCrypt password hashing, account lockout, role-based access control
- ✅ **Modern UI** - FlatLaf look and feel with intuitive navigation
- ✅ **Enterprise Architecture** - Dual-database design with connection pooling
- ✅ **Well Documented** - 50+ pages of technical documentation

---

## 🚀 Features

### 👨‍🎓 For Students
- 📚 **Course Catalog** - Browse available courses with real-time seat availability
- ✍️ **Registration** - Enroll in sections with automatic schedule conflict detection
- 📅 **Timetable** - Visual weekly schedule with color-coded sections
- 📊 **Grades** - View detailed assessment scores and final grades
- 📄 **Transcript** - Download official PDF transcripts with GPA calculation
- ⏰ **Deadlines** - Automatic enforcement of add/drop and withdrawal deadlines

### 👨‍🏫 For Instructors
- 📋 **My Sections** - View and manage all assigned teaching sections
- ✏️ **Grade Entry** - Enter scores for assignments, quizzes, midterms, and finals
- ⚖️ **Grade Components** - Configure assessment weights (must total 100%)
- 🧮 **Final Grades** - Automatic calculation of weighted final scores
- 📈 **Statistics** - Class performance analytics and grade distribution
- 📤 **Export** - Generate CSV reports for external analysis
- 👥 **Student Lists** - View enrolled students with contact information

### 👨‍💼 For Administrators
- 👤 **User Management** - Create, update, and manage all user accounts
- 📚 **Course Management** - Maintain course catalog with prerequisites
- 🏫 **Section Management** - Create sections, assign instructors, set schedules
- ⚙️ **System Settings** - Configure semester dates, deadlines, and system parameters
- 🔧 **Maintenance Mode** - Enable read-only mode for system updates
- 🔒 **Account Control** - Lock/unlock accounts, reset passwords
- 📊 **System Reports** - View enrollment statistics and system health

---

## 🛠️ Technology Stack

| Category | Technology | Version | Purpose |
|----------|-----------|---------|---------|
| **Language** | Java SE | 17+ | Core application logic |
| **UI Framework** | Swing + FlatLaf | 3.2.5 | Modern desktop interface |
| **Database** | MySQL | 8.0+ | Persistent data storage |
| **Connection Pool** | HikariCP | 5.0.1 | High-performance DB connections |
| **Security** | BCrypt | - | Password hashing (cost 12) |
| **Build Tool** | Maven | 3.6+ | Dependency management & build |
| **Testing** | JUnit 5 | 5.10.0 | Unit & integration testing |
| **Logging** | SLF4J + Logback | 2.0.7 | Application logging |
| **CSV Export** | OpenCSV | 5.7.1 | Grade report generation |
| **PDF Generation** | OpenPDF | 1.3.30 | Transcript creation |

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
- ☑️ Java 17 or higher ([Download](https://www.oracle.com/java/technologies/downloads/))
- ☑️ MySQL 8.0 or higher ([Download](https://dev.mysql.com/downloads/))
- ☑️ Maven 3.6+ (usually bundled with IDEs)

### **One-Command Setup** (Recommended)

```bash
# Clone or navigate to project directory
cd /path/to/ERP_Project/ERP

# Run automated setup script
./run-project.sh
```

This script will:
1. ✅ Check MySQL installation and service status
2. ✅ Create databases (erp_auth, erp_main)
3. ✅ Load schemas and seed data
4. ✅ Build project with Maven
5. ✅ Launch the application

### **Manual Setup** (Alternative)

See [SETUP.md](SETUP.md) for detailed manual installation steps.

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

### **Authentication & Authorization**
- ✅ **BCrypt Password Hashing** - Industry-standard with cost factor 12
- ✅ **Account Lockout** - Automatic lock after 5 failed login attempts
- ✅ **Separate Auth Database** - Credentials isolated from application data
- ✅ **Role-Based Access Control (RBAC)** - Permissions enforced at service layer
- ✅ **Session Management** - Secure single-session tracking per user

### **Data Security**
- ✅ **SQL Injection Protection** - PreparedStatements throughout
- ✅ **Password History** - Track password changes (optional)
- ✅ **Maintenance Mode** - Read-only system during updates
- ✅ **Transaction Management** - ACID compliance with rollback support

### **Security Best Practices**
```java
// Password hashing example
String hashedPassword = PasswordUtil.hashPassword("plaintext");
boolean isValid = PasswordUtil.verifyPassword("plaintext", hashedPassword);

// Permission checking example
PermissionChecker.requireRole(UserRole.INSTRUCTOR);
PermissionChecker.requireAdmin();
```

---

## 🧪 Testing

### **Test Coverage**

| Category | Tests | Status |
|----------|-------|--------|
| **Auth Layer** | 19 tests | ✅ 100% passing |
| **DAO Layer** | 39 tests | ✅ 100% passing |
| **Service Layer** | 34 tests | ✅ 100% passing |
| **UI Layer** | 5 tests | ✅ 100% passing |
| **TOTAL** | **97 tests** | ✅ **100% passing** |

### **Run Tests**

```bash
# Run all tests
mvn test

# Run specific test class
mvn test -Dtest=AuthServiceTest

# Run with coverage (if configured)
mvn test jacoco:report
```

### **Test Infrastructure**
- **BaseDAOTest** - Database setup/teardown for DAO tests
- **Mock Services** - Mockito for service layer isolation
- **Test Data** - Dedicated seed data for testing
- **Assertions** - JUnit 5 assertions with clear messages

---

## 📚 Documentation

### **Available Documentation**

| Document | Location | Description |
|----------|----------|-------------|
| **README** | [README.md](README.md) | This file - project overview |
| **Setup Guide** | [SETUP.md](SETUP.md) | Detailed installation instructions |
| **How to Run** | [HOW_TO_RUN.md](HOW_TO_RUN.md) | Quick start and troubleshooting |
| **Final Report** | [docs/Final_Project_Report.md](docs/Final_Project_Report.md) | 50+ page technical documentation |
| **Demo Script** | [docs/Demo_Video_Script.md](docs/Demo_Video_Script.md) | Video demonstration guide |

### **Code Documentation**
- JavaDoc comments on all public methods
- Inline comments for complex logic
- README files in key directories
- SQL schema documentation in database files

---

## 📊 Project Statistics

### **Codebase Metrics**
- **Lines of Code**: 8,000+ LOC
- **Java Classes**: 45+ classes
- **Test Cases**: 97 comprehensive tests
- **Test Coverage**: High (DAO & Service layers fully covered)
- **Build Time**: ~10 seconds
- **Startup Time**: ~2 seconds

### **Database Metrics**
- **Tables**: 9 tables across 2 databases
- **Sample Data**: 4 users, 3 courses, 4 sections, 6 enrollments
- **Connection Pool**: 10 max connections per database
- **Query Performance**: < 100ms average

### **Development Timeline**
- **Week 1-2**: Foundation (Auth, DB, UI Framework)
- **Week 3-4**: Student Features (Catalog, Registration, Grades)
- **Week 5-6**: Instructor Features (Grade Entry, Statistics)
- **Week 7**: Admin Features (User/Course/Section Management)
- **Week 8-9**: Testing, Documentation, Polish

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

## 🎓 Academic Information

**Course**: Database Management Systems  
**Institution**: IIIT Delhi  
**Semester**: Fall 2025  
**Team Size**: Individual Project  

### **Learning Outcomes**
- ✅ Multi-tier application architecture
- ✅ Relational database design and normalization
- ✅ Transaction management and ACID properties
- ✅ Security best practices (BCrypt, RBAC, SQL injection prevention)
- ✅ Connection pooling and performance optimization
- ✅ Comprehensive unit testing with JUnit 5
- ✅ Modern Java development with Maven
- ✅ UI/UX design with Swing and FlatLaf

---

## 📄 License

This project is for **educational purposes only** as part of the IIITD curriculum.

**Restrictions**:
- ❌ Not for commercial use
- ❌ Not for redistribution
- ✅ May be used as learning reference
- ✅ May be included in academic portfolios with proper attribution

---

## 🙏 Acknowledgments

- **IIIT Delhi** - Course curriculum and guidance
- **FlatLaf** - Modern look and feel library
- **HikariCP** - High-performance connection pooling
- **JUnit** - Testing framework
- **OpenCSV & OpenPDF** - Document generation libraries


---

<div align="center">

**🚀 Built with ❤️ using Java, Swing, and MySQL**

*Last Updated: October 2025*

</div>

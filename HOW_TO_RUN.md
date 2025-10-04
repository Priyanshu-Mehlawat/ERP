# 🚀 How to Run the University ERP Project

## Current Status
✅ **Project compiles successfully!**
❌ **MySQL is not installed on your system**

---

## 📋 **Complete Setup Guide**

### **Step 1: Install MySQL Server**

Run these commands in your terminal:

```bash
# Update package list
sudo apt update

# Install MySQL Server
sudo apt install mysql-server -y

# Start MySQL service
sudo systemctl start mysql
sudo systemctl enable mysql

# Verify MySQL is running
sudo systemctl status mysql
```

### **Step 2: Secure MySQL Installation**

```bash
# Run the security script
sudo mysql_secure_installation
```

**Answer the prompts:**
- Set root password: **`password`** (or your choice)
- Remove anonymous users: **Yes**
- Disallow root login remotely: **Yes**
- Remove test database: **Yes**
- Reload privilege tables: **Yes**

### **Step 3: Test MySQL Connection**

```bash
# Login to MySQL (use the password you just set)
mysql -u root -p

# Once logged in, you should see:
# mysql>

# Type 'exit' to quit
exit
```

---

## 🎯 **Option A: Automated Setup (Easiest)**

Once MySQL is installed, run the automated script:

```bash
# Navigate to project directory
cd /home/ronak-anand/Desktop/ERP_Project/ERP

# Run the automated setup script
./run-project.sh
```

This script will:
1. ✅ Check if MySQL is installed and running
2. ✅ Create both databases (erp_auth and erp_main)
3. ✅ Load all schemas and seed data
4. ✅ Update configuration with your MySQL password
5. ✅ Build the project with Maven
6. ✅ Run the application

---

## 🔧 **Option B: Manual Setup**

If you prefer to do it manually:

### **1. Create Databases**

```bash
# Run database creation scripts
mysql -u root -p < database/01_auth_schema.sql
mysql -u root -p < database/02_erp_schema.sql
mysql -u root -p < database/03_auth_seed.sql
mysql -u root -p < database/04_erp_seed.sql
```

### **2. Update Configuration**

Edit `src/main/resources/application.properties`:

```properties
# Change these lines with your MySQL root password
auth.db.password=YOUR_MYSQL_PASSWORD
erp.db.password=YOUR_MYSQL_PASSWORD
```

### **3. Build the Project**

```bash
mvn clean package
```

### **4. Run the Application**

```bash
mvn exec:java -Dexec.mainClass="edu.univ.erp.Main"
```

---

## 🔑 **Login Credentials**

Once the application starts, use these credentials:

| Username | Password     | Role       |
|----------|--------------|------------|
| admin1   | password123  | Admin      |
| inst1    | password123  | Instructor |
| stu1     | password123  | Student    |
| stu2     | password123  | Student    |

---

## ✅ **What Works Right Now**

1. **Login System**
   - Secure authentication with BCrypt
   - Failed login tracking
   - Account lockout after 5 attempts

2. **Role-Based Dashboards**
   - Admin dashboard with 6 management options
   - Instructor dashboard with 4 teaching tools
   - Student dashboard with 5 student features

3. **Session Management**
   - Tracks currently logged-in user
   - Proper logout functionality

4. **Database**
   - Two separate databases (Auth + ERP)
   - Connection pooling with HikariCP
   - Sample data pre-loaded

---

## 🐛 **Troubleshooting**

### **Problem: "Command 'mysql' not found"**

**Solution:** MySQL is not installed. Follow Step 1 above.

```bash
sudo apt install mysql-server -y
```

### **Problem: "Access denied for user 'root'@'localhost'"**

**Solution:** Wrong password or MySQL not secured.

```bash
# Reset root password
sudo mysql
mysql> ALTER USER 'root'@'localhost' IDENTIFIED WITH mysql_native_password BY 'password';
mysql> FLUSH PRIVILEGES;
mysql> exit
```

### **Problem: "Can't connect to MySQL server"**

**Solution:** MySQL is not running.

```bash
# Start MySQL
sudo systemctl start mysql

# Check status
sudo systemctl status mysql
```

### **Problem: Build fails with errors**

**Solution:** Clean and rebuild.

```bash
mvn clean
mvn compile
mvn package
```

### **Problem: "Database 'erp_auth' doesn't exist"**

**Solution:** Run the database setup scripts.

```bash
mysql -u root -p < database/01_auth_schema.sql
mysql -u root -p < database/02_erp_schema.sql
mysql -u root -p < database/03_auth_seed.sql
mysql -u root -p < database/04_erp_seed.sql
```

---

## 📊 **Verify Database Setup**

After running the scripts, verify the databases:

```bash
# Login to MySQL
mysql -u root -p

# Check databases
SHOW DATABASES;
# Should show: erp_auth, erp_main

# Check Auth DB tables
USE erp_auth;
SHOW TABLES;
# Should show: users_auth, password_history

# Check users
SELECT user_id, username, role FROM users_auth;
# Should show: admin1, inst1, stu1, stu2

# Check ERP DB tables
USE erp_main;
SHOW TABLES;
# Should show: students, instructors, courses, sections, enrollments, grades, settings

# Exit
exit
```

---

## 🚀 **Quick Commands**

```bash
# Install MySQL
sudo apt install mysql-server -y

# Setup and run (automated)
./run-project.sh

# OR Manual steps:
mysql -u root -p < database/01_auth_schema.sql
mysql -u root -p < database/02_erp_schema.sql
mysql -u root -p < database/03_auth_seed.sql
mysql -u root -p < database/04_erp_seed.sql

# Update config file with your password
nano src/main/resources/application.properties

# Build
mvn clean package

# Run
mvn exec:java -Dexec.mainClass="edu.univ.erp.Main"
```

---

## 📝 **Next Steps After First Run**

1. ✅ Test login with all 4 users
2. ✅ Verify dashboards open correctly
3. ✅ Check database connections work
4. ✅ Review logs in `logs/erp.log`
5. ⏳ Start implementing Week 3-4 features (Student catalog, registration, etc.)

---

## 🎓 **Need Help?**

- Check logs: `tail -f logs/erp.log`
- Review documentation: `README.md`, `SETUP.md`, `GETTING_STARTED.md`
- Verify database: Run SQL queries above
- Check configuration: `application.properties`

---

**Good luck! Your project is ready to run once MySQL is installed!** 🚀

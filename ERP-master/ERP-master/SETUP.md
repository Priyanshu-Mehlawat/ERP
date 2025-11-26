# 🔧 University ERP System - Setup Guide

> **Quick Setup**: Use `./run-project.sh` for automated installation!  
> **Detailed Guide**: See [README.md](README.md) for comprehensive documentation.

---

## ⚡ Quick Setup (Automated)

```bash
# Navigate to project directory
cd /path/to/ERP_Project/ERP

# Run automated setup script
./run-project.sh
```

The script will handle everything automatically:
- ✅ Check MySQL installation
- ✅ Create databases
- ✅ Load schemas and seed data
- ✅ Build project
- ✅ Launch application

---

## 🔨 Manual Setup (Step-by-Step)

### **Step 1: Install Prerequisites**

```bash
# Java 17+ (verify)
java -version

# MySQL 8.0+ (install if needed)
sudo apt update
sudo apt install mysql-server -y
sudo systemctl start mysql
sudo systemctl enable mysql

# Secure MySQL
sudo mysql_secure_installation

# Maven 3.6+ (verify)
mvn -version
```

### **Step 2: Create Databases**

```bash
# Run database scripts
mysql -u root -p < database/01_auth_schema.sql
mysql -u root -p < database/02_erp_schema.sql
mysql -u root -p < database/03_auth_seed.sql
mysql -u root -p < database/04_erp_seed.sql
```

### **Step 3: Configure Connection**

Edit `src/main/resources/application.properties`:

```properties
auth.db.password=YOUR_MYSQL_PASSWORD
erp.db.password=YOUR_MYSQL_PASSWORD
```

### **Step 4: Build and Run**

```bash
# Build
mvn clean package

# Run
mvn exec:java -Dexec.mainClass="edu.univ.erp.Main"
```

---

## 🔑 Default Credentials

| Username | Password     | Role       |
|----------|--------------|------------|
| `admin1` | `password123` | Admin      |
| `inst1`  | `password123` | Instructor |
| `stu1`   | `password123` | Student    |
| `stu2`   | `password123` | Student    |

---

## 🐛 Common Issues

### **"Access denied for user 'root'"**

```bash
sudo mysql
mysql> ALTER USER 'root'@'localhost' IDENTIFIED WITH mysql_native_password BY 'password';
mysql> FLUSH PRIVILEGES;
mysql> exit
```

### **"Database does not exist"**

```bash
mysql -u root -p < database/01_auth_schema.sql
mysql -u root -p < database/02_erp_schema.sql
```

### **Build fails**

```bash
mvn clean
mvn dependency:resolve -U
mvn clean package
```

### **Check logs**

```bash
tail -f logs/erp.log
```

---

## 🖥️ IDE Setup

### **IntelliJ IDEA**
1. Open project folder
2. Wait for Maven import
3. Set JDK to 17+
4. Run `Main.java`

### **Eclipse**
1. Import → Existing Maven Project
2. Set Java compliance to 17
3. Run `Main.java`

### **VS Code**
1. Install Java Extension Pack
2. Open project folder
3. Run `Main.java` from sidebar

---

## 📊 Verify Installation

```sql
-- Login to MySQL
mysql -u root -p

-- Check databases
SHOW DATABASES;

-- Check users (Auth DB)
USE erp_auth;
SELECT user_id, username, role FROM users_auth;

-- Check courses (ERP DB)
USE erp_main;
SELECT * FROM courses;

-- Exit
exit
```

---

## 🔄 Reset Database

```bash
# Drop databases
mysql -u root -p -e "DROP DATABASE erp_auth; DROP DATABASE erp_main;"

# Recreate from scripts
mysql -u root -p < database/01_auth_schema.sql
mysql -u root -p < database/02_erp_schema.sql
mysql -u root -p < database/03_auth_seed.sql
mysql -u root -p < database/04_erp_seed.sql
```

---

## 📚 Additional Resources

- **Full Documentation**: [README.md](README.md)
- **Technical Report**: [docs/Final_Project_Report.md](docs/Final_Project_Report.md)
- **Demo Script**: [docs/Demo_Video_Script.md](docs/Demo_Video_Script.md)

---

**For detailed troubleshooting and advanced configuration, see [README.md](README.md#troubleshooting)**

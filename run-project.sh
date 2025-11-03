#!/bin/bash

# ============================================
# University ERP - Complete Setup and Run
# ============================================

echo "================================================"
echo "  University ERP System - Setup & Run Guide"
echo "================================================"
echo ""

# Check if MySQL is installed
if ! command -v mysql &> /dev/null; then
    echo "❌ MySQL is NOT installed!"
    echo ""
    echo "📋 INSTALLATION STEPS:"
    echo ""
    echo "1. Install MySQL Server:"
    echo "   sudo apt update"
    echo "   sudo apt install mysql-server -y"
    echo ""
    echo "2. Start MySQL:"
    echo "   sudo systemctl start mysql"
    echo "   sudo systemctl enable mysql"
    echo ""
    echo "3. Secure MySQL (set root password):"
    echo "   sudo mysql_secure_installation"
    echo "   - Set root password: password (or your choice)"
    echo "   - Remove anonymous users: Yes"
    echo "   - Disallow root login remotely: Yes"
    echo "   - Remove test database: Yes"
    echo "   - Reload privilege tables: Yes"
    echo ""
    echo "4. Verify MySQL is running:"
    echo "   sudo systemctl status mysql"
    echo ""
    echo "5. Re-run this script after MySQL installation"
    echo ""
    exit 1
fi

echo "✅ MySQL is installed"
echo ""

# Check if MySQL is running
if ! systemctl is-active --quiet mysql; then
    echo "⚠️  MySQL is not running. Starting MySQL..."
    sudo systemctl start mysql
    sleep 2
fi

if systemctl is-active --quiet mysql; then
    echo "✅ MySQL is running"
else
    echo "❌ Failed to start MySQL"
    echo "   Try: sudo systemctl start mysql"
    exit 1
fi

echo ""
echo "================================================"
echo "  Step 1: Database Setup"
echo "================================================"
echo ""

# Prompt for MySQL root password
read -sp "Enter MySQL root password: " MYSQL_ROOT_PASS
echo ""
echo ""

# Test MySQL connection
if mysql -u root -p"$MYSQL_ROOT_PASS" -e "SELECT 1" &> /dev/null; then
    echo "✅ MySQL connection successful"
else
    echo "❌ MySQL connection failed"
    echo "   Please check your password"
    exit 1
fi

echo ""
echo "Creating databases and loading schemas..."
echo ""

# Create Auth DB
echo "📁 Creating Auth Database (erp_auth)..."
mysql -u root -p"$MYSQL_ROOT_PASS" < database/01_auth_schema.sql
if [ $? -eq 0 ]; then
    echo "   ✅ Auth database created"
else
    echo "   ❌ Failed to create Auth database"
    exit 1
fi

# Create ERP DB
echo "📁 Creating ERP Database (erp_main)..."
mysql -u root -p"$MYSQL_ROOT_PASS" < database/02_erp_schema.sql
if [ $? -eq 0 ]; then
    echo "   ✅ ERP database created"
else
    echo "   ❌ Failed to create ERP database"
    exit 1
fi

# Load Auth seed data
echo "📊 Loading Auth seed data..."
mysql -u root -p"$MYSQL_ROOT_PASS" < database/03_auth_seed.sql
if [ $? -eq 0 ]; then
    echo "   ✅ Auth seed data loaded"
else
    echo "   ❌ Failed to load Auth seed data"
    exit 1
fi

# Load ERP seed data
echo "📊 Loading ERP seed data..."
mysql -u root -p"$MYSQL_ROOT_PASS" < database/04_erp_seed.sql
if [ $? -eq 0 ]; then
    echo "   ✅ ERP seed data loaded"
else
    echo "   ❌ Failed to load ERP seed data"
    exit 1
fi

echo ""
echo "================================================"
echo "  Step 2: Update Configuration"
echo "================================================"
echo ""

# Update application.properties with password
CONFIG_FILE="src/main/resources/application.properties"
if [ -f "$CONFIG_FILE" ]; then
    echo "📝 Updating $CONFIG_FILE with MySQL password..."
    
    # Create backup
    cp "$CONFIG_FILE" "$CONFIG_FILE.backup"
    
    # Update password
    sed -i "s/^auth.db.password=.*/auth.db.password=$MYSQL_ROOT_PASS/" "$CONFIG_FILE"
    sed -i "s/^erp.db.password=.*/erp.db.password=$MYSQL_ROOT_PASS/" "$CONFIG_FILE"
    
    echo "   ✅ Configuration updated"
    echo "   💾 Backup saved as: $CONFIG_FILE.backup"
else
    echo "   ❌ Configuration file not found!"
    exit 1
fi

echo ""
echo "================================================"
echo "  Step 3: Build Project"
echo "================================================"
echo ""

echo "🔨 Building project with Maven..."
mvn clean package -DskipTests

if [ $? -eq 0 ]; then
    echo ""
    echo "   ✅ Build successful!"
else
    echo ""
    echo "   ❌ Build failed!"
    echo "   Check the errors above"
    exit 1
fi

echo ""
echo "================================================"
echo "  Step 4: Run Application"
echo "================================================"
echo ""

echo "🚀 Starting University ERP System..."
echo ""
echo "Default Login Credentials:"
echo "  • Admin:      admin1 / password123"
echo "  • Instructor: inst1  / password123"
echo "  • Student 1:  stu1   / password123"
echo "  • Student 2:  stu2   / password123"
echo ""
echo "Press Ctrl+C to stop the application"
echo ""
echo "================================================"
echo ""

# Run the application
mvn exec:java -Dexec.mainClass="edu.univ.erp.Main"

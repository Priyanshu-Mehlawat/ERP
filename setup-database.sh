#!/bin/bash

# University ERP - Database Setup Script
# This script sets up both Auth and ERP databases

echo "==========================================="
echo "  University ERP - Database Setup"
echo "==========================================="
echo ""

# Check if MySQL is installed
if ! command -v mysql &> /dev/null; then
    echo "❌ MySQL is not installed or not in PATH"
    echo "   Please install MySQL first"
    exit 1
fi

echo "✅ MySQL found"
echo ""

# Prompt for MySQL credentials
echo "Please enter your MySQL credentials:"
read -p "Username [root]: " MYSQL_USER
MYSQL_USER=${MYSQL_USER:-root}

read -sp "Password: " MYSQL_PASS
echo ""
echo ""

# Test connection
echo "Testing MySQL connection..."
mysql -u"$MYSQL_USER" -p"$MYSQL_PASS" -e "SELECT 1" > /dev/null 2>&1

if [ $? -ne 0 ]; then
    echo "❌ Failed to connect to MySQL"
    echo "   Please check your credentials"
    exit 1
fi

echo "✅ Connected to MySQL successfully"
echo ""

# Create databases and run schema scripts
echo "Creating Auth database..."
mysql -u"$MYSQL_USER" -p"$MYSQL_PASS" < database/01_auth_schema.sql
if [ $? -eq 0 ]; then
    echo "✅ Auth database created"
else
    echo "❌ Failed to create Auth database"
    exit 1
fi

echo "Creating ERP database..."
mysql -u"$MYSQL_USER" -p"$MYSQL_PASS" < database/02_erp_schema.sql
if [ $? -eq 0 ]; then
    echo "✅ ERP database created"
else
    echo "❌ Failed to create ERP database"
    exit 1
fi

echo ""
echo "Loading sample data..."

echo "Loading Auth seed data..."
mysql -u"$MYSQL_USER" -p"$MYSQL_PASS" < database/03_auth_seed.sql
if [ $? -eq 0 ]; then
    echo "✅ Auth seed data loaded"
else
    echo "❌ Failed to load Auth seed data"
    exit 1
fi

echo "Loading ERP seed data..."
mysql -u"$MYSQL_USER" -p"$MYSQL_PASS" < database/04_erp_seed.sql
if [ $? -eq 0 ]; then
    echo "✅ ERP seed data loaded"
else
    echo "❌ Failed to load ERP seed data"
    exit 1
fi

echo ""
echo "==========================================="
echo "  ✅ Database setup complete!"
echo "==========================================="
echo ""
echo "Default credentials:"
echo "  admin1 / password123 (Admin)"
echo "  inst1 / password123 (Instructor)"
echo "  stu1 / password123 (Student)"
echo "  stu2 / password123 (Student)"
echo ""
echo "Next steps:"
echo "  1. Update src/main/resources/application.properties"
echo "  2. Run: mvn clean package"
echo "  3. Run: mvn exec:java -Dexec.mainClass=\"edu.univ.erp.Main\""
echo ""

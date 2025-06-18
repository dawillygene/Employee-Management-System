# Database Triggers - Employee Management System

## 🎯 Overview

This system automatically creates database triggers during Spring Boot application startup to handle synchronization between `users` and `employees` tables.

## 🚀 Automatic Setup

### Components Created:

1. **DatabaseTriggerInitializer** - Runs at startup (Order 1)
2. **DataLoader** - Runs after triggers are created (Order 2)
3. **TriggerTestService** - Tests trigger functionality
4. **TriggerManagementController** - API endpoints for trigger management

## 📋 Triggers Created

### 1. User-to-Employee Trigger (`tr_user_employee_sync_insert`)
- **When**: New user registered with role 'EMPLOYEE'
- **Action**: Creates employee record with:
  - Auto-generated Employee ID (EMP001, EMP002, etc.)
  - Email from user record
  - Default department: "General"
  - Default position: "Employee"
  - Default salary: $30,000
  - Status: "ACTIVE"

### 2. Employee-to-User Trigger (`tr_employee_user_sync_insert`)
- **When**: New employee record created by Admin/HR
- **Action**: Creates user account with:
  - Username: Generated from email (before @)
  - Password: BCrypt hash of "password123"
  - Role: "EMPLOYEE"
  - Status: Enabled

### 3. Sync Update Triggers
- **User Email Update**: Syncs to employee table
- **Employee Email Update**: Syncs to user table
- **Role Changes**: Updates employee status accordingly
- **Status Changes**: Employee termination disables user account

### 4. User Deletion Trigger
- **When**: User account deleted
- **Action**: Marks employee as "TERMINATED"

## 🔧 Application Startup Process

```
1. DatabaseTriggerInitializer (Order 1)
   ├── Checks if triggers exist
   ├── Drops existing triggers (if any)
   ├── Creates new triggers from /resources/db/triggers.sql
   └── Logs success/failure

2. DataLoader (Order 2)
   ├── Creates default users and employees
   ├── Runs trigger tests
   └── Validates trigger functionality
```

## 🧪 Testing

### Automatic Tests (During Startup)
The system automatically tests triggers during startup:

```java
// Test 1: User to Employee trigger
testUserToEmployeeTrigger("test.employee1", "test.employee1@company.com");

// Test 2: Employee to User trigger  
testEmployeeToUserTrigger("EMP999", "Test Employee 2", "test.employee2@company.com");
```

### Manual API Testing (Admin Only)

#### Test All Triggers
```bash
POST /api/admin/triggers/test
```

#### Test User-to-Employee Trigger
```bash
POST /api/admin/triggers/test/user-to-employee
Content-Type: application/x-www-form-urlencoded

username=john.test&email=john.test@company.com
```

#### Test Employee-to-User Trigger
```bash
POST /api/admin/triggers/test/employee-to-user
Content-Type: application/x-www-form-urlencoded

employeeId=EMP888&fullName=Jane Test&email=jane.test@company.com
```

#### Check Trigger Status
```bash
GET /api/admin/triggers/status
```

#### Reinitialize Triggers
```bash
POST /api/admin/triggers/reinitialize
```

## 📁 Files Created

```
src/main/java/com/employeemanagementsystem/v1/
├── config/
│   └── DatabaseTriggerInitializer.java     # Auto-creates triggers
├── service/
│   └── TriggerTestService.java             # Tests trigger functionality
└── controller/
    └── TriggerManagementController.java    # API for trigger management

src/main/resources/db/
└── triggers.sql                            # SQL trigger definitions
```

## 🔐 Security Features

- **BCrypt Password Hashing**: Default password "password123" is properly hashed
- **Unique Constraints**: Prevents duplicate users/employees
- **Admin Only Access**: Trigger management APIs require ADMIN role
- **Email Validation**: Ensures data consistency
- **Error Handling**: Graceful failure handling

## 📊 Default Values

### New Employee (from User)
```sql
employee_id: Auto-generated (EMP001, EMP002, ...)
full_name: Username or "New Employee"
department: "General"
position: "Employee"
salary: 30000.00
status: "ACTIVE"
```

### New User (from Employee)
```sql
username: Email prefix (before @) + number if needed
password: "$2a$10$N9qo8uLOickgx2ZMRZoMyeIjZAgcfl7p92ldGxad68LJZdL17lhWy" (password123)
role: "EMPLOYEE"
enabled: true
```

## 🚨 Logs to Watch

During startup, look for these log messages:

```
✅ SUCCESS:
[INFO] Initializing database triggers...
[INFO] Database triggers initialized successfully
[INFO] ✅ Trigger SUCCESS: Employee automatically created with ID: EMP001
[INFO] ✅ Trigger SUCCESS: User automatically created with username: test.employee2

❌ FAILURE:
[ERROR] Failed to initialize database triggers: ...
[ERROR] ❌ Trigger FAILED: No employee record created for user ...
```

## 🔄 Manual Management

If triggers need to be recreated manually:

1. **Via API** (Recommended):
   ```bash
   POST /api/admin/triggers/reinitialize
   ```

2. **Via Database**:
   ```sql
   -- Run the contents of /resources/db/triggers.sql
   ```

## 🛠️ Troubleshooting

### Common Issues:

1. **Triggers Not Created**
   - Check database permissions
   - Verify MySQL version supports triggers
   - Check application logs for errors

2. **Duplicate Key Errors**
   - Employee ID generation collision
   - Email uniqueness violation
   - Username conflicts

3. **Trigger Not Firing**
   - Verify triggers exist: `SHOW TRIGGERS;`
   - Check trigger conditions
   - Review application logs

### Debug Commands:
```sql
-- Check existing triggers
SHOW TRIGGERS;

-- View trigger definitions
SHOW CREATE TRIGGER tr_user_employee_sync_insert;

-- Check for errors
SHOW WARNINGS;
```

## 📈 Performance Notes

- Triggers execute for every INSERT/UPDATE/DELETE
- Keep trigger logic minimal for performance
- Monitor performance with large datasets
- Consider indexing on email columns for better performance

---

**Note**: The triggers are automatically created when the application starts. No manual intervention required for normal operation.

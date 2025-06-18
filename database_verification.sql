-- ==============================================================================
-- DATABASE TRIGGER VERIFICATION SCRIPT
-- ==============================================================================
-- Run these queries in MySQL Workbench or any MySQL client to verify triggers

-- ==============================================================================
-- 1. CHECK IF TRIGGERS EXIST
-- ==============================================================================

-- List all triggers in current database
SELECT 
    TRIGGER_NAME,
    EVENT_MANIPULATION,
    EVENT_OBJECT_TABLE,
    TRIGGER_SCHEMA,
    CREATED
FROM information_schema.TRIGGERS 
WHERE TRIGGER_SCHEMA = DATABASE()
ORDER BY EVENT_OBJECT_TABLE, EVENT_MANIPULATION;

-- Expected Output: Should show 5 triggers
-- tr_user_employee_sync_insert    (INSERT on users)
-- tr_employee_user_sync_insert    (INSERT on employees) 
-- tr_user_employee_sync_update    (UPDATE on users)
-- tr_employee_user_sync_update    (UPDATE on employees)
-- tr_user_employee_sync_delete    (DELETE on users)

-- ==============================================================================
-- 2. CHECK TRIGGER DEFINITIONS
-- ==============================================================================

-- View specific trigger code
SHOW CREATE TRIGGER tr_user_employee_sync_insert;
SHOW CREATE TRIGGER tr_employee_user_sync_insert;

-- ==============================================================================
-- 3. CHECK HELPER FUNCTION
-- ==============================================================================

-- List custom functions
SELECT 
    ROUTINE_NAME,
    ROUTINE_TYPE,
    CREATED,
    LAST_ALTERED
FROM information_schema.ROUTINES 
WHERE ROUTINE_SCHEMA = DATABASE() 
AND ROUTINE_TYPE = 'FUNCTION';

-- Test the helper function
SELECT generate_employee_id() AS next_employee_id;

-- ==============================================================================
-- 4. MANUAL TRIGGER TESTING
-- ==============================================================================

-- TEST 1: User-to-Employee Trigger
-- Create a user with EMPLOYEE role and see if employee is auto-created

-- Check current counts
SELECT 'BEFORE TEST' as status, 
       (SELECT COUNT(*) FROM users WHERE role = 'EMPLOYEE') as employee_users,
       (SELECT COUNT(*) FROM employees) as total_employees;

-- Insert test user (this should trigger employee creation)
INSERT INTO users (username, password, email, role, enabled) 
VALUES ('trigger.test1', '$2a$10$N9qo8uLOickgx2ZMRZoMyeIjZAgcfl7p92ldGxad68LJZdL17lhWy', 
        'trigger.test1@company.com', 'EMPLOYEE', TRUE);

-- Check if employee was created automatically
SELECT 'AFTER USER INSERT' as status,
       (SELECT COUNT(*) FROM users WHERE role = 'EMPLOYEE') as employee_users,
       (SELECT COUNT(*) FROM employees) as total_employees;

-- Verify the specific records
SELECT 'USER RECORD' as type, u.id, u.username, u.email, u.role, u.enabled
FROM users u 
WHERE u.email = 'trigger.test1@company.com'
UNION ALL
SELECT 'EMPLOYEE RECORD' as type, e.id, e.employee_id, e.email, e.status, e.full_name
FROM employees e 
WHERE e.email = 'trigger.test1@company.com';

-- ==============================================================================

-- TEST 2: Employee-to-User Trigger  
-- Create an employee and see if user is auto-created

-- Insert test employee (this should trigger user creation)
INSERT INTO employees (employee_id, full_name, email, phone, department, position, salary, date_of_joining, status) 
VALUES ('TRIG001', 'Trigger Test User', 'trigger.test2@company.com', '1234567890', 
        'IT', 'Developer', 45000, CURDATE(), 'ACTIVE');

-- Check if user was created automatically  
SELECT 'AFTER EMPLOYEE INSERT' as status,
       (SELECT COUNT(*) FROM users WHERE role = 'EMPLOYEE') as employee_users,
       (SELECT COUNT(*) FROM employees) as total_employees;

-- Verify the specific records
SELECT 'EMPLOYEE RECORD' as type, e.id, e.employee_id, e.email, e.status, e.full_name
FROM employees e 
WHERE e.email = 'trigger.test2@company.com'
UNION ALL
SELECT 'USER RECORD' as type, u.id, u.username, u.email, u.role, u.enabled
FROM users u 
WHERE u.email = 'trigger.test2@company.com';

-- ==============================================================================

-- TEST 3: Update Sync Testing
-- Test if email updates sync between tables

-- Update user email and check if employee email updates
UPDATE users 
SET email = 'trigger.test1.updated@company.com' 
WHERE email = 'trigger.test1@company.com';

-- Check if employee email was updated by trigger
SELECT 'EMAIL SYNC TEST' as test,
       u.email as user_email,
       e.email as employee_email,
       CASE WHEN u.email = e.email THEN 'SYNCED ✅' ELSE 'NOT SYNCED ❌' END as sync_status
FROM users u
JOIN employees e ON u.email = e.email
WHERE u.email = 'trigger.test1.updated@company.com';

-- ==============================================================================

-- TEST 4: Status Sync Testing
-- Test if employee termination disables user account

-- Terminate employee and check if user is disabled
UPDATE employees 
SET status = 'TERMINATED' 
WHERE email = 'trigger.test2@company.com';

-- Check if user was disabled by trigger
SELECT 'STATUS SYNC TEST' as test,
       e.status as employee_status,
       u.enabled as user_enabled,
       CASE 
         WHEN e.status = 'TERMINATED' AND u.enabled = FALSE THEN 'SYNCED ✅'
         ELSE 'NOT SYNCED ❌' 
       END as sync_status
FROM employees e
JOIN users u ON e.email = u.email
WHERE e.email = 'trigger.test2@company.com';

-- ==============================================================================
-- 5. COMPREHENSIVE SYNC CHECK
-- ==============================================================================

-- Check all EMPLOYEE users have corresponding employee records
SELECT 
    'SYNC REPORT' as report_type,
    COUNT(*) as total_employee_users,
    SUM(CASE WHEN e.email IS NOT NULL THEN 1 ELSE 0 END) as users_with_employee_records,
    SUM(CASE WHEN e.email IS NULL THEN 1 ELSE 0 END) as users_without_employee_records
FROM users u
LEFT JOIN employees e ON u.email = e.email
WHERE u.role = 'EMPLOYEE';

-- List users without employee records (should be 0 if triggers work)
SELECT 'ORPHANED USERS' as issue, u.username, u.email, u.role
FROM users u
LEFT JOIN employees e ON u.email = e.email
WHERE u.role = 'EMPLOYEE' AND e.email IS NULL;

-- Check all active employees have corresponding user records
SELECT 
    'EMPLOYEE SYNC REPORT' as report_type,
    COUNT(*) as total_active_employees,
    SUM(CASE WHEN u.email IS NOT NULL THEN 1 ELSE 0 END) as employees_with_user_records,
    SUM(CASE WHEN u.email IS NULL THEN 1 ELSE 0 END) as employees_without_user_records
FROM employees e
LEFT JOIN users u ON e.email = u.email AND u.role = 'EMPLOYEE'
WHERE e.status = 'ACTIVE';

-- List employees without user records (should be 0 if triggers work)
SELECT 'ORPHANED EMPLOYEES' as issue, e.employee_id, e.full_name, e.email, e.status
FROM employees e
LEFT JOIN users u ON e.email = u.email AND u.role = 'EMPLOYEE'
WHERE e.status = 'ACTIVE' AND u.email IS NULL;

-- ==============================================================================
-- 6. CLEANUP TEST DATA (OPTIONAL)
-- ==============================================================================

-- Remove test data created during verification
-- DELETE FROM users WHERE email IN ('trigger.test1@company.com', 'trigger.test1.updated@company.com', 'trigger.test2@company.com');
-- DELETE FROM employees WHERE email IN ('trigger.test1@company.com', 'trigger.test1.updated@company.com', 'trigger.test2@company.com');

-- ==============================================================================
-- 7. TRIGGER PERFORMANCE CHECK
-- ==============================================================================

-- Check trigger execution statistics (if available)
SELECT 
    TRIGGER_NAME,
    EVENT_OBJECT_TABLE,
    ACTION_TIMING,
    EVENT_MANIPULATION,
    CREATED
FROM information_schema.TRIGGERS 
WHERE TRIGGER_SCHEMA = DATABASE()
ORDER BY CREATED DESC;

-- ==============================================================================
-- 8. ERROR LOG CHECK
-- ==============================================================================

-- Check for any MySQL errors related to triggers
-- SHOW WARNINGS;
-- SHOW ERRORS;

-- ==============================================================================
-- QUICK VERIFICATION SUMMARY
-- ==============================================================================

-- Run this single query to get a quick status overview
SELECT 
    'TRIGGER STATUS SUMMARY' as summary,
    (SELECT COUNT(*) FROM information_schema.TRIGGERS WHERE TRIGGER_SCHEMA = DATABASE()) as triggers_count,
    (SELECT COUNT(*) FROM users WHERE role = 'EMPLOYEE') as employee_users,
    (SELECT COUNT(*) FROM employees WHERE status = 'ACTIVE') as active_employees,
    (SELECT COUNT(*) FROM information_schema.ROUTINES WHERE ROUTINE_SCHEMA = DATABASE() AND ROUTINE_NAME = 'generate_employee_id') as helper_functions;

-- Expected Results:
-- triggers_count: 5
-- employee_users: Should match active_employees (or be close)
-- active_employees: Should have corresponding users
-- helper_functions: 1

-- ==============================================================================

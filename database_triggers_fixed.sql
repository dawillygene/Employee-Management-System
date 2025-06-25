-- ==============================================================================
-- Employee Management System Database Triggers - FIXED VERSION
-- ==============================================================================
-- These triggers handle automatic data synchronization between users and employees tables
-- Fixed to avoid circular references and table modification conflicts

DELIMITER /
/

-- Drop existing triggers if they exist
DROP TRIGGER IF EXISTS TR_USER_EMPLOYEE_SYNC_INSERT//
DROP TRIGGER IF EXISTS TR_EMPLOYEE_USER_SYNC_INSERT//
DROP TRIGGER IF EXISTS TR_USER_EMPLOYEE_SYNC_UPDATE//
DROP TRIGGER IF EXISTS TR_EMPLOYEE_USER_SYNC_UPDATE//
DROP TRIGGER IF EXISTS TR_USER_EMPLOYEE_SYNC_DELETE//
DROP TRIGGER IF EXISTS TR_EMPLOYEE_USER_SYNC_DELETE//
 
-- ==============================================================================
-- TRIGGER 1: Auto-create employee record when new user with EMPLOYEE role is created
-- ==============================================================================
CREATE TRIGGER TR_USER_EMPLOYEE_SYNC_INSERT
AFTER INSERT ON USERS
FOR EACH ROW
BEGIN
 
    -- Only create employee record if the user role is EMPLOYEE
    IF NEW.ROLE = 'EMPLOYEE' THEN
 
        -- Check if employee record doesn't already exist with this email
        IF NOT EXISTS (
    SELECT
         1
    FROM
         EMPLOYEES
    WHERE
         EMAIL = NEW.EMAIL
) THEN
 
            -- Generate a unique employee ID based on user ID
            SET @EMPLOYEE_ID = CONCAT('EMP', LPAD(NEW.ID, 3, '0'));

-- Insert new employee record with basic information from user
INSERT INTO EMPLOYEES (
    EMPLOYEE_ID,
    FULL_NAME,
    EMAIL,
    PHONE,
    DEPARTMENT,
    POSITION,
    SALARY,
    DATE_OF_JOINING,
    ADDRESS,
    STATUS,
    AVATAR_COLOR,
    CREATED_AT,
    UPDATED_AT,
    USER_ID
) VALUES (
    @EMPLOYEE_ID, -- Generated employee ID
    COALESCE(NEW.USERNAME, 'New Employee'), -- Use username as default full name
    NEW.EMAIL, -- Email from user record
    NULL, -- Phone - to be updated later
    'General', -- Default department
    'Employee', -- Default position
    30000.00, -- Default salary
    CURDATE(), -- Current date as joining date
    NULL, -- Address - to be updated later
    'ACTIVE', -- Default status - changed to ACTIVE
    '#3B82F6', -- Default avatar color
    NOW(), -- Created timestamp
    NOW(), -- Updated timestamp
    NEW.ID -- Link to user record
);

END IF;

END IF;

END/
/

-- ==============================================================================
-- TRIGGER 2: Update employee record when user information is changed
-- ==============================================================================

CREATE TRIGGER TR_USER_EMPLOYEE_SYNC_UPDATE AFTER
    UPDATE ON USERS FOR EACH ROW
BEGIN
 
    -- Only sync if email changed and user is EMPLOYEE
    IF OLD.EMAIL != NEW.EMAIL AND NEW.ROLE = 'EMPLOYEE' THEN
 
        -- Update employee record with new email
        UPDATE EMPLOYEES
        SET
            EMAIL = NEW.EMAIL,
            UPDATED_AT = NOW(
            )
        WHERE
            USER_ID = NEW.ID;
    END IF;
 

    -- If role changed from EMPLOYEE to something else, deactivate employee
    IF OLD.ROLE = 'EMPLOYEE' AND NEW.ROLE != 'EMPLOYEE' THEN
        UPDATE EMPLOYEES
        SET
            STATUS = 'INACTIVE',
            UPDATED_AT = NOW(
            )
        WHERE
            USER_ID = NEW.ID;
    END IF;
 

    -- If role changed to EMPLOYEE from something else, create employee record if doesn't exist
    IF OLD.ROLE != 'EMPLOYEE' AND NEW.ROLE = 'EMPLOYEE' THEN
 
        -- First try to reactivate existing employee
        IF EXISTS (
            SELECT
                1
            FROM
                EMPLOYEES
            WHERE
                USER_ID = NEW.ID
        ) THEN
            UPDATE EMPLOYEES
            SET
                STATUS = 'ACTIVE',
                UPDATED_AT = NOW(
                )
            WHERE
                USER_ID = NEW.ID;
        ELSE
 
            -- Create new employee record
            SET @EMPLOYEE_ID = CONCAT('EMP', LPAD(NEW.ID, 3, '0'));
            INSERT INTO EMPLOYEES (
                EMPLOYEE_ID,
                FULL_NAME,
                EMAIL,
                DEPARTMENT,
                POSITION,
                SALARY,
                DATE_OF_JOINING,
                STATUS,
                AVATAR_COLOR,
                CREATED_AT,
                UPDATED_AT,
                USER_ID
            ) VALUES (
                @EMPLOYEE_ID,
                COALESCE(NEW.USERNAME, 'New Employee'),
                NEW.EMAIL,
                'General',
                'Employee',
                30000.00,
                CURDATE(),
                'ACTIVE',
                '#3B82F6',
                NOW(),
                NOW(),
                NEW.ID
            );
        END IF;
    END IF;
 

    -- If user account is disabled, deactivate employee
    IF OLD.ENABLED = TRUE AND NEW.ENABLED = FALSE THEN
        UPDATE EMPLOYEES
        SET
            STATUS = 'INACTIVE',
            UPDATED_AT = NOW(
            )
        WHERE
            USER_ID = NEW.ID;
    END IF;
 

    -- If user account is enabled, reactivate employee
    IF OLD.ENABLED = FALSE AND NEW.ENABLED = TRUE AND NEW.ROLE = 'EMPLOYEE' THEN
        UPDATE EMPLOYEES
        SET
            STATUS = 'ACTIVE',
            UPDATED_AT = NOW(
            )
        WHERE
            USER_ID = NEW.ID;
    END IF;
END//
 -- ==============================================================================
-- TRIGGER 3: Handle user deletion
-- ==============================================================================
CREATE TRIGGER TR_USER_EMPLOYEE_SYNC_DELETE BEFORE DELETE ON USERS FOR EACH ROW BEGIN
 -- When a user is deleted, mark associated employee as terminated
IF OLD.ROLE = 'EMPLOYEE' THEN UPDATE EMPLOYEES SET STATUS = 'TERMINATED', UPDATED_AT = NOW() WHERE USER_ID = OLD.ID;
END IF;
END//
 -- ==============================================================================
-- TRIGGER 4: Update user status when employee status changes
-- ==============================================================================
CREATE TRIGGER TR_EMPLOYEE_USER_SYNC_UPDATE AFTER UPDATE ON EMPLOYEES FOR EACH ROW BEGIN
 -- Only sync if status changed and user_id exists
IF OLD.STATUS != NEW.STATUS AND NEW.USER_ID IS NOT NULL THEN
 -- If employee is terminated, disable user account
IF NEW.STATUS = 'TERMINATED' THEN UPDATE USERS SET ENABLED = FALSE WHERE ID = NEW.USER_ID;
 
-- If employee is reactivated, enable user account
ELSEIF OLD.STATUS = 'TERMINATED' AND NEW.STATUS IN ('ACTIVE', 'INACTIVE') THEN
    UPDATE USERS
    SET
        ENABLED = TRUE
    WHERE
        ID = NEW.USER_ID;
END IF;
END IF;
 
-- Sync email changes back to user
IF OLD.EMAIL != NEW.EMAIL AND NEW.USER_ID IS NOT NULL THEN
    UPDATE USERS
    SET
        EMAIL = NEW.EMAIL
    WHERE
        ID = NEW.USER_ID;
END IF;
END//
 -- ==============================================================================
-- TRIGGER 5: Handle employee deletion
-- ==============================================================================
CREATE TRIGGER TR_EMPLOYEE_USER_SYNC_DELETE BEFORE DELETE ON EMPLOYEES FOR EACH ROW BEGIN
 -- When an employee is deleted, we don't delete the user but change role to HR
-- This preserves the user account for audit purposes
IF OLD.USER_ID IS NOT NULL THEN UPDATE USERS SET ROLE = 'HR' WHERE ID = OLD.USER_ID;
END IF;
END// DELIMITER;
 
-- ==============================================================================
-- Create indices for better performance
-- ==============================================================================
-- Index on user_id in employees table for faster joins
CREATE INDEX IF NOT EXISTS IDX_EMPLOYEES_USER_ID ON EMPLOYEES(USER_ID);
 
-- Index on email in both tables for faster lookups
CREATE INDEX IF NOT EXISTS IDX_USERS_EMAIL ON USERS(EMAIL);
CREATE INDEX IF NOT EXISTS IDX_EMPLOYEES_EMAIL ON EMPLOYEES(EMAIL);
 
-- Index on role for faster role-based queries
CREATE INDEX IF NOT EXISTS IDX_USERS_ROLE ON USERS(ROLE);
 
-- Index on status for faster status-based queries
CREATE INDEX IF NOT EXISTS IDX_EMPLOYEES_STATUS ON EMPLOYEES(STATUS);
 
-- ==============================================================================
-- Verification queries
-- ==============================================================================
-- Check if triggers were created successfully
SELECT
    TRIGGER_NAME,
    EVENT_MANIPULATION,
    EVENT_OBJECT_TABLE,
    ACTION_TIMING,
    TRIGGER_SCHEMA
FROM
    INFORMATION_SCHEMA.TRIGGERS
WHERE
    TRIGGER_SCHEMA = DATABASE()
ORDER BY
    EVENT_OBJECT_TABLE,
    ACTION_TIMING,
    EVENT_MANIPULATION;
 
-- Show trigger count
SELECT
    COUNT(*) AS TRIGGER_COUNT
FROM
    INFORMATION_SCHEMA.TRIGGERS
WHERE
    TRIGGER_SCHEMA = DATABASE();
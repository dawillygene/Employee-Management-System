-- Database Triggers for Employee Management System
-- Auto-executes during Spring Boot application startup

-- Drop existing triggers first
DROP TRIGGER IF EXISTS tr_user_employee_sync_insert;
DROP TRIGGER IF EXISTS tr_employee_user_sync_insert;
DROP TRIGGER IF EXISTS tr_user_employee_sync_update;
DROP TRIGGER IF EXISTS tr_employee_user_sync_update;
DROP TRIGGER IF EXISTS tr_user_employee_sync_delete;
DROP FUNCTION IF EXISTS generate_employee_id;

DELIMITER $$

-- Helper Function: Generate unique employee ID
CREATE FUNCTION generate_employee_id() 
RETURNS VARCHAR(20)
READS SQL DATA
DETERMINISTIC
BEGIN
    DECLARE new_id VARCHAR(20);
    DECLARE counter INT DEFAULT 1;
    
    -- Get the next available employee ID
    SELECT COALESCE(MAX(CAST(SUBSTRING(employee_id, 4) AS UNSIGNED)), 0) + 1 
    INTO counter 
    FROM employees 
    WHERE employee_id REGEXP '^EMP[0-9]+$';
    
    SET new_id = CONCAT('EMP', LPAD(counter, 3, '0'));
    
    -- Ensure uniqueness
    WHILE EXISTS (SELECT 1 FROM employees WHERE employee_id = new_id) DO
        SET counter = counter + 1;
        SET new_id = CONCAT('EMP', LPAD(counter, 3, '0'));
    END WHILE;
    
    RETURN new_id;
END$$

-- TRIGGER 1: Auto-create employee record when new user with EMPLOYEE role is created
CREATE TRIGGER tr_user_employee_sync_insert
AFTER INSERT ON users
FOR EACH ROW
BEGIN
    -- Only create employee record if the user role is EMPLOYEE
    IF NEW.role = 'EMPLOYEE' THEN
        -- Check if employee record doesn't already exist with this email
        IF NOT EXISTS (SELECT 1 FROM employees WHERE email = NEW.email) THEN
            -- Generate a unique employee ID
            SET @employee_id = generate_employee_id();
            
            -- Insert new employee record with basic information from user
            INSERT INTO employees (
                employee_id,
                full_name,
                email,
                phone,
                department,
                position,
                salary,
                date_of_joining,
                address,
                status,
                avatar_color,
                created_at,
                updated_at
            ) VALUES (
                @employee_id,                           -- Generated employee ID
                COALESCE(NEW.username, 'New Employee'), -- Use username as default full name
                NEW.email,                              -- Email from user record
                NULL,                                   -- Phone - to be updated later
                'General',                              -- Default department
                'Employee',                             -- Default position
                30000.00,                              -- Default salary
                CURDATE(),                             -- Current date as joining date
                NULL,                                  -- Address - to be updated later
                'ACTIVE',                              -- Default status
                '#3B82F6',                             -- Default avatar color
                NOW(),                                 -- Created timestamp
                NOW()                                  -- Updated timestamp
            );
        END IF;
    END IF;
END$$

-- TRIGGER 2: Auto-create user record when employee is created by ADMIN/HR
CREATE TRIGGER tr_employee_user_sync_insert
AFTER INSERT ON employees
FOR EACH ROW
BEGIN
    -- Check if user record doesn't already exist with this email
    IF NOT EXISTS (SELECT 1 FROM users WHERE email = NEW.email) THEN
        -- Create username from email (part before @)
        SET @username = SUBSTRING_INDEX(NEW.email, '@', 1);
        
        -- Ensure username is unique by adding numbers if needed
        SET @counter = 0;
        SET @base_username = @username;
        
        WHILE EXISTS (SELECT 1 FROM users WHERE username = @username) DO
            SET @counter = @counter + 1;
            SET @username = CONCAT(@base_username, @counter);
        END WHILE;
        
        -- Insert new user record with hashed password 'password123'
        -- BCrypt hash of 'password123' with strength 10
        INSERT INTO users (
            username,
            password,
            email,
            role,
            enabled
        ) VALUES (
            @username,                                                          -- Generated username
            '$2a$10$N9qo8uLOickgx2ZMRZoMyeIjZAgcfl7p92ldGxad68LJZdL17lhWy',    -- BCrypt hash of 'password123'
            NEW.email,                                                          -- Email from employee record
            'EMPLOYEE',                                                         -- Default role
            TRUE                                                               -- Account enabled
        );
    END IF;
END$$

-- TRIGGER 3: Update employee record when user email is changed
CREATE TRIGGER tr_user_employee_sync_update
AFTER UPDATE ON users
FOR EACH ROW
BEGIN
    -- Only sync if email changed and user is EMPLOYEE
    IF OLD.email != NEW.email AND NEW.role = 'EMPLOYEE' THEN
        -- Update employee record with new email
        UPDATE employees 
        SET email = NEW.email, 
            updated_at = NOW()
        WHERE email = OLD.email;
    END IF;
    
    -- If role changed from EMPLOYEE to something else, deactivate employee
    IF OLD.role = 'EMPLOYEE' AND NEW.role != 'EMPLOYEE' THEN
        UPDATE employees 
        SET status = 'INACTIVE', 
            updated_at = NOW()
        WHERE email = NEW.email;
    END IF;
    
    -- If role changed to EMPLOYEE from something else, reactivate employee
    IF OLD.role != 'EMPLOYEE' AND NEW.role = 'EMPLOYEE' THEN
        UPDATE employees 
        SET status = 'ACTIVE', 
            updated_at = NOW()
        WHERE email = NEW.email;
    END IF;
END$$

-- TRIGGER 4: Update user record when employee email is changed
CREATE TRIGGER tr_employee_user_sync_update
AFTER UPDATE ON employees
FOR EACH ROW
BEGIN
    -- Only sync if email changed
    IF OLD.email != NEW.email THEN
        -- Update user record with new email
        UPDATE users 
        SET email = NEW.email
        WHERE email = OLD.email AND role = 'EMPLOYEE';
    END IF;
    
    -- If employee is terminated, disable user account
    IF OLD.status != 'TERMINATED' AND NEW.status = 'TERMINATED' THEN
        UPDATE users 
        SET enabled = FALSE
        WHERE email = NEW.email AND role = 'EMPLOYEE';
    END IF;
    
    -- If employee is reactivated, enable user account
    IF OLD.status = 'TERMINATED' AND NEW.status = 'ACTIVE' THEN
        UPDATE users 
        SET enabled = TRUE
        WHERE email = NEW.email AND role = 'EMPLOYEE';
    END IF;
END$$

-- TRIGGER 5: Handle user deletion (soft delete employee)
CREATE TRIGGER tr_user_employee_sync_delete
AFTER DELETE ON users
FOR EACH ROW
BEGIN
    -- If deleted user was EMPLOYEE, mark employee as terminated
    IF OLD.role = 'EMPLOYEE' THEN
        UPDATE employees 
        SET status = 'TERMINATED', 
            updated_at = NOW()
        WHERE email = OLD.email;
    END IF;
END$$

DELIMITER ;

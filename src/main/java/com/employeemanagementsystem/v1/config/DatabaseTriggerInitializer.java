package com.employeemanagementsystem.v1.config;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.CommandLineRunner;
import org.springframework.core.annotation.Order;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
@Slf4j
@Order(1) // Run before DataLoader
public class DatabaseTriggerInitializer implements CommandLineRunner {

    private final JdbcTemplate jdbcTemplate;

    @Override
    public void run(String... args) throws Exception {
        try {
            log.info("Initializing database triggers...");
            
            // Check if triggers already exist
            if (triggersExist()) {
                log.info("Database triggers already exist, skipping initialization");
                return;
            }
            
            // Read and execute the trigger SQL script
            createDatabaseTriggers();
            
            log.info("Database triggers initialized successfully");
            
        } catch (Exception e) {
            log.error("Failed to initialize database triggers: {}", e.getMessage(), e);
            // Don't throw exception to prevent application startup failure
        }
    }

    private boolean triggersExist() {
        try {
            String checkTriggerQuery = """
                SELECT COUNT(*) FROM information_schema.triggers 
                WHERE trigger_schema = DATABASE() 
                AND trigger_name IN (
                    'after_user_insert_to_employee',
                    'after_employee_insert_to_user'
                )
                """;
            
            Integer triggerCount = jdbcTemplate.queryForObject(checkTriggerQuery, Integer.class);
            return triggerCount != null && triggerCount >= 2;
            
        } catch (Exception e) {
            log.warn("Could not check existing triggers: {}", e.getMessage());
            return false;
        }
    }

    private void createDatabaseTriggers() {
        try {
            log.info("Creating database triggers and functions...");
            
            // Drop existing triggers and function first
            dropExistingTriggers();
            
            // Create the custom hash function
            String createHashFunction = """
                CREATE FUNCTION custom_hash(password VARCHAR(255))
                RETURNS VARCHAR(255)
                DETERMINISTIC
                BEGIN
                    RETURN SHA2(password, 256);
                END
                """;
            
            jdbcTemplate.execute(createHashFunction);
            log.debug("Created custom_hash function successfully");
            
            // Create trigger: after_user_insert_to_employee
            String userToEmployeeTrigger = """
                CREATE TRIGGER after_user_insert_to_employee
                AFTER INSERT ON users
                FOR EACH ROW
                BEGIN
                    IF NEW.role = 'EMPLOYEE' THEN
                        INSERT INTO employees (
                            user_id,
                            employee_id,
                            full_name,
                            email,
                            phone,
                            position,
                            department,
                            salary,
                            date_of_joining,
                            status
                        )
                        VALUES (
                            NEW.id,
                            CONCAT('EMP-', LPAD(NEW.id, 4, '0')),
                            NEW.username,
                            NEW.email,
                            NULL,
                            'Default Position',
                            'Default Department',
                            50000.00,
                            CURDATE(),
                            'ACTIVE'
                        );
                    END IF;
                END
                """;
            
            jdbcTemplate.execute(userToEmployeeTrigger);
            log.debug("Created after_user_insert_to_employee trigger successfully");
            
            // Create trigger: after_employee_insert_to_user
            String employeeToUserTrigger = """
                CREATE TRIGGER after_employee_insert_to_user
                AFTER INSERT ON employees
                FOR EACH ROW
                BEGIN
                    DECLARE user_exists INT;
                    
                    SELECT COUNT(*) INTO user_exists FROM users WHERE email = NEW.email;
                    
                    IF user_exists = 0 THEN
                        INSERT INTO users (username, password, email, role, enabled)
                        VALUES (
                            NEW.email,
                            custom_hash('password123'),
                            NEW.email,
                            'EMPLOYEE',
                            1
                        );
                        
                        UPDATE employees SET user_id = LAST_INSERT_ID() WHERE id = NEW.id;
                    END IF;
                END
                """;
            
            jdbcTemplate.execute(employeeToUserTrigger);
            log.debug("Created after_employee_insert_to_user trigger successfully");
            
            log.info("All database triggers created successfully");
            
        } catch (Exception e) {
            log.error("Failed to create database triggers: {}", e.getMessage());
            throw new RuntimeException("Database trigger initialization failed", e);
        }
    }

    public void dropExistingTriggers() {
        try {
            log.info("Dropping existing triggers...");
            
            String[] triggerNames = {
                "after_user_insert_to_employee",
                "after_employee_insert_to_user"
            };
            
            for (String triggerName : triggerNames) {
                try {
                    jdbcTemplate.execute("DROP TRIGGER IF EXISTS " + triggerName);
                    log.debug("Dropped trigger: {}", triggerName);
                } catch (Exception e) {
                    log.warn("Could not drop trigger {}: {}", triggerName, e.getMessage());
                }
            }
            
            // Drop helper function
            try {
                jdbcTemplate.execute("DROP FUNCTION IF EXISTS custom_hash");
                log.debug("Dropped function: custom_hash");
            } catch (Exception e) {
                log.warn("Could not drop function custom_hash: {}", e.getMessage());
            }
            
        } catch (Exception e) {
            log.error("Failed to drop existing triggers: {}", e.getMessage());
        }
    }
}

package com.employeemanagementsystem.v1.service;

import com.employeemanagementsystem.v1.entity.Employee;
import com.employeemanagementsystem.v1.entity.User;
import com.employeemanagementsystem.v1.repository.EmployeeRepository;
import com.employeemanagementsystem.v1.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Optional;

@Service
@RequiredArgsConstructor
@Slf4j
@Transactional
public class TriggerTestService {

    private final UserRepository userRepository;
    private final EmployeeRepository employeeRepository;

    /**
     * Test trigger by creating a user with EMPLOYEE role
     * Should automatically create an employee record
     */
    public void testUserToEmployeeTrigger(String username, String email) {
        try {
            log.info("Testing user-to-employee trigger with email: {}", email);
            
            // Check if user already exists
            if (userRepository.existsByEmail(email)) {
                log.info("User with email {} already exists, skipping test", email);
                return;
            }
            
            // Create new user with EMPLOYEE role
            User newUser = new User();
            newUser.setUsername(username);
            newUser.setPassword("$2a$10$N9qo8uLOickgx2ZMRZoMyeIjZAgcfl7p92ldGxad68LJZdL17lhWy"); // password123
            newUser.setEmail(email);
            newUser.setRole(User.Role.EMPLOYEE);
            newUser.setEnabled(true);
            
            User savedUser = userRepository.save(newUser);
            log.info("Created user: {}", savedUser.getUsername());
            
            // Check if employee was automatically created by trigger
            Optional<Employee> employeeOpt = employeeRepository.findByEmail(email);
            if (employeeOpt.isPresent()) {
                Employee employee = employeeOpt.get();
                log.info("✅ Trigger SUCCESS: Employee automatically created with ID: {}", employee.getEmployeeId());
            } else {
                log.error("❌ Trigger FAILED: No employee record created for user {}", email);
            }
            
        } catch (Exception e) {
            log.error("Error testing user-to-employee trigger: {}", e.getMessage(), e);
        }
    }

    /**
     * Test trigger by creating an employee record
     * Should automatically create a user account with password123
     */
    public void testEmployeeToUserTrigger(String employeeId, String fullName, String email) {
        try {
            log.info("Testing employee-to-user trigger with email: {}", email);
            
            // Check if employee already exists
            if (employeeRepository.existsByEmail(email)) {
                log.info("Employee with email {} already exists, skipping test", email);
                return;
            }
            
            // Create new employee record
            Employee newEmployee = new Employee();
            newEmployee.setEmployeeId(employeeId);
            newEmployee.setFullName(fullName);
            newEmployee.setEmail(email);
            newEmployee.setPhone("1234567890");
            newEmployee.setDepartment("IT");
            newEmployee.setPosition("Developer");
            newEmployee.setSalary(new BigDecimal("50000"));
            newEmployee.setDateOfJoining(LocalDate.now());
            newEmployee.setAddress("Test Address");
            newEmployee.setStatus(Employee.EmployeeStatus.ACTIVE);
            
            Employee savedEmployee = employeeRepository.save(newEmployee);
            log.info("Created employee: {}", savedEmployee.getEmployeeId());
            
            // Check if user was automatically created by trigger
            Optional<User> userOpt = userRepository.findByEmail(email);
            if (userOpt.isPresent()) {
                User user = userOpt.get();
                log.info("✅ Trigger SUCCESS: User automatically created with username: {}", user.getUsername());
                log.info("✅ User role: {}, enabled: {}", user.getRole(), user.isEnabled());
            } else {
                log.error("❌ Trigger FAILED: No user record created for employee {}", email);
            }
            
        } catch (Exception e) {
            log.error("Error testing employee-to-user trigger: {}", e.getMessage(), e);
        }
    }

    /**
     * Test both triggers with sample data
     */
    public void runTriggerTests() {
        log.info("🧪 Running database trigger tests...");
        
        // Test 1: User to Employee trigger
        testUserToEmployeeTrigger("test.employee1", "test.employee1@company.com");
        
        // Test 2: Employee to User trigger  
        testEmployeeToUserTrigger("EMP999", "Test Employee 2", "test.employee2@company.com");
        
        log.info("🧪 Trigger tests completed");
    }

    /**
     * Check trigger status
     */
    public void checkTriggerStatus() {
        try {
            log.info("📊 Checking database trigger status...");
            
            // Count users and employees
            long userCount = userRepository.count();
            long employeeCount = employeeRepository.count();
            
            log.info("Current database state:");
            log.info("- Total users: {}", userCount);
            log.info("- Total employees: {}", employeeCount);
            
            // Check for users with EMPLOYEE role
            long employeeUsers = userRepository.findAll().stream()
                    .filter(user -> user.getRole() == User.Role.EMPLOYEE)
                    .count();
            
            log.info("- Employee users: {}", employeeUsers);
            
        } catch (Exception e) {
            log.error("Error checking trigger status: {}", e.getMessage());
        }
    }
}

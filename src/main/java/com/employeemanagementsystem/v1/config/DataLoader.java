package com.employeemanagementsystem.v1.config;

import com.employeemanagementsystem.v1.entity.User;
import com.employeemanagementsystem.v1.entity.Employee;
import com.employeemanagementsystem.v1.service.UserService;
import com.employeemanagementsystem.v1.service.EmployeeService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.time.LocalDate;

@Component
@RequiredArgsConstructor
@Slf4j
public class DataLoader implements CommandLineRunner {
    
    private final UserService userService;
    private final EmployeeService employeeService;
    
    @Override
    public void run(String... args) throws Exception {
        loadInitialData();
    }
    
    private void loadInitialData() {
        try {
            // Create default admin user
            createDefaultUsers();
            
            // Create sample employees
            createSampleEmployees();
            
            log.info("Initial data loaded successfully");
        } catch (Exception e) {
            log.info("Initial data already exists or error occurred: {}", e.getMessage());
        }
    }
    
    private void createDefaultUsers() {
        try {
            // Create Admin user
            User admin = new User();
            admin.setUsername("admin");
            admin.setEmail("admin@company.com");
            admin.setPassword("admin123");
            admin.setRole(User.Role.ADMIN);
            userService.createUser(admin);
            log.info("Default admin user created: admin/admin123");
            
            // Create HR user
            User hr = new User();
            hr.setUsername("hr");
            hr.setEmail("hr@company.com");
            hr.setPassword("hr123");
            hr.setRole(User.Role.HR);
            userService.createUser(hr);
            log.info("Default HR user created: hr/hr123");
            
        } catch (RuntimeException e) {
            log.info("Default users already exist");
        }
    }
    
    private void createSampleEmployees() {
        try {
            // Sample Employee 1
            Employee emp1 = new Employee();
            emp1.setEmployeeId("EMP001");
            emp1.setFullName("John Doe");
            emp1.setEmail("john.doe@company.com");
            emp1.setPhone("1234567890");
            emp1.setDepartment("IT");
            emp1.setPosition("Senior Developer");
            emp1.setSalary(new BigDecimal("75000"));
            emp1.setDateOfJoining(LocalDate.of(2023, 1, 15));
            emp1.setAddress("123 Main St, City, State 12345");
            employeeService.createEmployee(emp1);
            
            // Sample Employee 2
            Employee emp2 = new Employee();
            emp2.setEmployeeId("EMP002");
            emp2.setFullName("Sarah Johnson");
            emp2.setEmail("sarah.johnson@company.com");
            emp2.setPhone("9876543210");
            emp2.setDepartment("Marketing");
            emp2.setPosition("Marketing Manager");
            emp2.setSalary(new BigDecimal("68000"));
            emp2.setDateOfJoining(LocalDate.of(2023, 3, 10));
            emp2.setAddress("456 Oak Ave, City, State 12345");
            employeeService.createEmployee(emp2);
            
            // Sample Employee 3
            Employee emp3 = new Employee();
            emp3.setEmployeeId("EMP003");
            emp3.setFullName("Michael Brown");
            emp3.setEmail("michael.brown@company.com");
            emp3.setPhone("5555555555");
            emp3.setDepartment("Finance");
            emp3.setPosition("Financial Analyst");
            emp3.setSalary(new BigDecimal("55000"));
            emp3.setDateOfJoining(LocalDate.of(2023, 5, 20));
            emp3.setAddress("789 Pine St, City, State 12345");
            employeeService.createEmployee(emp3);
            
            // Sample Employee 4
            Employee emp4 = new Employee();
            emp4.setEmployeeId("EMP004");
            emp4.setFullName("Emily Davis");
            emp4.setEmail("emily.davis@company.com");
            emp4.setPhone("7777777777");
            emp4.setDepartment("HR");
            emp4.setPosition("HR Specialist");
            emp4.setSalary(new BigDecimal("52000"));
            emp4.setDateOfJoining(LocalDate.of(2023, 7, 1));
            emp4.setAddress("321 Elm St, City, State 12345");
            employeeService.createEmployee(emp4);
            
            // Sample Employee 5
            Employee emp5 = new Employee();
            emp5.setEmployeeId("EMP005");
            emp5.setFullName("Robert Wilson");
            emp5.setEmail("robert.wilson@company.com");
            emp5.setPhone("8888888888");
            emp5.setDepartment("Operations");
            emp5.setPosition("Operations Manager");
            emp5.setSalary(new BigDecimal("72000"));
            emp5.setDateOfJoining(LocalDate.of(2023, 2, 28));
            emp5.setAddress("654 Maple Ave, City, State 12345");
            employeeService.createEmployee(emp5);
            
            log.info("Sample employees created successfully");
            
        } catch (RuntimeException e) {
            log.info("Sample employees already exist or error occurred: {}", e.getMessage());
        }
    }
}
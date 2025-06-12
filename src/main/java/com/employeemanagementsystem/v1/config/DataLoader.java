package com.employeemanagementsystem.v1.config;

import com.employeemanagementsystem.v1.entity.User;
import com.employeemanagementsystem.v1.entity.Employee;
import com.employeemanagementsystem.v1.entity.LeaveRequest;
import com.employeemanagementsystem.v1.service.UserService;
import com.employeemanagementsystem.v1.service.EmployeeService;
import com.employeemanagementsystem.v1.service.LeaveService;
import com.employeemanagementsystem.v1.repository.LeaveRequestRepository;
import com.employeemanagementsystem.v1.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.CommandLineRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

@Component
@RequiredArgsConstructor
@Slf4j
public class DataLoader implements CommandLineRunner {
    
    private final UserService userService;
    private final EmployeeService employeeService;
    private final LeaveService leaveService;
    private final LeaveRequestRepository leaveRequestRepository; // Direct repository access for sample data
    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    
    @Override
    public void run(String... args) throws Exception {
        loadInitialData();
    }
    
    private void loadInitialData() {
        try {
            // Create default admin user
            createDefaultUsers();
            
            // Create sample users with different roles
            createSampleEmployeeUsers();
            
            // Create sample employees
            createSampleEmployees();
            
            // Create sample leave requests
            createSampleLeaveRequests();
            
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
    
    private void createSampleLeaveRequests() {
        try {
            List<Employee> employees = employeeService.getAllActiveEmployees();
            if (employees.isEmpty()) {
                log.info("No employees found, skipping leave request creation");
                return;
            }

            // Clear existing sample data to avoid duplicates
            log.info("Creating sample leave requests for {} employees", employees.size());

            // Sample Leave Request 1 - Approved Vacation (John Doe)
            if (employees.size() > 0) {
                LeaveRequest leave1 = new LeaveRequest();
                leave1.setEmployee(employees.get(0)); // John Doe
                leave1.setLeaveType(LeaveRequest.LeaveType.VACATION);
                leave1.setStartDate(LocalDate.now().plusDays(10));
                leave1.setEndDate(LocalDate.now().plusDays(14));
                leave1.setReason("Family vacation to visit relatives during the holiday season. Planning to spend quality time with extended family.");
                leave1.setStatus(LeaveRequest.LeaveStatus.APPROVED);
                leave1.setApprovedBy("hr");
                leave1.setApprovedAt(LocalDateTime.now().minusDays(1));
                leave1.setManagerComments("Approved. Enjoy your vacation!");
                leaveRequestRepository.save(leave1);
                log.info("Created leave request for {}", employees.get(0).getFullName());
            }

            // Sample Leave Request 2 - Pending Sick Leave (Sarah Johnson)
            if (employees.size() > 1) {
                LeaveRequest leave2 = new LeaveRequest();
                leave2.setEmployee(employees.get(1)); // Sarah Johnson
                leave2.setLeaveType(LeaveRequest.LeaveType.SICK_LEAVE);
                leave2.setStartDate(LocalDate.now().plusDays(2));
                leave2.setEndDate(LocalDate.now().plusDays(4));
                leave2.setReason("Medical appointment and recovery from minor procedure. Doctor has recommended 3 days rest.");
                leave2.setStatus(LeaveRequest.LeaveStatus.PENDING);
                leaveRequestRepository.save(leave2);
                log.info("Created leave request for {}", employees.get(1).getFullName());
            }

            // Sample Leave Request 3 - Approved Personal Leave (Michael Brown)
            if (employees.size() > 2) {
                LeaveRequest leave3 = new LeaveRequest();
                leave3.setEmployee(employees.get(2)); // Michael Brown
                leave3.setLeaveType(LeaveRequest.LeaveType.PERSONAL);
                leave3.setStartDate(LocalDate.now().plusDays(7));
                leave3.setEndDate(LocalDate.now().plusDays(7));
                leave3.setReason("Personal matter requiring immediate attention. Need to handle important family business.");
                leave3.setStatus(LeaveRequest.LeaveStatus.APPROVED);
                leave3.setApprovedBy("admin");
                leave3.setApprovedAt(LocalDateTime.now().minusHours(6));
                leave3.setManagerComments("Approved for personal day.");
                leaveRequestRepository.save(leave3);
                log.info("Created leave request for {}", employees.get(2).getFullName());
            }

            // Sample Leave Request 4 - Rejected Vacation (Emily Davis)
            if (employees.size() > 3) {
                LeaveRequest leave4 = new LeaveRequest();
                leave4.setEmployee(employees.get(3)); // Emily Davis
                leave4.setLeaveType(LeaveRequest.LeaveType.VACATION);
                leave4.setStartDate(LocalDate.now().plusDays(1));
                leave4.setEndDate(LocalDate.now().plusDays(3));
                leave4.setReason("Short notice vacation request for weekend getaway.");
                leave4.setStatus(LeaveRequest.LeaveStatus.REJECTED);
                leave4.setApprovedBy("hr");
                leave4.setApprovedAt(LocalDateTime.now().minusHours(2));
                leave4.setManagerComments("Request rejected due to short notice and peak work period. Please submit requests at least 2 weeks in advance.");
                leaveRequestRepository.save(leave4);
                log.info("Created leave request for {}", employees.get(3).getFullName());
            }

            // Sample Leave Request 5 - Pending Emergency Leave (Robert Wilson if available)
            if (employees.size() > 4) {
                LeaveRequest leave5 = new LeaveRequest();
                leave5.setEmployee(employees.get(4)); // Robert Wilson
                leave5.setLeaveType(LeaveRequest.LeaveType.EMERGENCY);
                leave5.setStartDate(LocalDate.now().plusDays(1));
                leave5.setEndDate(LocalDate.now().plusDays(2));
                leave5.setReason("Family emergency requiring immediate attention. Unexpected situation needs urgent response.");
                leave5.setStatus(LeaveRequest.LeaveStatus.PENDING);
                leaveRequestRepository.save(leave5);
                log.info("Created leave request for {}", employees.get(4).getFullName());
            }

            log.info("Sample leave requests created successfully - Total: {}", Math.min(5, employees.size()));

        } catch (Exception e) {
            log.error("Error creating sample leave requests: {}", e.getMessage(), e);
        }
    }

    private void createSampleEmployeeUsers() {
        try {
            // Create EMPLOYEE users for testing
            String[] employeeUsernames = {"john.doe", "sarah.johnson", "michael.brown", "emily.davis", "robert.wilson"};
            String[] employeeEmails = {"john.doe@company.com", "sarah.johnson@company.com", "michael.brown@company.com", 
                                     "emily.davis@company.com", "robert.wilson@company.com"};
            
            for (int i = 0; i < employeeUsernames.length; i++) {
                try {
                    User employee = new User();
                    employee.setUsername(employeeUsernames[i]);
                    employee.setPassword("employee123");
                    employee.setEmail(employeeEmails[i]);
                    employee.setRole(User.Role.EMPLOYEE);
                    userService.createUser(employee);
                    log.info("Created employee user: {}/employee123", employeeUsernames[i]);
                } catch (RuntimeException e) {
                    // User already exists, skip
                }
            }

            log.info("Sample employee users processed");
        } catch (Exception e) {
            log.error("Error creating sample employee users: {}", e.getMessage());
        }
    }
}
package com.employeemanagementsystem.v1.controller;

import com.employeemanagementsystem.v1.config.DatabaseTriggerInitializer;
import com.employeemanagementsystem.v1.service.TriggerTestService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.Map;

@RestController
@RequestMapping("/api/admin/triggers")
@RequiredArgsConstructor
@Slf4j
@PreAuthorize("hasRole('ADMIN')")
public class TriggerManagementController {

    private final DatabaseTriggerInitializer triggerInitializer;
    private final TriggerTestService triggerTestService;

    @PostMapping("/reinitialize")
    public ResponseEntity<Map<String, String>> reinitializeTriggers() {
        Map<String, String> response = new HashMap<>();
        
        try {
            log.info("Reinitializing database triggers via API request");
            
            // Drop existing triggers
            triggerInitializer.dropExistingTriggers();
            
            // Recreate triggers
            triggerInitializer.run();
            
            response.put("status", "success");
            response.put("message", "Database triggers reinitialized successfully");
            
            log.info("Database triggers reinitialized successfully via API");
            return ResponseEntity.ok(response);
            
        } catch (Exception e) {
            log.error("Failed to reinitialize triggers via API: {}", e.getMessage(), e);
            response.put("status", "error");
            response.put("message", "Failed to reinitialize triggers: " + e.getMessage());
            return ResponseEntity.status(500).body(response);
        }
    }

    @PostMapping("/test")
    public ResponseEntity<Map<String, String>> testTriggers() {
        Map<String, String> response = new HashMap<>();
        
        try {
            log.info("Testing database triggers via API request");
            
            // Run trigger tests
            triggerTestService.runTriggerTests();
            triggerTestService.checkTriggerStatus();
            
            response.put("status", "success");
            response.put("message", "Trigger tests completed successfully");
            
            return ResponseEntity.ok(response);
            
        } catch (Exception e) {
            log.error("Failed to test triggers via API: {}", e.getMessage(), e);
            response.put("status", "error");
            response.put("message", "Failed to test triggers: " + e.getMessage());
            return ResponseEntity.status(500).body(response);
        }
    }

    @PostMapping("/test/user-to-employee")
    public ResponseEntity<Map<String, String>> testUserToEmployeeTrigger(
            @RequestParam String username,
            @RequestParam String email) {
        
        Map<String, String> response = new HashMap<>();
        
        try {
            log.info("Testing user-to-employee trigger for: {}", email);
            
            triggerTestService.testUserToEmployeeTrigger(username, email);
            
            response.put("status", "success");
            response.put("message", "User-to-employee trigger test completed for " + email);
            
            return ResponseEntity.ok(response);
            
        } catch (Exception e) {
            log.error("Failed to test user-to-employee trigger: {}", e.getMessage(), e);
            response.put("status", "error");
            response.put("message", "Failed to test trigger: " + e.getMessage());
            return ResponseEntity.status(500).body(response);
        }
    }

    @PostMapping("/test/employee-to-user")
    public ResponseEntity<Map<String, String>> testEmployeeToUserTrigger(
            @RequestParam String employeeId,
            @RequestParam String fullName,
            @RequestParam String email) {
        
        Map<String, String> response = new HashMap<>();
        
        try {
            log.info("Testing employee-to-user trigger for: {}", email);
            
            triggerTestService.testEmployeeToUserTrigger(employeeId, fullName, email);
            
            response.put("status", "success");
            response.put("message", "Employee-to-user trigger test completed for " + email);
            
            return ResponseEntity.ok(response);
            
        } catch (Exception e) {
            log.error("Failed to test employee-to-user trigger: {}", e.getMessage(), e);
            response.put("status", "error");
            response.put("message", "Failed to test trigger: " + e.getMessage());
            return ResponseEntity.status(500).body(response);
        }
    }

    @GetMapping("/status")
    public ResponseEntity<Map<String, Object>> getTriggerStatus() {
        Map<String, Object> response = new HashMap<>();
        
        try {
            triggerTestService.checkTriggerStatus();
            
            response.put("status", "success");
            response.put("message", "Trigger status checked successfully");
            response.put("timestamp", System.currentTimeMillis());
            
            return ResponseEntity.ok(response);
            
        } catch (Exception e) {
            log.error("Failed to check trigger status: {}", e.getMessage(), e);
            response.put("status", "error");
            response.put("message", "Failed to check trigger status: " + e.getMessage());
            return ResponseEntity.status(500).body(response);
        }
    }
}

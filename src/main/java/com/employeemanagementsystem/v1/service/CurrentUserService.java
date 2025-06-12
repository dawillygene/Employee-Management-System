package com.employeemanagementsystem.v1.service;

import com.employeemanagementsystem.v1.entity.Employee;
import com.employeemanagementsystem.v1.entity.User;
import com.employeemanagementsystem.v1.repository.EmployeeRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;

import java.util.Optional;

@Service
@RequiredArgsConstructor
public class CurrentUserService {
    
    private final EmployeeRepository employeeRepository;
    
    /**
     * Get the currently logged-in user
     */
    public User getCurrentUser() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication != null && authentication.getPrincipal() instanceof User) {
            return (User) authentication.getPrincipal();
        }
        return null;
    }
    
    /**
     * Get the employee record for the currently logged-in user
     * This matches the user's email with the employee's email
     */
    public Optional<Employee> getCurrentEmployee() {
        User currentUser = getCurrentUser();
        if (currentUser != null) {
            return employeeRepository.findByEmail(currentUser.getEmail());
        }
        return Optional.empty();
    }
    
    /**
     * Check if the current user is an employee (has an employee record)
     */
    public boolean isCurrentUserEmployee() {
        return getCurrentEmployee().isPresent();
    }
    
    /**
     * Get the current user's role
     */
    public String getCurrentUserRole() {
        User currentUser = getCurrentUser();
        return currentUser != null ? currentUser.getRole().name() : null;
    }
    
    /**
     * Check if the current user has a specific role
     */
    public boolean hasRole(String role) {
        String currentRole = getCurrentUserRole();
        return currentRole != null && currentRole.equals(role);
    }
}
package com.employeemanagementsystem.v1.service;

import com.employeemanagementsystem.v1.entity.Employee;
import com.employeemanagementsystem.v1.repository.EmployeeRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
@Transactional
public class EmployeeService {
    
    private final EmployeeRepository employeeRepository;
    
    public Employee createEmployee(Employee employee) {
        if (employeeRepository.existsByEmployeeId(employee.getEmployeeId())) {
            throw new RuntimeException("Employee ID already exists");
        }
        if (employeeRepository.existsByEmail(employee.getEmail())) {
            throw new RuntimeException("Email already exists");
        }
        return employeeRepository.save(employee);
    }
    
    public Employee updateEmployee(Employee employee) {
        Employee existingEmployee = findById(employee.getId());
        
        // Check if employeeId or email changed and already exists
        if (!existingEmployee.getEmployeeId().equals(employee.getEmployeeId()) && 
            employeeRepository.existsByEmployeeId(employee.getEmployeeId())) {
            throw new RuntimeException("Employee ID already exists");
        }
        
        if (!existingEmployee.getEmail().equals(employee.getEmail()) && 
            employeeRepository.existsByEmail(employee.getEmail())) {
            throw new RuntimeException("Email already exists");
        }
        
        return employeeRepository.save(employee);
    }
    
    public Employee findById(Long id) {
        return employeeRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Employee not found"));
    }
    
    public Employee findByEmployeeId(String employeeId) {
        return employeeRepository.findByEmployeeId(employeeId)
                .orElseThrow(() -> new RuntimeException("Employee not found"));
    }
    
    public List<Employee> findAll() {
        return employeeRepository.findAll();
    }
    
    public Page<Employee> findBySearchCriteria(String search, String department, String position, Pageable pageable) {
        return employeeRepository.findBySearchCriteria(search, department, position, pageable);
    }
    
    public List<String> getAllDepartments() {
        return employeeRepository.findDistinctDepartments();
    }
    
    public List<String> getAllPositions() {
        return employeeRepository.findDistinctPositions();
    }
    
    public void deleteEmployee(Long id) {
        Employee employee = findById(id);
        employee.setStatus(Employee.EmployeeStatus.TERMINATED);
        employeeRepository.save(employee);
    }
    
    public List<Employee> getAllActiveEmployees() {
        return employeeRepository.findByStatus(Employee.EmployeeStatus.ACTIVE);
    }

    public long getTotalEmployees() {
        return employeeRepository.count();
    }

    public long getActiveEmployees() {
        return employeeRepository.countByStatus(Employee.EmployeeStatus.ACTIVE);
    }
}
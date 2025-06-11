package com.employeemanagementsystem.v1.controller;

import com.employeemanagementsystem.v1.entity.Employee;
import com.employeemanagementsystem.v1.service.EmployeeService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import jakarta.validation.Valid;

@Controller
@RequestMapping("/employees")
@RequiredArgsConstructor
public class EmployeeController {
    
    private final EmployeeService employeeService;
    
    @GetMapping
    public String listEmployees(@RequestParam(defaultValue = "") String search,
                               @RequestParam(defaultValue = "") String department,
                               @RequestParam(defaultValue = "") String position,
                               @RequestParam(defaultValue = "0") int page,
                               @RequestParam(defaultValue = "10") int size,
                               Model model) {
        
        Pageable pageable = PageRequest.of(page, size, Sort.by("fullName"));
        Page<Employee> employees = employeeService.findBySearchCriteria(
            search.isEmpty() ? null : search,
            department.isEmpty() ? null : department,
            position.isEmpty() ? null : position,
            pageable);
        
        model.addAttribute("pageTitle", "Employee Management");
        model.addAttribute("pageDescription", "Manage employee information and records");
        model.addAttribute("activePage", "employees");
        model.addAttribute("employees", employees);
        model.addAttribute("departments", employeeService.getAllDepartments());
        model.addAttribute("positions", employeeService.getAllPositions());
        model.addAttribute("search", search);
        model.addAttribute("selectedDepartment", department);
        model.addAttribute("selectedPosition", position);
        
        return "employees/list";
    }
    
    @GetMapping("/add")
    public String showAddForm(Model model) {
        model.addAttribute("pageTitle", "Add Employee");
        model.addAttribute("pageDescription", "Add a new employee to the system");
        model.addAttribute("activePage", "employees");
        model.addAttribute("employee", new Employee());
        model.addAttribute("departments", employeeService.getAllDepartments());
        model.addAttribute("positions", employeeService.getAllPositions());
        model.addAttribute("isEdit", false);
        return "employees/form";
    }
    
    @PostMapping("/add")
    public String addEmployee(@Valid @ModelAttribute Employee employee,
                             BindingResult result,
                             RedirectAttributes redirectAttributes,
                             Model model) {
        if (result.hasErrors()) {
            model.addAttribute("pageTitle", "Add Employee");
            model.addAttribute("pageDescription", "Add a new employee to the system");
            model.addAttribute("activePage", "employees");
            model.addAttribute("departments", employeeService.getAllDepartments());
            model.addAttribute("positions", employeeService.getAllPositions());
            model.addAttribute("isEdit", false);
            return "employees/form";
        }
        
        try {
            employeeService.createEmployee(employee);
            redirectAttributes.addFlashAttribute("success", "Employee added successfully!");
            return "redirect:/employees";
        } catch (RuntimeException e) {
            model.addAttribute("error", e.getMessage());
            model.addAttribute("pageTitle", "Add Employee");
            model.addAttribute("pageDescription", "Add a new employee to the system");
            model.addAttribute("activePage", "employees");
            model.addAttribute("departments", employeeService.getAllDepartments());
            model.addAttribute("positions", employeeService.getAllPositions());
            model.addAttribute("isEdit", false);
            return "employees/form";
        }
    }
    
    @GetMapping("/{id}")
    public String viewEmployee(@PathVariable Long id, Model model) {
        Employee employee = employeeService.findById(id);
        model.addAttribute("pageTitle", "Employee Details");
        model.addAttribute("pageDescription", "View employee information and details");
        model.addAttribute("activePage", "employees");
        model.addAttribute("employee", employee);
        return "employees/view";
    }
    
    @GetMapping("/{id}/edit")
    public String showEditForm(@PathVariable Long id, Model model) {
        Employee employee = employeeService.findById(id);
        model.addAttribute("pageTitle", "Edit Employee");
        model.addAttribute("pageDescription", "Update employee information");
        model.addAttribute("activePage", "employees");
        model.addAttribute("employee", employee);
        model.addAttribute("departments", employeeService.getAllDepartments());
        model.addAttribute("positions", employeeService.getAllPositions());
        model.addAttribute("isEdit", true);
        return "employees/form";
    }
    
    @PostMapping("/{id}/edit")
    public String updateEmployee(@PathVariable Long id,
                                @Valid @ModelAttribute Employee employee,
                                BindingResult result,
                                RedirectAttributes redirectAttributes,
                                Model model) {
        employee.setId(id);
        
        if (result.hasErrors()) {
            model.addAttribute("pageTitle", "Edit Employee");
            model.addAttribute("pageDescription", "Update employee information");
            model.addAttribute("activePage", "employees");
            model.addAttribute("departments", employeeService.getAllDepartments());
            model.addAttribute("positions", employeeService.getAllPositions());
            model.addAttribute("isEdit", true);
            return "employees/form";
        }
        
        try {
            employeeService.updateEmployee(employee);
            redirectAttributes.addFlashAttribute("success", "Employee updated successfully!");
            return "redirect:/employees";
        } catch (RuntimeException e) {
            model.addAttribute("error", e.getMessage());
            model.addAttribute("pageTitle", "Edit Employee");
            model.addAttribute("pageDescription", "Update employee information");
            model.addAttribute("activePage", "employees");
            model.addAttribute("departments", employeeService.getAllDepartments());
            model.addAttribute("positions", employeeService.getAllPositions());
            model.addAttribute("isEdit", true);
            return "employees/form";
        }
    }
    
    @PostMapping("/{id}/delete")
    @PreAuthorize("hasRole('ADMIN')")
    public String deleteEmployee(@PathVariable Long id, RedirectAttributes redirectAttributes) {
        try {
            employeeService.deleteEmployee(id);
            redirectAttributes.addFlashAttribute("success", "Employee deleted successfully!");
        } catch (RuntimeException e) {
            redirectAttributes.addFlashAttribute("error", e.getMessage());
        }
        return "redirect:/employees";
    }
}
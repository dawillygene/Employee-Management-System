package com.employeemanagementsystem.v1.controller;

import com.employeemanagementsystem.v1.service.EmployeeService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;

@Controller
@RequiredArgsConstructor
public class DashboardController {
    
    private final EmployeeService employeeService;
    
    @GetMapping({"/", "/dashboard"})
    public String dashboard(Model model) {
        model.addAttribute("pageTitle", "Dashboard");
        model.addAttribute("activePage", "dashboard");
        
        // Dashboard statistics
        model.addAttribute("totalEmployees", employeeService.getTotalEmployees());
        model.addAttribute("activeEmployees", employeeService.getActiveEmployees());
        model.addAttribute("departments", employeeService.getAllDepartments());
        
        return "dashboard";
    }
}
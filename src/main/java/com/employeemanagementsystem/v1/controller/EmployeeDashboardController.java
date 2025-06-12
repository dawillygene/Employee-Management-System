package com.employeemanagementsystem.v1.controller;

import com.employeemanagementsystem.v1.entity.Employee;
import com.employeemanagementsystem.v1.entity.Attendance;
import com.employeemanagementsystem.v1.entity.LeaveRequest;
import com.employeemanagementsystem.v1.service.CurrentUserService;
import com.employeemanagementsystem.v1.service.AttendanceService;
import com.employeemanagementsystem.v1.service.LeaveService;
import com.employeemanagementsystem.v1.repository.AttendanceRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

@Controller
@RequiredArgsConstructor
@PreAuthorize("hasRole('EMPLOYEE')")
public class EmployeeDashboardController {
    
    private final CurrentUserService currentUserService;
    private final AttendanceService attendanceService;
    private final LeaveService leaveService;
    private final AttendanceRepository attendanceRepository;
    
    @GetMapping("/employee-dashboard")
    public String employeeDashboard(Model model) {
        Optional<Employee> currentEmployeeOpt = currentUserService.getCurrentEmployee();
        
        if (currentEmployeeOpt.isEmpty()) {
            model.addAttribute("error", "No employee record found for current user. Please contact HR.");
            return "error/403";
        }
        
        Employee currentEmployee = currentEmployeeOpt.get();
        
        // Get recent attendance (last 7 days)
        LocalDate endDate = LocalDate.now();
        LocalDate startDate = endDate.minusDays(6);
        List<Attendance> recentAttendance = attendanceRepository.findByEmployeeAndDateBetweenOrderByDateDesc(
            currentEmployee, startDate, endDate);
        
        // Get recent leave requests (last 5)
        List<LeaveRequest> recentLeaveRequests = leaveService.findByEmployee(currentEmployee)
            .stream()
            .limit(5)
            .toList();
        
        // Calculate attendance statistics for current month
        LocalDate monthStart = LocalDate.now().withDayOfMonth(1);
        LocalDate monthEnd = LocalDate.now();
        double attendancePercentage = attendanceService.calculateAttendancePercentage(currentEmployee, monthStart, monthEnd);
        long presentDays = attendanceService.countPresentDays(currentEmployee, monthStart, monthEnd);
        long totalWorkingDays = attendanceService.countWorkingDaysBetween(monthStart, monthEnd);
        
        // Leave balances
        int vacationBalance = leaveService.getLeaveBalance(currentEmployee, LeaveRequest.LeaveType.VACATION);
        int sickBalance = leaveService.getLeaveBalance(currentEmployee, LeaveRequest.LeaveType.SICK_LEAVE);
        int personalBalance = leaveService.getLeaveBalance(currentEmployee, LeaveRequest.LeaveType.PERSONAL);
        long usedLeave = leaveService.getUsedLeaveThisYear(currentEmployee);
        
        model.addAttribute("pageTitle", "My Dashboard");
        model.addAttribute("pageDescription", "Welcome to your employee dashboard");
        model.addAttribute("activePage", "dashboard");
        model.addAttribute("currentEmployee", currentEmployee);
        model.addAttribute("recentAttendance", recentAttendance);
        model.addAttribute("recentLeaveRequests", recentLeaveRequests);
        model.addAttribute("attendancePercentage", attendancePercentage);
        model.addAttribute("presentDays", presentDays);
        model.addAttribute("totalWorkingDays", totalWorkingDays);
        model.addAttribute("vacationBalance", vacationBalance);
        model.addAttribute("sickBalance", sickBalance);
        model.addAttribute("personalBalance", personalBalance);
        model.addAttribute("usedLeave", usedLeave);
        
        return "employee/dashboard";
    }
    
    @GetMapping("/my-attendance")
    public String myAttendance(@RequestParam(defaultValue = "0") int page,
                              @RequestParam(defaultValue = "10") int size,
                              @RequestParam(required = false) String startDate,
                              @RequestParam(required = false) String endDate,
                              Model model) {
        
        Optional<Employee> currentEmployeeOpt = currentUserService.getCurrentEmployee();
        
        if (currentEmployeeOpt.isEmpty()) {
            model.addAttribute("error", "No employee record found for current user. Please contact HR.");
            return "error/403";
        }
        
        Employee currentEmployee = currentEmployeeOpt.get();
        
        // Set default date range (last 30 days if not specified)
        LocalDate start = startDate != null ? LocalDate.parse(startDate) : LocalDate.now().minusDays(30);
        LocalDate end = endDate != null ? LocalDate.parse(endDate) : LocalDate.now();
        
        Pageable pageable = PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "date"));
        Page<Attendance> attendanceRecords = attendanceService.findByEmployeeAndDateRange(currentEmployee, start, end, pageable);
        
        // Calculate attendance statistics
        double attendancePercentage = attendanceService.calculateAttendancePercentage(currentEmployee, start, end);
        long totalWorkingDays = attendanceService.countWorkingDaysBetween(start, end);
        long daysPresent = attendanceService.countPresentDays(currentEmployee, start, end);
        long daysAbsent = totalWorkingDays - daysPresent;
        
        model.addAttribute("pageTitle", "My Attendance");
        model.addAttribute("pageDescription", "View your attendance records");
        model.addAttribute("activePage", "attendance");
        model.addAttribute("employee", currentEmployee);
        model.addAttribute("attendanceRecords", attendanceRecords);
        model.addAttribute("attendancePercentage", attendancePercentage);
        model.addAttribute("totalWorkingDays", totalWorkingDays);
        model.addAttribute("daysPresent", daysPresent);
        model.addAttribute("daysAbsent", daysAbsent);
        model.addAttribute("startDate", start.toString());
        model.addAttribute("endDate", end.toString());
        
        return "attendance/my-attendance";
    }
}
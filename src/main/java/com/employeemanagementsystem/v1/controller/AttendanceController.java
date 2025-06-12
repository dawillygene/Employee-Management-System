package com.employeemanagementsystem.v1.controller;

import com.employeemanagementsystem.v1.entity.Attendance;
import com.employeemanagementsystem.v1.entity.Employee;
import com.employeemanagementsystem.v1.service.EmployeeService;
import com.employeemanagementsystem.v1.repository.AttendanceRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

@Controller
@RequestMapping("/attendance")
@RequiredArgsConstructor
public class AttendanceController {
    
    private final AttendanceRepository attendanceRepository;
    private final EmployeeService employeeService;
    
    @GetMapping
    public String listAttendance(@RequestParam(defaultValue = "") String search,
                                @RequestParam(defaultValue = "") String date,
                                @RequestParam(defaultValue = "") String status,
                                @RequestParam(defaultValue = "0") int page,
                                @RequestParam(defaultValue = "10") int size,
                                Model model) {
        
        Pageable pageable = PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "date"));
        
        LocalDate filterDate = date.isEmpty() ? LocalDate.now() : LocalDate.parse(date);
        
        // Get attendance records with proper filtering
        Page<Attendance> attendances;
        
        // Convert status string to enum if provided
        Attendance.AttendanceStatus statusEnum = null;
        if (!status.isEmpty()) {
            try {
                statusEnum = Attendance.AttendanceStatus.valueOf(status);
            } catch (IllegalArgumentException e) {
                // Invalid status, ignore it
            }
        }
        
        // Apply filters based on provided parameters
        if (!search.isEmpty() && statusEnum != null) {
            // Both search and status filter
            attendances = attendanceRepository.findByDateAndFilters(
                filterDate, 
                statusEnum, 
                search, 
                pageable
            );
        } else if (!search.isEmpty()) {
            // Only search filter
            attendances = attendanceRepository.findByDateAndSearch(filterDate, search, pageable);
        } else if (statusEnum != null) {
            // Only status filter
            attendances = attendanceRepository.findByDateAndStatus(filterDate, statusEnum, pageable);
        } else {
            // No filters, show all for the date
            attendances = attendanceRepository.findByDate(filterDate, pageable);
        }
        
        model.addAttribute("pageTitle", "Attendance Management");
        model.addAttribute("pageDescription", "Track daily attendance and manage check-ins/check-outs");
        model.addAttribute("activePage", "attendance");
        model.addAttribute("attendances", attendances);
        model.addAttribute("employees", employeeService.getAllActiveEmployees());
        model.addAttribute("search", search);
        model.addAttribute("selectedDate", date.isEmpty() ? LocalDate.now().toString() : date);
        model.addAttribute("selectedStatus", status);
        
        // Calculate statistics
        List<Employee> allEmployees = employeeService.getAllActiveEmployees();
        long totalEmployees = allEmployees.size();
        long presentToday = attendanceRepository.countByDateAndStatus(filterDate, Attendance.AttendanceStatus.PRESENT);
        long absentToday = totalEmployees - presentToday;
        
        model.addAttribute("totalEmployees", totalEmployees);
        model.addAttribute("presentToday", presentToday);
        model.addAttribute("absentToday", absentToday);
        model.addAttribute("attendanceRate", totalEmployees > 0 ? Math.round((double) presentToday / totalEmployees * 100) : 0);
        
        return "attendance/list";
    }
    
    @GetMapping("/checkin")
    public String showCheckInForm(Model model) {
        model.addAttribute("pageTitle", "Employee Check-In");
        model.addAttribute("pageDescription", "Record employee check-in time");
        model.addAttribute("activePage", "attendance");
        model.addAttribute("employees", employeeService.getAllActiveEmployees());
        return "attendance/checkin";
    }
    
    @PostMapping("/checkin")
    public String processCheckIn(@RequestParam Long employeeId, RedirectAttributes redirectAttributes) {
        try {
            Employee employee = employeeService.findById(employeeId);
            LocalDate today = LocalDate.now();
            
            // Check if employee already checked in today
            if (attendanceRepository.existsByEmployeeAndDate(employee, today)) {
                redirectAttributes.addFlashAttribute("error", "Employee has already checked in today!");
                return "redirect:/attendance";
            }
            
            Attendance attendance = new Attendance();
            attendance.setEmployee(employee);
            attendance.setDate(today);
            attendance.setCheckInTime(LocalDateTime.now().toLocalTime());
            attendance.setStatus(Attendance.AttendanceStatus.PRESENT);
            
            attendanceRepository.save(attendance);
            redirectAttributes.addFlashAttribute("success", 
                employee.getFullName() + " checked in successfully at " + 
                LocalDateTime.now().getHour() + ":" + 
                String.format("%02d", LocalDateTime.now().getMinute()));
                
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("error", "Error processing check-in: " + e.getMessage());
        }
        
        return "redirect:/attendance";
    }
    
    @PostMapping("/checkout/{id}")
    public String processCheckOut(@PathVariable Long id, RedirectAttributes redirectAttributes) {
        try {
            Attendance attendance = attendanceRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Attendance record not found"));
            
            if (attendance.getCheckOutTime() != null) {
                redirectAttributes.addFlashAttribute("error", "Employee has already checked out!");
                return "redirect:/attendance";
            }
            
            attendance.setCheckOutTime(LocalDateTime.now().toLocalTime());
            attendanceRepository.save(attendance);
            
            redirectAttributes.addFlashAttribute("success", 
                attendance.getEmployee().getFullName() + " checked out successfully at " + 
                LocalDateTime.now().getHour() + ":" + 
                String.format("%02d", LocalDateTime.now().getMinute()));
                
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("error", "Error processing check-out: " + e.getMessage());
        }
        
        return "redirect:/attendance";
    }
    
    @GetMapping("/mark-absent")
    public String showMarkAbsentForm(Model model) {
        model.addAttribute("pageTitle", "Mark Employee Absent");
        model.addAttribute("pageDescription", "Mark employees as absent for specific dates");
        model.addAttribute("activePage", "attendance");
        model.addAttribute("employees", employeeService.getAllActiveEmployees());
        return "attendance/mark-absent";
    }

    @PostMapping("/mark-absent")
    public String markAbsent(@RequestParam Long employeeId, 
                            @RequestParam String date,
                            RedirectAttributes redirectAttributes) {
        try {
            Employee employee = employeeService.findById(employeeId);
            LocalDate attendanceDate = LocalDate.parse(date);
            
            // Check if attendance record already exists
            if (attendanceRepository.existsByEmployeeAndDate(employee, attendanceDate)) {
                redirectAttributes.addFlashAttribute("error", "Attendance record already exists for this date!");
                return "redirect:/attendance";
            }
            
            Attendance attendance = new Attendance();
            attendance.setEmployee(employee);
            attendance.setDate(attendanceDate);
            attendance.setStatus(Attendance.AttendanceStatus.ABSENT);
            
            attendanceRepository.save(attendance);
            redirectAttributes.addFlashAttribute("success", 
                employee.getFullName() + " marked as absent for " + attendanceDate);
                
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("error", "Error marking absent: " + e.getMessage());
        }
        
        return "redirect:/attendance";
    }
}
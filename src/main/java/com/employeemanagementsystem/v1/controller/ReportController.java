package com.employeemanagementsystem.v1.controller;

import com.employeemanagementsystem.v1.dto.ReportFilterDto;
import com.employeemanagementsystem.v1.service.ReportService;
import com.employeemanagementsystem.v1.service.EmployeeService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;

@Controller
@RequestMapping("/reports")
@RequiredArgsConstructor
@Slf4j
public class ReportController {

    private final ReportService reportService;
    private final EmployeeService employeeService;

    @GetMapping
    public String showReportsPage(Model model) {
        model.addAttribute("pageTitle", "Reports & Analytics");
        model.addAttribute("pageDescription", "Generate comprehensive reports and analyze employee data");
        model.addAttribute("activePage", "reports");
        
        // Add dropdown data
        model.addAttribute("departments", employeeService.getAllDepartments());
        model.addAttribute("positions", employeeService.getAllPositions());
        
        return "reports/index";
    }

    @GetMapping("/employee")
    @PreAuthorize("hasRole('ADMIN') or hasRole('HR')")
    public ResponseEntity<ByteArrayResource> generateEmployeeReport(
            @RequestParam(defaultValue = "pdf") String format,
            @RequestParam(required = false) String department,
            @RequestParam(required = false) String position) {
        
        try {
            ReportFilterDto filter = new ReportFilterDto();
            filter.setDepartment(department);
            filter.setPosition(position);
            
            ByteArrayResource resource = reportService.generateEmployeeReport(format, filter);
            String filename = reportService.getReportFilename("employee", format);
            
            return ResponseEntity.ok()
                    .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + filename + "\"")
                    .contentType(getMediaType(format))
                    .body(resource);
                    
        } catch (Exception e) {
            log.error("Error generating employee report", e);
            return ResponseEntity.internalServerError().build();
        }
    }

    @GetMapping("/attendance")
    @PreAuthorize("hasRole('ADMIN') or hasRole('HR')")
    public ResponseEntity<ByteArrayResource> generateAttendanceReport(
            @RequestParam(defaultValue = "pdf") String format,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate,
            @RequestParam(required = false) String department) {
        
        try {
            ReportFilterDto filter = new ReportFilterDto();
            filter.setStartDate(startDate != null ? startDate : LocalDate.now().withDayOfMonth(1));
            filter.setEndDate(endDate != null ? endDate : LocalDate.now());
            filter.setDepartment(department);
            
            ByteArrayResource resource = reportService.generateAttendanceReport(format, filter);
            String filename = reportService.getReportFilename("attendance", format);
            
            return ResponseEntity.ok()
                    .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + filename + "\"")
                    .contentType(getMediaType(format))
                    .body(resource);
                    
        } catch (Exception e) {
            log.error("Error generating attendance report", e);
            return ResponseEntity.internalServerError().build();
        }
    }

    @GetMapping("/salary")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ByteArrayResource> generateSalaryReport(
            @RequestParam(defaultValue = "pdf") String format,
            @RequestParam(required = false) String department,
            @RequestParam(required = false) String position) {
        
        try {
            ReportFilterDto filter = new ReportFilterDto();
            filter.setDepartment(department);
            filter.setPosition(position);
            
            ByteArrayResource resource = reportService.generateSalaryReport(format, filter);
            String filename = reportService.getReportFilename("salary", format);
            
            return ResponseEntity.ok()
                    .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + filename + "\"")
                    .contentType(getMediaType(format))
                    .body(resource);
                    
        } catch (Exception e) {
            log.error("Error generating salary report", e);
            return ResponseEntity.internalServerError().build();
        }
    }

    @GetMapping("/performance")
    @PreAuthorize("hasRole('ADMIN') or hasRole('HR')")
    public ResponseEntity<ByteArrayResource> generatePerformanceReport(
            @RequestParam(defaultValue = "pdf") String format,
            @RequestParam(required = false) String department,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate) {
        
        try {
            ReportFilterDto filter = new ReportFilterDto();
            filter.setDepartment(department);
            filter.setStartDate(startDate);
            filter.setEndDate(endDate);
            
            ByteArrayResource resource = reportService.generatePerformanceReport(format, filter);
            String filename = reportService.getReportFilename("performance", format);
            
            return ResponseEntity.ok()
                    .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + filename + "\"")
                    .contentType(getMediaType(format))
                    .body(resource);
                    
        } catch (Exception e) {
            log.error("Error generating performance report", e);
            return ResponseEntity.internalServerError().build();
        }
    }

    @GetMapping("/leave")
    @PreAuthorize("hasRole('ADMIN') or hasRole('HR')")
    public ResponseEntity<ByteArrayResource> generateLeaveReport(
            @RequestParam(defaultValue = "pdf") String format,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate,
            @RequestParam(required = false) String status) {
        
        try {
            ReportFilterDto filter = new ReportFilterDto();
            filter.setStartDate(startDate != null ? startDate : LocalDate.now().withDayOfMonth(1));
            filter.setEndDate(endDate != null ? endDate : LocalDate.now());
            filter.setStatus(status);
            
            ByteArrayResource resource = reportService.generateLeaveReport(format, filter);
            String filename = reportService.getReportFilename("leave", format);
            
            return ResponseEntity.ok()
                    .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + filename + "\"")
                    .contentType(getMediaType(format))
                    .body(resource);
                    
        } catch (Exception e) {
            log.error("Error generating leave report", e);
            return ResponseEntity.internalServerError().build();
        }
    }

    @PostMapping("/custom")
    @PreAuthorize("hasRole('ADMIN') or hasRole('HR')")
    public ResponseEntity<ByteArrayResource> generateCustomReport(
            @RequestParam String reportType,
            @RequestParam(defaultValue = "pdf") String format,
            @RequestParam(required = false) String department,
            @RequestParam(required = false) String position,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate,
            @RequestParam(required = false) String status) {
        
        try {
            ReportFilterDto filter = new ReportFilterDto();
            filter.setDepartment(department);
            filter.setPosition(position);
            filter.setStartDate(startDate);
            filter.setEndDate(endDate);
            filter.setStatus(status);
            
            ByteArrayResource resource;
            String filename;
            
            switch (reportType.toLowerCase()) {
                case "employee":
                    resource = reportService.generateEmployeeReport(format, filter);
                    filename = reportService.getReportFilename("employee", format);
                    break;
                case "attendance":
                    resource = reportService.generateAttendanceReport(format, filter);
                    filename = reportService.getReportFilename("attendance", format);
                    break;
                case "salary":
                    // Check admin permission for salary reports
                    if (!hasAdminRole()) {
                        return ResponseEntity.status(403).build();
                    }
                    resource = reportService.generateSalaryReport(format, filter);
                    filename = reportService.getReportFilename("salary", format);
                    break;
                case "performance":
                    resource = reportService.generatePerformanceReport(format, filter);
                    filename = reportService.getReportFilename("performance", format);
                    break;
                case "leave":
                    resource = reportService.generateLeaveReport(format, filter);
                    filename = reportService.getReportFilename("leave", format);
                    break;
                default:
                    return ResponseEntity.badRequest().build();
            }
            
            return ResponseEntity.ok()
                    .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + filename + "\"")
                    .contentType(getMediaType(format))
                    .body(resource);
                    
        } catch (Exception e) {
            log.error("Error generating custom report", e);
            return ResponseEntity.internalServerError().build();
        }
    }

    private MediaType getMediaType(String format) {
        return switch (format.toLowerCase()) {
            case "excel", "xlsx" -> MediaType.parseMediaType("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet");
            case "csv" -> MediaType.parseMediaType("text/csv");
            default -> MediaType.APPLICATION_PDF;
        };
    }

    private boolean hasAdminRole() {
        return org.springframework.security.core.context.SecurityContextHolder
                .getContext()
                .getAuthentication()
                .getAuthorities()
                .stream()
                .anyMatch(grantedAuthority -> grantedAuthority.getAuthority().equals("ROLE_ADMIN"));
    }
}
package com.employeemanagementsystem.v1.service;

import com.employeemanagementsystem.v1.dto.ReportFilterDto;
import org.springframework.core.io.ByteArrayResource;

public interface ReportService {
    
    /**
     * Generate Employee List Report
     */
    ByteArrayResource generateEmployeeReport(String format, ReportFilterDto filter);
    
    /**
     * Generate Attendance Summary Report
     */
    ByteArrayResource generateAttendanceReport(String format, ReportFilterDto filter);
    
    /**
     * Generate Salary Summary Report
     */
    ByteArrayResource generateSalaryReport(String format, ReportFilterDto filter);
    
    /**
     * Generate Performance Review Report
     */
    ByteArrayResource generatePerformanceReport(String format, ReportFilterDto filter);
    
    /**
     * Generate Leave Balance Report
     */
    ByteArrayResource generateLeaveReport(String format, ReportFilterDto filter);
    
    /**
     * Get report filename with timestamp
     */
    String getReportFilename(String reportType, String format);
}
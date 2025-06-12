package com.employeemanagementsystem.v1.service.impl;

import com.employeemanagementsystem.v1.dto.ReportFilterDto;
import com.employeemanagementsystem.v1.entity.Employee;
import com.employeemanagementsystem.v1.entity.Attendance;
import com.employeemanagementsystem.v1.entity.PerformanceReview;
import com.employeemanagementsystem.v1.entity.LeaveRequest;
import com.employeemanagementsystem.v1.repository.EmployeeRepository;
import com.employeemanagementsystem.v1.repository.AttendanceRepository;
import com.employeemanagementsystem.v1.repository.PerformanceReviewRepository;
import com.employeemanagementsystem.v1.repository.LeaveRequestRepository;
import com.employeemanagementsystem.v1.service.ReportService;

// iText PDF imports
import com.itextpdf.text.*;
import com.itextpdf.text.pdf.*;

// Apache POI Excel imports
import org.apache.poi.ss.usermodel.*;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.stereotype.Service;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;

@Service
@RequiredArgsConstructor
@Slf4j
public class ReportServiceImpl implements ReportService {

    private final EmployeeRepository employeeRepository;
    private final AttendanceRepository attendanceRepository;
    private final PerformanceReviewRepository performanceReviewRepository;
    private final LeaveRequestRepository leaveRequestRepository;

    @Override
    public ByteArrayResource generateEmployeeReport(String format, ReportFilterDto filter) {
        try {
            List<Employee> employees = getFilteredEmployees(filter);
            
            if ("excel".equalsIgnoreCase(format)) {
                return generateEmployeeExcelReport(employees);
            } else {
                return generateEmployeePdfReport(employees);
            }
        } catch (Exception e) {
            log.error("Error generating employee report", e);
            throw new RuntimeException("Failed to generate employee report", e);
        }
    }

    @Override
    public ByteArrayResource generateAttendanceReport(String format, ReportFilterDto filter) {
        try {
            List<Attendance> attendanceList = getFilteredAttendance(filter);
            
            if ("excel".equalsIgnoreCase(format)) {
                return generateAttendanceExcelReport(attendanceList);
            } else {
                return generateAttendancePdfReport(attendanceList);
            }
        } catch (Exception e) {
            log.error("Error generating attendance report", e);
            throw new RuntimeException("Failed to generate attendance report", e);
        }
    }

    @Override
    public ByteArrayResource generateSalaryReport(String format, ReportFilterDto filter) {
        try {
            List<Employee> employees = getFilteredEmployees(filter);
            
            if ("excel".equalsIgnoreCase(format)) {
                return generateSalaryExcelReport(employees);
            } else {
                return generateSalaryPdfReport(employees);
            }
        } catch (Exception e) {
            log.error("Error generating salary report", e);
            throw new RuntimeException("Failed to generate salary report", e);
        }
    }

    @Override
    public ByteArrayResource generatePerformanceReport(String format, ReportFilterDto filter) {
        try {
            List<PerformanceReview> reviews = getFilteredPerformanceReviews(filter);
            
            if ("excel".equalsIgnoreCase(format)) {
                return generatePerformanceExcelReport(reviews);
            } else {
                return generatePerformancePdfReport(reviews);
            }
        } catch (Exception e) {
            log.error("Error generating performance report", e);
            throw new RuntimeException("Failed to generate performance report", e);
        }
    }

    @Override
    public ByteArrayResource generateLeaveReport(String format, ReportFilterDto filter) {
        try {
            List<LeaveRequest> leaveRequests = getFilteredLeaveRequests(filter);
            
            if ("excel".equalsIgnoreCase(format)) {
                return generateLeaveExcelReport(leaveRequests);
            } else {
                return generateLeavePdfReport(leaveRequests);
            }
        } catch (Exception e) {
            log.error("Error generating leave report", e);
            throw new RuntimeException("Failed to generate leave report", e);
        }
    }

    @Override
    public String getReportFilename(String reportType, String format) {
        String timestamp = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMdd_HHmmss"));
        return String.format("%s_report_%s.%s", reportType, timestamp, format.toLowerCase());
    }

    // Helper methods for data filtering
    private List<Employee> getFilteredEmployees(ReportFilterDto filter) {
        if (filter == null) {
            return employeeRepository.findAll();
        }
        
        // Apply filters based on filter criteria
        return employeeRepository.findBySearchCriteria(
            null, // search term
            filter.getDepartment(),
            filter.getPosition(),
            org.springframework.data.domain.Pageable.unpaged()
        ).getContent();
    }

    private List<Attendance> getFilteredAttendance(ReportFilterDto filter) {
        if (filter == null || filter.getStartDate() == null || filter.getEndDate() == null) {
            // Default to current month if no date range specified
            LocalDate startOfMonth = LocalDate.now().withDayOfMonth(1);
            LocalDate endOfMonth = LocalDate.now().withDayOfMonth(LocalDate.now().lengthOfMonth());
            return attendanceRepository.findAll().stream()
                .filter(a -> !a.getDate().isBefore(startOfMonth) && !a.getDate().isAfter(endOfMonth))
                .toList();
        }
        
        return attendanceRepository.findAll().stream()
            .filter(a -> !a.getDate().isBefore(filter.getStartDate()) && !a.getDate().isAfter(filter.getEndDate()))
            .toList();
    }

    private List<PerformanceReview> getFilteredPerformanceReviews(ReportFilterDto filter) {
        if (filter == null) {
            return performanceReviewRepository.findAll();
        }
        
        return performanceReviewRepository.findAll().stream()
            .filter(review -> {
                if (filter.getDepartment() != null && !filter.getDepartment().isEmpty()) {
                    return filter.getDepartment().equals(review.getEmployee().getDepartment());
                }
                return true;
            })
            .toList();
    }

    private List<LeaveRequest> getFilteredLeaveRequests(ReportFilterDto filter) {
        if (filter == null) {
            return leaveRequestRepository.findAll();
        }
        
        return leaveRequestRepository.findAll().stream()
            .filter(leave -> {
                if (filter.getStartDate() != null && filter.getEndDate() != null) {
                    return !leave.getStartDate().isAfter(filter.getEndDate()) && 
                           !leave.getEndDate().isBefore(filter.getStartDate());
                }
                return true;
            })
            .toList();
    }

    // Excel Report Generation Methods
    private ByteArrayResource generateEmployeeExcelReport(List<Employee> employees) throws IOException {
        try (Workbook workbook = new XSSFWorkbook()) {
            Sheet sheet = workbook.createSheet("Employee Report");
            
            // Create header style
            CellStyle headerStyle = workbook.createCellStyle();
            org.apache.poi.ss.usermodel.Font headerFont = workbook.createFont(); // Explicit Apache POI Font
            headerFont.setBold(true);
            headerStyle.setFont(headerFont);
            
            // Create header row
            Row headerRow = sheet.createRow(0);
            String[] headers = {"Employee ID", "Full Name", "Email", "Phone", "Department", 
                              "Position", "Salary", "Date of Joining", "Status"};
            
            for (int i = 0; i < headers.length; i++) {
                Cell cell = headerRow.createCell(i);
                cell.setCellValue(headers[i]);
                cell.setCellStyle(headerStyle);
            }
            
            // Fill data rows
            int rowIdx = 1;
            for (Employee employee : employees) {
                Row row = sheet.createRow(rowIdx++);
                row.createCell(0).setCellValue(employee.getEmployeeId());
                row.createCell(1).setCellValue(employee.getFullName());
                row.createCell(2).setCellValue(employee.getEmail());
                row.createCell(3).setCellValue(employee.getPhone());
                row.createCell(4).setCellValue(employee.getDepartment());
                row.createCell(5).setCellValue(employee.getPosition());
                row.createCell(6).setCellValue(employee.getSalary().doubleValue());
                row.createCell(7).setCellValue(employee.getDateOfJoining().toString());
                row.createCell(8).setCellValue(employee.getStatus().toString());
            }
            
            // Auto-size columns
            for (int i = 0; i < headers.length; i++) {
                sheet.autoSizeColumn(i);
            }
            
            ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
            workbook.write(outputStream);
            return new ByteArrayResource(outputStream.toByteArray());
        }
    }

    private ByteArrayResource generateAttendanceExcelReport(List<Attendance> attendanceList) throws IOException {
        try (Workbook workbook = new XSSFWorkbook()) {
            Sheet sheet = workbook.createSheet("Attendance Report");
            
            // Create header style
            CellStyle headerStyle = workbook.createCellStyle();
            org.apache.poi.ss.usermodel.Font headerFont = workbook.createFont();
            headerFont.setBold(true);
            headerStyle.setFont(headerFont);
            
            // Create header row
            Row headerRow = sheet.createRow(0);
            String[] headers = {"Employee ID", "Employee Name", "Date", "Check In", "Check Out", "Status"};
            
            for (int i = 0; i < headers.length; i++) {
                Cell cell = headerRow.createCell(i);
                cell.setCellValue(headers[i]);
                cell.setCellStyle(headerStyle);
            }
            
            // Fill data rows
            int rowIdx = 1;
            for (Attendance attendance : attendanceList) {
                Row row = sheet.createRow(rowIdx++);
                row.createCell(0).setCellValue(attendance.getEmployee().getEmployeeId());
                row.createCell(1).setCellValue(attendance.getEmployee().getFullName());
                row.createCell(2).setCellValue(attendance.getDate().toString());
                row.createCell(3).setCellValue(attendance.getCheckInTime() != null ? attendance.getCheckInTime().toString() : "");
                row.createCell(4).setCellValue(attendance.getCheckOutTime() != null ? attendance.getCheckOutTime().toString() : "");
                row.createCell(5).setCellValue(attendance.getStatus().toString());
            }
            
            // Auto-size columns
            for (int i = 0; i < headers.length; i++) {
                sheet.autoSizeColumn(i);
            }
            
            ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
            workbook.write(outputStream);
            return new ByteArrayResource(outputStream.toByteArray());
        }
    }

    private ByteArrayResource generateSalaryExcelReport(List<Employee> employees) throws IOException {
        try (Workbook workbook = new XSSFWorkbook()) {
            Sheet sheet = workbook.createSheet("Salary Report");
            
            // Create header style
            CellStyle headerStyle = workbook.createCellStyle();
            org.apache.poi.ss.usermodel.Font headerFont = workbook.createFont();
            headerFont.setBold(true);
            headerStyle.setFont(headerFont);
            
            // Create header row
            Row headerRow = sheet.createRow(0);
            String[] headers = {"Employee ID", "Full Name", "Department", "Position", "Salary"};
            
            for (int i = 0; i < headers.length; i++) {
                Cell cell = headerRow.createCell(i);
                cell.setCellValue(headers[i]);
                cell.setCellStyle(headerStyle);
            }
            
            // Fill data rows
            int rowIdx = 1;
            BigDecimal totalSalary = BigDecimal.ZERO;
            
            for (Employee employee : employees) {
                Row row = sheet.createRow(rowIdx++);
                row.createCell(0).setCellValue(employee.getEmployeeId());
                row.createCell(1).setCellValue(employee.getFullName());
                row.createCell(2).setCellValue(employee.getDepartment());
                row.createCell(3).setCellValue(employee.getPosition());
                row.createCell(4).setCellValue(employee.getSalary().doubleValue());
                
                totalSalary = totalSalary.add(employee.getSalary());
            }
            
            // Add total row
            Row totalRow = sheet.createRow(rowIdx + 1);
            totalRow.createCell(3).setCellValue("TOTAL:");
            totalRow.createCell(4).setCellValue(totalSalary.doubleValue());
            
            // Auto-size columns
            for (int i = 0; i < headers.length; i++) {
                sheet.autoSizeColumn(i);
            }
            
            ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
            workbook.write(outputStream);
            return new ByteArrayResource(outputStream.toByteArray());
        }
    }

    private ByteArrayResource generatePerformanceExcelReport(List<PerformanceReview> reviews) throws IOException {
        try (Workbook workbook = new XSSFWorkbook()) {
            Sheet sheet = workbook.createSheet("Performance Report");
            
            // Create header style
            CellStyle headerStyle = workbook.createCellStyle();
            org.apache.poi.ss.usermodel.Font headerFont = workbook.createFont();
            headerFont.setBold(true);
            headerStyle.setFont(headerFont);
            
            // Create header row
            Row headerRow = sheet.createRow(0);
            String[] headers = {"Employee ID", "Employee Name", "Department", "Review Period", 
                              "Overall Rating", "Manager Comments", "Review Date"};
            
            for (int i = 0; i < headers.length; i++) {
                Cell cell = headerRow.createCell(i);
                cell.setCellValue(headers[i]);
                cell.setCellStyle(headerStyle);
            }
            
            // Fill data rows
            int rowIdx = 1;
            for (PerformanceReview review : reviews) {
                Row row = sheet.createRow(rowIdx++);
                row.createCell(0).setCellValue(review.getEmployee().getEmployeeId());
                row.createCell(1).setCellValue(review.getEmployee().getFullName());
                row.createCell(2).setCellValue(review.getEmployee().getDepartment());
                row.createCell(3).setCellValue(review.getReviewPeriod());
                row.createCell(4).setCellValue(review.getOverallRating() != null ? review.getOverallRating().doubleValue() : 0.0);
                row.createCell(5).setCellValue(review.getManagerComments() != null ? review.getManagerComments() : "");
                row.createCell(6).setCellValue(review.getReviewDate() != null ? review.getReviewDate().toString() : "");
            }
            
            // Auto-size columns
            for (int i = 0; i < headers.length; i++) {
                sheet.autoSizeColumn(i);
            }
            
            ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
            workbook.write(outputStream);
            return new ByteArrayResource(outputStream.toByteArray());
        }
    }

    private ByteArrayResource generateLeaveExcelReport(List<LeaveRequest> leaveRequests) throws IOException {
        try (Workbook workbook = new XSSFWorkbook()) {
            Sheet sheet = workbook.createSheet("Leave Report");
            
            // Create header style
            CellStyle headerStyle = workbook.createCellStyle();
            org.apache.poi.ss.usermodel.Font headerFont = workbook.createFont();
            headerFont.setBold(true);
            headerStyle.setFont(headerFont);
            
            // Create header row
            Row headerRow = sheet.createRow(0);
            String[] headers = {"Employee ID", "Employee Name", "Leave Type", "Start Date", 
                              "End Date", "Days", "Status", "Applied Date"};
            
            for (int i = 0; i < headers.length; i++) {
                Cell cell = headerRow.createCell(i);
                cell.setCellValue(headers[i]);
                cell.setCellStyle(headerStyle);
            }
            
            // Fill data rows
            int rowIdx = 1;
            for (LeaveRequest leave : leaveRequests) {
                Row row = sheet.createRow(rowIdx++);
                row.createCell(0).setCellValue(leave.getEmployee().getEmployeeId());
                row.createCell(1).setCellValue(leave.getEmployee().getFullName());
                row.createCell(2).setCellValue(leave.getLeaveType().toString());
                row.createCell(3).setCellValue(leave.getStartDate().toString());
                row.createCell(4).setCellValue(leave.getEndDate().toString());
                row.createCell(5).setCellValue(leave.getLeaveDuration()); // Using correct method name
                row.createCell(6).setCellValue(leave.getStatus().toString());
                row.createCell(7).setCellValue(leave.getCreatedAt().toLocalDate().toString());
            }
            
            // Auto-size columns
            for (int i = 0; i < headers.length; i++) {
                sheet.autoSizeColumn(i);
            }
            
            ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
            workbook.write(outputStream);
            return new ByteArrayResource(outputStream.toByteArray());
        }
    }

    // PDF Report Generation Methods - Updated for iText 5.x
    private ByteArrayResource generateEmployeePdfReport(List<Employee> employees) throws IOException, DocumentException {
        ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
        Document document = new Document(PageSize.A4.rotate()); // Landscape for better table fit
        PdfWriter.getInstance(document, outputStream);
        document.open();
        
        // Title - Using iText Font explicitly
        com.itextpdf.text.Font titleFont = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 18);
        Paragraph title = new Paragraph("Employee Report", titleFont);
        title.setAlignment(Element.ALIGN_CENTER);
        document.add(title);
        
        Paragraph date = new Paragraph("Generated on: " + LocalDate.now().toString());
        date.setAlignment(Element.ALIGN_CENTER);
        document.add(date);
        document.add(new Paragraph(" ")); // Space
        
        // Create table
        PdfPTable table = new PdfPTable(8);
        table.setWidthPercentage(100);
        
        // Headers
        String[] headers = {"ID", "Name", "Email", "Phone", "Department", "Position", "Salary", "Status"};
        for (String header : headers) {
            PdfPCell cell = new PdfPCell(new Phrase(header, FontFactory.getFont(FontFactory.HELVETICA_BOLD)));
            cell.setBackgroundColor(BaseColor.LIGHT_GRAY);
            table.addCell(cell);
        }
        
        // Data rows
        for (Employee employee : employees) {
            table.addCell(employee.getEmployeeId());
            table.addCell(employee.getFullName());
            table.addCell(employee.getEmail());
            table.addCell(employee.getPhone() != null ? employee.getPhone() : "");
            table.addCell(employee.getDepartment());
            table.addCell(employee.getPosition());
            table.addCell("Tsh " + String.format("%,.2f", employee.getSalary()));
            table.addCell(employee.getStatus().toString());
        }
        
        document.add(table);
        document.close();
        
        return new ByteArrayResource(outputStream.toByteArray());
    }

    private ByteArrayResource generateAttendancePdfReport(List<Attendance> attendanceList) throws IOException, DocumentException {
        ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
        Document document = new Document(PageSize.A4.rotate());
        PdfWriter.getInstance(document, outputStream);
        document.open();
        
        com.itextpdf.text.Font titleFont = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 18);
        Paragraph title = new Paragraph("Attendance Report", titleFont);
        title.setAlignment(Element.ALIGN_CENTER);
        document.add(title);
        
        Paragraph date = new Paragraph("Generated on: " + LocalDate.now().toString());
        date.setAlignment(Element.ALIGN_CENTER);
        document.add(date);
        document.add(new Paragraph(" "));
        
        PdfPTable table = new PdfPTable(6);
        table.setWidthPercentage(100);
        
        String[] headers = {"Emp ID", "Name", "Date", "Check In", "Check Out", "Status"};
        for (String header : headers) {
            PdfPCell cell = new PdfPCell(new Phrase(header, FontFactory.getFont(FontFactory.HELVETICA_BOLD)));
            cell.setBackgroundColor(BaseColor.LIGHT_GRAY);
            table.addCell(cell);
        }
        
        for (Attendance attendance : attendanceList) {
            table.addCell(attendance.getEmployee().getEmployeeId());
            table.addCell(attendance.getEmployee().getFullName());
            table.addCell(attendance.getDate().toString());
            table.addCell(attendance.getCheckInTime() != null ? attendance.getCheckInTime().toString() : "");
            table.addCell(attendance.getCheckOutTime() != null ? attendance.getCheckOutTime().toString() : "");
            table.addCell(attendance.getStatus().toString());
        }
        
        document.add(table);
        document.close();
        
        return new ByteArrayResource(outputStream.toByteArray());
    }

    private ByteArrayResource generateSalaryPdfReport(List<Employee> employees) throws IOException, DocumentException {
        ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
        Document document = new Document(PageSize.A4);
        PdfWriter.getInstance(document, outputStream);
        document.open();
        
        com.itextpdf.text.Font titleFont = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 18);
        Paragraph title = new Paragraph("Salary Report", titleFont);
        title.setAlignment(Element.ALIGN_CENTER);
        document.add(title);
        
        Paragraph date = new Paragraph("Generated on: " + LocalDate.now().toString());
        date.setAlignment(Element.ALIGN_CENTER);
        document.add(date);
        document.add(new Paragraph(" "));
        
        PdfPTable table = new PdfPTable(5);
        table.setWidthPercentage(100);
        
        String[] headers = {"Employee ID", "Name", "Department", "Position", "Salary (Tsh)"};
        for (String header : headers) {
            PdfPCell cell = new PdfPCell(new Phrase(header, FontFactory.getFont(FontFactory.HELVETICA_BOLD)));
            cell.setBackgroundColor(BaseColor.LIGHT_GRAY);
            table.addCell(cell);
        }
        
        BigDecimal totalSalary = BigDecimal.ZERO;
        for (Employee employee : employees) {
            table.addCell(employee.getEmployeeId());
            table.addCell(employee.getFullName());
            table.addCell(employee.getDepartment());
            table.addCell(employee.getPosition());
            table.addCell("Tsh " + String.format("%,.2f", employee.getSalary()));
            totalSalary = totalSalary.add(employee.getSalary());
        }
        
        // Add total row
        table.addCell("");
        table.addCell("");
        table.addCell("");
        PdfPCell totalLabelCell = new PdfPCell(new Phrase("TOTAL:", FontFactory.getFont(FontFactory.HELVETICA_BOLD)));
        table.addCell(totalLabelCell);
        PdfPCell totalValueCell = new PdfPCell(new Phrase("Tsh " + String.format("%,.2f", totalSalary), FontFactory.getFont(FontFactory.HELVETICA_BOLD)));
        table.addCell(totalValueCell);
        
        document.add(table);
        document.close();
        
        return new ByteArrayResource(outputStream.toByteArray());
    }

    private ByteArrayResource generatePerformancePdfReport(List<PerformanceReview> reviews) throws IOException, DocumentException {
        ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
        Document document = new Document(PageSize.A4.rotate());
        PdfWriter.getInstance(document, outputStream);
        document.open();
        
        com.itextpdf.text.Font titleFont = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 18);
        Paragraph title = new Paragraph("Performance Report", titleFont);
        title.setAlignment(Element.ALIGN_CENTER);
        document.add(title);
        
        Paragraph date = new Paragraph("Generated on: " + LocalDate.now().toString());
        date.setAlignment(Element.ALIGN_CENTER);
        document.add(date);
        document.add(new Paragraph(" "));
        
        PdfPTable table = new PdfPTable(5);
        table.setWidthPercentage(100);
        
        String[] headers = {"Emp ID", "Name", "Department", "Period", "Rating"};
        for (String header : headers) {
            PdfPCell cell = new PdfPCell(new Phrase(header, FontFactory.getFont(FontFactory.HELVETICA_BOLD)));
            cell.setBackgroundColor(BaseColor.LIGHT_GRAY);
            table.addCell(cell);
        }
        
        for (PerformanceReview review : reviews) {
            table.addCell(review.getEmployee().getEmployeeId());
            table.addCell(review.getEmployee().getFullName());
            table.addCell(review.getEmployee().getDepartment());
            table.addCell(review.getReviewPeriod());
            table.addCell(review.getOverallRating() != null ? review.getOverallRating().toString() : "N/A");
        }
        
        document.add(table);
        document.close();
        
        return new ByteArrayResource(outputStream.toByteArray());
    }

    private ByteArrayResource generateLeavePdfReport(List<LeaveRequest> leaveRequests) throws IOException, DocumentException {
        ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
        Document document = new Document(PageSize.A4.rotate());
        PdfWriter.getInstance(document, outputStream);
        document.open();
        
        com.itextpdf.text.Font titleFont = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 18);
        Paragraph title = new Paragraph("Leave Report", titleFont);
        title.setAlignment(Element.ALIGN_CENTER);
        document.add(title);
        
        Paragraph date = new Paragraph("Generated on: " + LocalDate.now().toString());
        date.setAlignment(Element.ALIGN_CENTER);
        document.add(date);
        document.add(new Paragraph(" "));
        
        PdfPTable table = new PdfPTable(7);
        table.setWidthPercentage(100);
        
        String[] headers = {"Emp ID", "Name", "Type", "Start", "End", "Days", "Status"};
        for (String header : headers) {
            PdfPCell cell = new PdfPCell(new Phrase(header, FontFactory.getFont(FontFactory.HELVETICA_BOLD)));
            cell.setBackgroundColor(BaseColor.LIGHT_GRAY);
            table.addCell(cell);
        }
        
        for (LeaveRequest leave : leaveRequests) {
            table.addCell(leave.getEmployee().getEmployeeId());
            table.addCell(leave.getEmployee().getFullName());
            table.addCell(leave.getLeaveType().toString());
            table.addCell(leave.getStartDate().toString());
            table.addCell(leave.getEndDate().toString());
            table.addCell(String.valueOf(leave.getLeaveDuration())); // Using correct method name
            table.addCell(leave.getStatus().toString());
        }
        
        document.add(table);
        document.close();
        
        return new ByteArrayResource(outputStream.toByteArray());
    }
}
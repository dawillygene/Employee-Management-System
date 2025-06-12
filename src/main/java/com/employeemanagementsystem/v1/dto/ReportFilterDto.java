package com.employeemanagementsystem.v1.dto;

import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;

import java.time.LocalDate;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class ReportFilterDto {
    
    private String department;
    private String position;
    private LocalDate startDate;
    private LocalDate endDate;
    private String employeeId;
    private String status;
    private String performanceRating;
    
    // Convenience constructor for date range filtering
    public ReportFilterDto(LocalDate startDate, LocalDate endDate) {
        this.startDate = startDate;
        this.endDate = endDate;
    }
    
    // Convenience constructor for department filtering
    public ReportFilterDto(String department) {
        this.department = department;
    }
}
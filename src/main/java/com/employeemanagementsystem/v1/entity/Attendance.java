package com.employeemanagementsystem.v1.entity;

import jakarta.persistence.*;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;

@Entity
@Table(name = "attendance")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class Attendance {
    
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "employee_id", nullable = false)
    @NotNull(message = "Employee is required")
    private Employee employee;
    
    @NotNull(message = "Date is required")
    @Column(nullable = false)
    private LocalDate date;
    
    @Column(name = "check_in_time")
    private LocalTime checkInTime;
    
    @Column(name = "check_out_time")
    private LocalTime checkOutTime;
    
    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private AttendanceStatus status = AttendanceStatus.ABSENT;
    
    @Column(columnDefinition = "TEXT")
    private String notes;
    
    @CreationTimestamp
    @Column(updatable = false)
    private LocalDateTime createdAt;
    
    public enum AttendanceStatus {
        PRESENT, ABSENT, HALF_DAY, LATE
    }
    
    // Unique constraint to prevent duplicate attendance records for the same employee on the same date
    @Table(uniqueConstraints = {@UniqueConstraint(columnNames = {"employee_id", "date"})})
    public static class AttendanceConstraint {}
}
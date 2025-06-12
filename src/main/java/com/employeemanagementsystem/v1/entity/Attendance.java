package com.employeemanagementsystem.v1.entity;

import jakarta.persistence.*;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;

@Entity
@Table(name = "attendance")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class Attendance {
    
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    @ManyToOne(fetch = FetchType.EAGER)
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

    @Column(name = "hours_worked")
    private Double hoursWorked;
    
    @CreationTimestamp
    @Column(updatable = false)
    private LocalDateTime createdAt;
    
    public enum AttendanceStatus {
        PRESENT, ABSENT, HALF_DAY, LATE
    }
    
    // Custom toString method to avoid circular references
    @Override
    public String toString() {
        return "Attendance{" +
                "id=" + id +
                ", employeeId=" + (employee != null ? employee.getId() : "null") +
                ", date=" + date +
                ", checkInTime=" + checkInTime +
                ", checkOutTime=" + checkOutTime +
                ", status=" + status +
                '}';
    }
    
    // Custom equals and hashCode methods
    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Attendance that = (Attendance) o;
        return id != null && id.equals(that.id);
    }
    
    @Override
    public int hashCode() {
        return id != null ? id.hashCode() : 0;
    }
    
    // Unique constraint to prevent duplicate attendance records for the same employee on the same date
    @Table(uniqueConstraints = {@UniqueConstraint(columnNames = {"employee_id", "date"})})
    public static class AttendanceConstraint {}

    // Method to calculate hours worked if both check-in and check-out times are available
    public Double calculateHoursWorked() {
        if (checkInTime != null && checkOutTime != null) {
            return (double) java.time.Duration.between(checkInTime, checkOutTime).toMinutes() / 60.0;
        }
        return null;
    }

    // Update hours worked when check-out time is set
    public void setCheckOutTime(LocalTime checkOutTime) {
        this.checkOutTime = checkOutTime;
        this.hoursWorked = calculateHoursWorked();
    }
}
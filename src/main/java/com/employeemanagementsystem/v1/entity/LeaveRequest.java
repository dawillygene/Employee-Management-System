package com.employeemanagementsystem.v1.entity;

import jakarta.persistence.*;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.LocalDate;
import java.time.LocalDateTime;

@Entity
@Table(name = "leave_requests")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class LeaveRequest {
    
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "employee_id", nullable = false)
    @NotNull(message = "Employee is required")
    private Employee employee;
    
    @NotNull(message = "Start date is required")
    @Column(nullable = false)
    private LocalDate startDate;
    
    @NotNull(message = "End date is required")
    @Column(nullable = false)
    private LocalDate endDate;
    
    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private LeaveType leaveType;
    
    @NotBlank(message = "Reason is required")
    @Column(nullable = false, columnDefinition = "TEXT")
    private String reason;
    
    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private LeaveStatus status = LeaveStatus.PENDING;
    
    @Column(columnDefinition = "TEXT")
    private String managerComments;
    
    @Column(name = "approved_by")
    private String approvedBy;
    
    @Column(name = "approved_at")
    private LocalDateTime approvedAt;
    
    @CreationTimestamp
    @Column(updatable = false)
    private LocalDateTime createdAt;
    
    @UpdateTimestamp
    private LocalDateTime updatedAt;
    
    public enum LeaveType {
        SICK_LEAVE, VACATION, PERSONAL, MATERNITY, PATERNITY, EMERGENCY
    }
    
    public enum LeaveStatus {
        PENDING, APPROVED, REJECTED, CANCELLED
    }
    
    // Helper method to calculate leave duration
    @Transient
    public long getLeaveDuration() {
        if (startDate != null && endDate != null) {
            return startDate.until(endDate).getDays() + 1;
        }
        return 0;
    }
}
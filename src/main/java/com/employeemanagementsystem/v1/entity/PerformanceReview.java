package com.employeemanagementsystem.v1.entity;

import jakarta.persistence.*;
import jakarta.validation.constraints.*;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.LocalDateTime;
import java.time.LocalDate;

@Entity
@Table(name = "performance_reviews", 
       uniqueConstraints = @UniqueConstraint(
           columnNames = {"employee_id", "review_date"}, 
           name = "uk_employee_review_date"
       ))
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class PerformanceReview {
    
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "employee_id", nullable = false)
    @NotNull(message = "Employee is required")
    private Employee employee;
    
    @NotBlank(message = "Review period is required")
    @Column(nullable = false)
    private String reviewPeriod; // e.g., "Q1 2024", "2024"
    
    // Overall rating is calculated, not user input - remove validation
    @Column(nullable = false)
    private Double overallRating;
    
    @NotNull(message = "Technical skills rating is required")
    @DecimalMin(value = "1.0", message = "Technical skills rating must be at least 1.0")
    @DecimalMax(value = "5.0", message = "Technical skills rating must not exceed 5.0")
    @Column(nullable = false)
    private Double technicalSkills;
    
    @NotNull(message = "Communication rating is required")
    @DecimalMin(value = "1.0", message = "Communication rating must be at least 1.0")
    @DecimalMax(value = "5.0", message = "Communication rating must not exceed 5.0")
    @Column(nullable = false)
    private Double communication;
    
    @NotNull(message = "Leadership rating is required")
    @DecimalMin(value = "1.0", message = "Leadership rating must be at least 1.0")
    @DecimalMax(value = "5.0", message = "Leadership rating must not exceed 5.0")
    @Column(nullable = false)
    private Double leadership;
    
    @NotNull(message = "Punctuality rating is required")
    @DecimalMin(value = "1.0", message = "Punctuality rating must be at least 1.0")
    @DecimalMax(value = "5.0", message = "Punctuality rating must not exceed 5.0")
    @Column(nullable = false)
    private Double punctuality;
    
    @NotNull(message = "Teamwork rating is required")
    @DecimalMin(value = "1.0", message = "Teamwork rating must be at least 1.0")
    @DecimalMax(value = "5.0", message = "Teamwork rating must not exceed 5.0")
    @Column(nullable = false)
    private Double teamwork;
    
    @NotNull(message = "Problem solving rating is required")
    @DecimalMin(value = "1.0", message = "Problem solving rating must be at least 1.0")
    @DecimalMax(value = "5.0", message = "Problem solving rating must not exceed 5.0")
    @Column(nullable = false)
    private Double problemSolving;
    
    @Column(columnDefinition = "TEXT")
    private String achievements;
    
    @Column(columnDefinition = "TEXT")
    private String areasForImprovement;
    
    @Column(columnDefinition = "TEXT")
    private String goals;
    
    @Column(columnDefinition = "TEXT")
    private String goalsForNextPeriod;
    
    @NotBlank(message = "Manager comments are required")
    @Column(nullable = false, columnDefinition = "TEXT")
    private String managerComments;
    
    // Reviewer name is auto-set, not user input - remove validation
    @Column(nullable = false)
    private String reviewedBy;
    
    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private ReviewStatus status = ReviewStatus.DRAFT;
    
    @Column
    private LocalDate reviewDate;
    
    @CreationTimestamp
    @Column(updatable = false)
    private LocalDateTime createdAt;
    
    @UpdateTimestamp
    private LocalDateTime updatedAt;
    
    // Helper method to get performance category based on overall rating
    @Transient
    public String getPerformanceCategory() {
        if (overallRating == null) return "Not Rated";
        
        if (overallRating >= 4.5) return "Excellent";
        else if (overallRating >= 3.5) return "Good";
        else if (overallRating >= 2.5) return "Average";
        else return "Needs Improvement";
    }
    
    public enum ReviewStatus {
        DRAFT, IN_PROGRESS, COMPLETED, APPROVED
    }
    
    // Custom toString method to avoid circular references
    @Override
    public String toString() {
        return "PerformanceReview{" +
                "id=" + id +
                ", employeeId=" + (employee != null ? employee.getId() : "null") +
                ", reviewPeriod='" + reviewPeriod + '\'' +
                ", overallRating=" + overallRating +
                ", status=" + status +
                ", reviewDate=" + reviewDate +
                '}';
    }
    
    // Custom equals and hashCode methods
    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        PerformanceReview that = (PerformanceReview) o;
        return id != null && id.equals(that.id);
    }
    
    @Override
    public int hashCode() {
        return id != null ? id.hashCode() : 0;
    }
}
package com.employeemanagementsystem.v1.entity;

import jakarta.persistence.*;
import jakarta.validation.constraints.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.LocalDateTime;

@Entity
@Table(name = "performance_reviews")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class PerformanceReview {
    
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "employee_id", nullable = false)
    @NotNull(message = "Employee is required")
    private Employee employee;
    
    @NotBlank(message = "Review period is required")
    @Column(nullable = false)
    private String reviewPeriod; // e.g., "Q1 2024", "2024"
    
    @NotNull(message = "Overall rating is required")
    @DecimalMin(value = "1.0", message = "Rating must be at least 1.0")
    @DecimalMax(value = "5.0", message = "Rating must not exceed 5.0")
    @Column(nullable = false)
    private Double overallRating;
    
    @DecimalMin(value = "1.0", message = "Technical skills rating must be at least 1.0")
    @DecimalMax(value = "5.0", message = "Technical skills rating must not exceed 5.0")
    @Column
    private Double technicalSkills;
    
    @DecimalMin(value = "1.0", message = "Communication rating must be at least 1.0")
    @DecimalMax(value = "5.0", message = "Communication rating must not exceed 5.0")
    @Column
    private Double communication;
    
    @DecimalMin(value = "1.0", message = "Leadership rating must be at least 1.0")
    @DecimalMax(value = "5.0", message = "Leadership rating must not exceed 5.0")
    @Column
    private Double leadership;
    
    @DecimalMin(value = "1.0", message = "Punctuality rating must be at least 1.0")
    @DecimalMax(value = "5.0", message = "Punctuality rating must not exceed 5.0")
    @Column
    private Double punctuality;
    
    @Column(columnDefinition = "TEXT")
    private String achievements;
    
    @Column(columnDefinition = "TEXT")
    private String areasForImprovement;
    
    @Column(columnDefinition = "TEXT")
    private String goals;
    
    @NotBlank(message = "Manager comments are required")
    @Column(nullable = false, columnDefinition = "TEXT")
    private String managerComments;
    
    @NotBlank(message = "Reviewer name is required")
    @Column(nullable = false)
    private String reviewedBy;
    
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
}
package com.employeemanagementsystem.v1.service;

import com.employeemanagementsystem.v1.entity.Employee;
import com.employeemanagementsystem.v1.entity.PerformanceReview;
import com.employeemanagementsystem.v1.repository.PerformanceReviewRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class PerformanceReviewService {
    
    private final PerformanceReviewRepository performanceReviewRepository;
    
    public Page<PerformanceReview> findByEmployee(Employee employee, Pageable pageable) {
        return performanceReviewRepository.findByEmployee(employee, pageable);
    }
    
    public Double calculateAverageRating(Employee employee) {
        return performanceReviewRepository.findAverageRatingByEmployee(employee);
    }
    
    public String getPerformanceCategory(Employee employee) {
        Double averageRating = calculateAverageRating(employee);
        if (averageRating == null) {
            return "Not Rated";
        }
        
        if (averageRating >= 4.5) {
            return "Excellent";
        } else if (averageRating >= 3.5) {
            return "Good";
        } else if (averageRating >= 2.5) {
            return "Average";
        } else {
            return "Needs Improvement";
        }
    }
    
    public int countReviewsByEmployee(Employee employee) {
        return (int) performanceReviewRepository.countByEmployee(employee);
    }
    
    public PerformanceReview findLatestReviewByEmployee(Employee employee) {
        return performanceReviewRepository.findTopByEmployeeOrderByReviewDateDesc(employee);
    }
}
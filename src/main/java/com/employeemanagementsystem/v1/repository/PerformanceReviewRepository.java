package com.employeemanagementsystem.v1.repository;

import com.employeemanagementsystem.v1.entity.PerformanceReview;
import com.employeemanagementsystem.v1.entity.Employee;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface PerformanceReviewRepository extends JpaRepository<PerformanceReview, Long> {
    List<PerformanceReview> findByEmployee(Employee employee);
    List<PerformanceReview> findByReviewPeriod(String reviewPeriod);
    Optional<PerformanceReview> findByEmployeeAndReviewPeriod(Employee employee, String reviewPeriod);
    
    @Query("SELECT pr FROM PerformanceReview pr ORDER BY pr.createdAt DESC")
    List<PerformanceReview> findAllOrderByCreatedAtDesc();
    
    @Query("SELECT AVG(pr.overallRating) FROM PerformanceReview pr WHERE pr.employee.department = :department")
    Double findAverageRatingByDepartment(@Param("department") String department);
    
    @Query("SELECT COUNT(pr) FROM PerformanceReview pr WHERE pr.overallRating >= :minRating")
    long countByRatingGreaterThanEqual(@Param("minRating") Double minRating);
}
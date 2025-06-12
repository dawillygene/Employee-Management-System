package com.employeemanagementsystem.v1.repository;

import com.employeemanagementsystem.v1.entity.Employee;
import com.employeemanagementsystem.v1.entity.PerformanceReview;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface PerformanceReviewRepository extends JpaRepository<PerformanceReview, Long> {
    List<PerformanceReview> findByEmployee(Employee employee);
    Page<PerformanceReview> findByEmployee(Employee employee, Pageable pageable);
    List<PerformanceReview> findByReviewPeriod(String reviewPeriod);
    Optional<PerformanceReview> findByEmployeeAndReviewPeriod(Employee employee, String reviewPeriod);
    long countByEmployee(Employee employee);
    PerformanceReview findTopByEmployeeOrderByReviewDateDesc(Employee employee);
    
    @Query("SELECT pr FROM PerformanceReview pr ORDER BY pr.createdAt DESC")
    List<PerformanceReview> findAllOrderByCreatedAtDesc();
    
    @Query("SELECT AVG(pr.overallRating) FROM PerformanceReview pr WHERE pr.employee.department = :department")
    Double findAverageRatingByDepartment(@Param("department") String department);
    
    @Query("SELECT AVG(pr.overallRating) FROM PerformanceReview pr WHERE pr.employee = :employee")
    Double findAverageRatingByEmployee(@Param("employee") Employee employee);
    
    @Query("SELECT COUNT(pr) FROM PerformanceReview pr WHERE pr.overallRating >= :minRating")
    long countByRatingGreaterThanEqual(@Param("minRating") Double minRating);
    
    List<PerformanceReview> findByEmployeeOrderByReviewDateDesc(Employee employee);
    
    boolean existsByEmployeeAndReviewPeriod(Employee employee, String reviewPeriod);
    
    boolean existsByEmployeeAndReviewDate(Employee employee, java.time.LocalDate reviewDate);
    
    long countByOverallRatingGreaterThanEqual(double rating);
    
    long countByOverallRatingLessThan(double rating);
    
    long countByStatus(PerformanceReview.ReviewStatus status);
    
    @Query("SELECT AVG(pr.overallRating) FROM PerformanceReview pr")
    Double findAverageOverallRating();
    
    @Query("SELECT pr FROM PerformanceReview pr WHERE " +
           "(:search = '' OR LOWER(pr.employee.fullName) LIKE LOWER(CONCAT('%', :search, '%')) OR " +
           "LOWER(pr.employee.employeeId) LIKE LOWER(CONCAT('%', :search, '%'))) AND " +
           "(:department = '' OR LOWER(pr.employee.department) = LOWER(:department)) AND " +
           "(:period = '' OR LOWER(pr.reviewPeriod) LIKE LOWER(CONCAT('%', :period, '%')))")
    Page<PerformanceReview> findBySearchCriteria(@Param("search") String search,
                                                 @Param("department") String department,
                                                 @Param("period") String period,
                                                 Pageable pageable);
    
    @Query("SELECT pr FROM PerformanceReview pr WHERE pr.employee.department = :department ORDER BY pr.reviewDate DESC")
    List<PerformanceReview> findByEmployeeDepartmentOrderByReviewDateDesc(@Param("department") String department);
    
    @Query("SELECT pr FROM PerformanceReview pr WHERE pr.overallRating >= :minRating ORDER BY pr.overallRating DESC")
    List<PerformanceReview> findTopPerformers(@Param("minRating") double minRating);
}
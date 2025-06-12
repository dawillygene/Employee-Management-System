package com.employeemanagementsystem.v1.repository;

import com.employeemanagementsystem.v1.entity.Attendance;
import com.employeemanagementsystem.v1.entity.Employee;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

@Repository
public interface AttendanceRepository extends JpaRepository<Attendance, Long> {
    Optional<Attendance> findByEmployeeAndDate(Employee employee, LocalDate date);
    List<Attendance> findByEmployeeAndDateBetween(Employee employee, LocalDate startDate, LocalDate endDate);
    List<Attendance> findByDateOrderByEmployeeFullName(LocalDate date);
    
    // New methods for attendance controller
    Page<Attendance> findByDate(LocalDate date, Pageable pageable);
    boolean existsByEmployeeAndDate(Employee employee, LocalDate date);
    long countByDateAndStatus(LocalDate date, Attendance.AttendanceStatus status);
    
    // Add paginated method for employee attendance between dates
    Page<Attendance> findByEmployeeAndDateBetween(Employee employee, LocalDate startDate, LocalDate endDate, Pageable pageable);
    
    @Query("SELECT a FROM Attendance a WHERE a.date = :date AND a.status = :status")
    List<Attendance> findByDateAndStatus(@Param("date") LocalDate date, 
                                       @Param("status") Attendance.AttendanceStatus status);
    
    // Enhanced filtering methods
    @Query("SELECT a FROM Attendance a WHERE a.date = :date AND " +
           "(:status IS NULL OR a.status = :status) AND " +
           "(:search IS NULL OR " +
           "LOWER(a.employee.fullName) LIKE LOWER(CONCAT('%', :search, '%')) OR " +
           "LOWER(a.employee.employeeId) LIKE LOWER(CONCAT('%', :search, '%')))")
    Page<Attendance> findByDateAndFilters(@Param("date") LocalDate date,
                                        @Param("status") Attendance.AttendanceStatus status,
                                        @Param("search") String search,
                                        Pageable pageable);
    
    @Query("SELECT a FROM Attendance a WHERE a.date = :date AND " +
           "(:search IS NULL OR " +
           "LOWER(a.employee.fullName) LIKE LOWER(CONCAT('%', :search, '%')) OR " +
           "LOWER(a.employee.employeeId) LIKE LOWER(CONCAT('%', :search, '%')))")
    Page<Attendance> findByDateAndSearch(@Param("date") LocalDate date,
                                       @Param("search") String search,
                                       Pageable pageable);
    
    @Query("SELECT a FROM Attendance a WHERE a.date = :date AND a.status = :status")
    Page<Attendance> findByDateAndStatus(@Param("date") LocalDate date,
                                       @Param("status") Attendance.AttendanceStatus status,
                                       Pageable pageable);
    
    @Query("SELECT COUNT(a) FROM Attendance a WHERE a.employee = :employee AND a.date BETWEEN :startDate AND :endDate AND a.status = :status")
    long countByEmployeeAndDateBetweenAndStatus(@Param("employee") Employee employee,
                                              @Param("startDate") LocalDate startDate,
                                              @Param("endDate") LocalDate endDate,
                                              @Param("status") Attendance.AttendanceStatus status);
}
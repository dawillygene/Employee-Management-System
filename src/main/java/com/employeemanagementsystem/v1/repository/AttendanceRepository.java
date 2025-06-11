package com.employeemanagementsystem.v1.repository;

import com.employeemanagementsystem.v1.entity.Attendance;
import com.employeemanagementsystem.v1.entity.Employee;
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
    
    @Query("SELECT a FROM Attendance a WHERE a.date = :date AND a.status = :status")
    List<Attendance> findByDateAndStatus(@Param("date") LocalDate date, 
                                       @Param("status") Attendance.AttendanceStatus status);
    
    @Query("SELECT COUNT(a) FROM Attendance a WHERE a.employee = :employee AND a.date BETWEEN :startDate AND :endDate AND a.status = :status")
    long countByEmployeeAndDateBetweenAndStatus(@Param("employee") Employee employee,
                                              @Param("startDate") LocalDate startDate,
                                              @Param("endDate") LocalDate endDate,
                                              @Param("status") Attendance.AttendanceStatus status);
}
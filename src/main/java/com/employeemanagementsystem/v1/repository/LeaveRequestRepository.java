package com.employeemanagementsystem.v1.repository;

import com.employeemanagementsystem.v1.entity.LeaveRequest;
import com.employeemanagementsystem.v1.entity.Employee;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;

@Repository
public interface LeaveRequestRepository extends JpaRepository<LeaveRequest, Long> {
    List<LeaveRequest> findByEmployee(Employee employee);
    List<LeaveRequest> findByStatus(LeaveRequest.LeaveStatus status);
    List<LeaveRequest> findByEmployeeAndStatus(Employee employee, LeaveRequest.LeaveStatus status);
    
    @Query("SELECT lr FROM LeaveRequest lr WHERE lr.status = :status ORDER BY lr.createdAt DESC")
    List<LeaveRequest> findByStatusOrderByCreatedAtDesc(@Param("status") LeaveRequest.LeaveStatus status);
    
    @Query("SELECT lr FROM LeaveRequest lr WHERE lr.employee = :employee AND " +
           "((lr.startDate BETWEEN :startDate AND :endDate) OR " +
           "(lr.endDate BETWEEN :startDate AND :endDate) OR " +
           "(lr.startDate <= :startDate AND lr.endDate >= :endDate)) AND " +
           "lr.status IN ('APPROVED', 'PENDING')")
    List<LeaveRequest> findOverlappingLeaves(@Param("employee") Employee employee,
                                           @Param("startDate") LocalDate startDate,
                                           @Param("endDate") LocalDate endDate);
}
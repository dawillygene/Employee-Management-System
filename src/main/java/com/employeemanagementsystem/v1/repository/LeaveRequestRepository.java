package com.employeemanagementsystem.v1.repository;

import com.employeemanagementsystem.v1.entity.LeaveRequest;
import com.employeemanagementsystem.v1.entity.Employee;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
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
    
    // Find overlapping leaves for a specific employee (for new requests)
    @Query("SELECT lr FROM LeaveRequest lr WHERE lr.employee = :employee AND " +
           "((lr.startDate <= :endDate AND lr.endDate >= :startDate)) AND " +
           "lr.status IN ('APPROVED', 'PENDING')")
    List<LeaveRequest> findOverlappingLeaves(@Param("employee") Employee employee,
                                           @Param("startDate") LocalDate startDate,
                                           @Param("endDate") LocalDate endDate);
    
    // Find overlapping leaves excluding a specific request ID (for updates)
    @Query("SELECT lr FROM LeaveRequest lr WHERE lr.employee = :employee AND " +
           "lr.id != :excludeId AND " +
           "((lr.startDate <= :endDate AND lr.endDate >= :startDate)) AND " +
           "lr.status IN ('APPROVED', 'PENDING')")
    List<LeaveRequest> findOverlappingLeavesExcluding(@Param("employee") Employee employee,
                                                     @Param("startDate") LocalDate startDate,
                                                     @Param("endDate") LocalDate endDate,
                                                     @Param("excludeId") Long excludeId);
    
    // Find leave requests by employee and year for balance calculations
    @Query("SELECT lr FROM LeaveRequest lr WHERE lr.employee = :employee AND " +
           "YEAR(lr.startDate) = :year AND lr.status = 'APPROVED'")
    List<LeaveRequest> findByEmployeeAndYear(@Param("employee") Employee employee, 
                                           @Param("year") int year);
    
    // Find leave requests by employee, leave type and year
    @Query("SELECT lr FROM LeaveRequest lr WHERE lr.employee = :employee AND " +
           "lr.leaveType = :leaveType AND YEAR(lr.startDate) = :year AND lr.status = 'APPROVED'")
    List<LeaveRequest> findByEmployeeAndLeaveTypeAndYear(@Param("employee") Employee employee,
                                                       @Param("leaveType") LeaveRequest.LeaveType leaveType,
                                                       @Param("year") int year);
    
    // Pagination methods
    Page<LeaveRequest> findByStatusOrderByCreatedAtDesc(LeaveRequest.LeaveStatus status, Pageable pageable);
    Page<LeaveRequest> findByEmployeeOrderByCreatedAtDesc(Employee employee, Pageable pageable);
    Page<LeaveRequest> findByEmployeeAndStatusOrderByCreatedAtDesc(Employee employee, LeaveRequest.LeaveStatus status, Pageable pageable);
}
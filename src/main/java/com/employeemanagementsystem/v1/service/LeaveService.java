package com.employeemanagementsystem.v1.service;

import com.employeemanagementsystem.v1.entity.Employee;
import com.employeemanagementsystem.v1.entity.LeaveRequest;
import com.employeemanagementsystem.v1.repository.LeaveRequestRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

@Service
@RequiredArgsConstructor
@Transactional
public class LeaveService {
    
    private final LeaveRequestRepository leaveRequestRepository;
    
    // Annual leave allowance (can be made configurable)
    private static final int ANNUAL_LEAVE_ALLOWANCE = 25;
    private static final int SICK_LEAVE_ALLOWANCE = 10;
    private static final int PERSONAL_LEAVE_ALLOWANCE = 5;
    
    public LeaveRequest createLeaveRequest(LeaveRequest leaveRequest) {
        // Validate leave request
        validateLeaveRequest(leaveRequest);
        
        // Check for overlapping leaves
        List<LeaveRequest> overlappingLeaves = leaveRequestRepository.findOverlappingLeaves(
            leaveRequest.getEmployee(), 
            leaveRequest.getStartDate(), 
            leaveRequest.getEndDate()
        );
        
        if (!overlappingLeaves.isEmpty()) {
            throw new RuntimeException("Leave request overlaps with existing approved/pending leave");
        }
        
        // Check leave balance
        if (!hasEnoughLeaveBalance(leaveRequest)) {
            throw new RuntimeException("Insufficient leave balance for this request");
        }
        
        return leaveRequestRepository.save(leaveRequest);
    }
    
    public LeaveRequest approveLeaveRequest(Long leaveId, String approvedBy, String comments) {
        LeaveRequest leaveRequest = findById(leaveId);
        
        if (leaveRequest.getStatus() != LeaveRequest.LeaveStatus.PENDING) {
            throw new RuntimeException("Only pending leave requests can be approved");
        }
        
        leaveRequest.setStatus(LeaveRequest.LeaveStatus.APPROVED);
        leaveRequest.setApprovedBy(approvedBy);
        leaveRequest.setApprovedAt(LocalDateTime.now());
        leaveRequest.setManagerComments(comments);
        
        return leaveRequestRepository.save(leaveRequest);
    }
    
    public LeaveRequest rejectLeaveRequest(Long leaveId, String rejectedBy, String comments) {
        LeaveRequest leaveRequest = findById(leaveId);
        
        if (leaveRequest.getStatus() != LeaveRequest.LeaveStatus.PENDING) {
            throw new RuntimeException("Only pending leave requests can be rejected");
        }
        
        leaveRequest.setStatus(LeaveRequest.LeaveStatus.REJECTED);
        leaveRequest.setApprovedBy(rejectedBy);
        leaveRequest.setApprovedAt(LocalDateTime.now());
        leaveRequest.setManagerComments(comments);
        
        return leaveRequestRepository.save(leaveRequest);
    }
    
    public LeaveRequest cancelLeaveRequest(Long leaveId) {
        LeaveRequest leaveRequest = findById(leaveId);
        
        if (leaveRequest.getStatus() == LeaveRequest.LeaveStatus.CANCELLED) {
            throw new RuntimeException("Leave request is already cancelled");
        }
        
        leaveRequest.setStatus(LeaveRequest.LeaveStatus.CANCELLED);
        return leaveRequestRepository.save(leaveRequest);
    }
    
    public LeaveRequest findById(Long id) {
        return leaveRequestRepository.findById(id)
            .orElseThrow(() -> new RuntimeException("Leave request not found"));
    }
    
    public Page<LeaveRequest> findAllLeaveRequests(Pageable pageable) {
        return leaveRequestRepository.findAll(pageable);
    }
    
    public Page<LeaveRequest> findByStatusWithPagination(LeaveRequest.LeaveStatus status, Pageable pageable) {
        return leaveRequestRepository.findByStatusOrderByCreatedAtDesc(status, pageable);
    }
    
    public Page<LeaveRequest> findByEmployeeWithPagination(Employee employee, Pageable pageable) {
        return leaveRequestRepository.findByEmployeeOrderByCreatedAtDesc(employee, pageable);
    }
    
    public Page<LeaveRequest> findByEmployeeAndStatusWithPagination(Employee employee, LeaveRequest.LeaveStatus status, Pageable pageable) {
        return leaveRequestRepository.findByEmployeeAndStatusOrderByCreatedAtDesc(employee, status, pageable);
    }
    
    public List<LeaveRequest> findByEmployee(Employee employee) {
        return leaveRequestRepository.findByEmployee(employee);
    }
    
    public List<LeaveRequest> findPendingRequests() {
        return leaveRequestRepository.findByStatusOrderByCreatedAtDesc(LeaveRequest.LeaveStatus.PENDING);
    }
    
    public List<LeaveRequest> findByStatus(LeaveRequest.LeaveStatus status) {
        return leaveRequestRepository.findByStatus(status);
    }
    
    public List<LeaveRequest> findLeaveRequestsForMonth(int year, int month) {
        LocalDate startOfMonth = LocalDate.of(year, month, 1);
        LocalDate endOfMonth = startOfMonth.withDayOfMonth(startOfMonth.lengthOfMonth());
        
        return leaveRequestRepository.findAll()
            .stream()
            .filter(leave -> leave.getStatus() == LeaveRequest.LeaveStatus.APPROVED || 
                           leave.getStatus() == LeaveRequest.LeaveStatus.PENDING)
            .filter(leave -> !leave.getStartDate().isAfter(endOfMonth) && 
                           !leave.getEndDate().isBefore(startOfMonth))
            .toList();
    }
    
    // Leave balance calculations
    public int getLeaveBalance(Employee employee, LeaveRequest.LeaveType leaveType) {
        int currentYear = LocalDate.now().getYear();
        LocalDate startOfYear = LocalDate.of(currentYear, 1, 1);
        LocalDate endOfYear = LocalDate.of(currentYear, 12, 31);
        
        List<LeaveRequest> approvedLeaves = leaveRequestRepository.findByEmployee(employee)
            .stream()
            .filter(leave -> leave.getStatus() == LeaveRequest.LeaveStatus.APPROVED)
            .filter(leave -> leave.getLeaveType() == leaveType)
            .filter(leave -> !leave.getStartDate().isAfter(endOfYear) && !leave.getEndDate().isBefore(startOfYear))
            .toList();
        
        long usedDays = approvedLeaves.stream()
            .mapToLong(LeaveRequest::getLeaveDuration)
            .sum();
        
        int allowance = getAllowanceForLeaveType(leaveType);
        return allowance - (int) usedDays;
    }
    
    public int getTotalLeaveBalance(Employee employee) {
        int annualBalance = getLeaveBalance(employee, LeaveRequest.LeaveType.VACATION);
        int sickBalance = getLeaveBalance(employee, LeaveRequest.LeaveType.SICK_LEAVE);
        int personalBalance = getLeaveBalance(employee, LeaveRequest.LeaveType.PERSONAL);
        
        return annualBalance + sickBalance + personalBalance;
    }
    
    public long getUsedLeaveThisYear(Employee employee) {
        int currentYear = LocalDate.now().getYear();
        LocalDate startOfYear = LocalDate.of(currentYear, 1, 1);
        LocalDate endOfYear = LocalDate.of(currentYear, 12, 31);
        
        return leaveRequestRepository.findByEmployee(employee)
            .stream()
            .filter(leave -> leave.getStatus() == LeaveRequest.LeaveStatus.APPROVED)
            .filter(leave -> !leave.getStartDate().isAfter(endOfYear) && !leave.getEndDate().isBefore(startOfYear))
            .mapToLong(LeaveRequest::getLeaveDuration)
            .sum();
    }
    
    private void validateLeaveRequest(LeaveRequest leaveRequest) {
        if (leaveRequest.getStartDate().isAfter(leaveRequest.getEndDate())) {
            throw new RuntimeException("Start date cannot be after end date");
        }
        
        if (leaveRequest.getStartDate().isBefore(LocalDate.now())) {
            throw new RuntimeException("Cannot apply for leave in the past");
        }
        
        if (leaveRequest.getLeaveDuration() > 90) {
            throw new RuntimeException("Leave duration cannot exceed 90 days");
        }
        
        if (leaveRequest.getReason() == null || leaveRequest.getReason().trim().isEmpty()) {
            throw new RuntimeException("Leave reason is required");
        }
    }
    
    private boolean hasEnoughLeaveBalance(LeaveRequest leaveRequest) {
        // For emergency leave, always allow (emergency situations)
        if (leaveRequest.getLeaveType() == LeaveRequest.LeaveType.EMERGENCY) {
            return true;
        }
        
        // For maternity/paternity leave, always allow (legal requirement)
        if (leaveRequest.getLeaveType() == LeaveRequest.LeaveType.MATERNITY || 
            leaveRequest.getLeaveType() == LeaveRequest.LeaveType.PATERNITY) {
            return true;
        }
        
        int availableBalance = getLeaveBalance(leaveRequest.getEmployee(), leaveRequest.getLeaveType());
        long requestedDays = leaveRequest.getLeaveDuration();
        
        // Allow if balance is sufficient OR if it's a minor overrun (1-2 days) for vacation/personal
        if (leaveRequest.getLeaveType() == LeaveRequest.LeaveType.VACATION || 
            leaveRequest.getLeaveType() == LeaveRequest.LeaveType.PERSONAL) {
            return availableBalance >= (requestedDays - 2); // Allow 2 days overrun
        }
        
        return availableBalance >= requestedDays;
    }
    
    private int getAllowanceForLeaveType(LeaveRequest.LeaveType leaveType) {
        return switch (leaveType) {
            case VACATION -> ANNUAL_LEAVE_ALLOWANCE;
            case SICK_LEAVE -> SICK_LEAVE_ALLOWANCE;
            case PERSONAL -> PERSONAL_LEAVE_ALLOWANCE;
            case MATERNITY, PATERNITY -> 90; // 3 months
            case EMERGENCY -> 3; // Emergency leave
        };
    }
}
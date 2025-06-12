package com.employeemanagementsystem.v1.controller;

import com.employeemanagementsystem.v1.entity.Employee;
import com.employeemanagementsystem.v1.entity.LeaveRequest;
import com.employeemanagementsystem.v1.service.EmployeeService;
import com.employeemanagementsystem.v1.service.LeaveService;
import com.employeemanagementsystem.v1.service.CurrentUserService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import jakarta.validation.Valid;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@Controller
@RequestMapping("/leave")
@RequiredArgsConstructor
public class LeaveController {
    
    private final LeaveService leaveService;
    private final EmployeeService employeeService;
    private final CurrentUserService currentUserService;
    
    @GetMapping
    public String listLeaveRequests(@RequestParam(defaultValue = "") String status,
                                   @RequestParam(defaultValue = "0") int page,
                                   @RequestParam(defaultValue = "5") int size,
                                   Model model) {
        
        Pageable pageable = PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "createdAt"));
        
        // Check user role and filter accordingly
        boolean isAdminOrHR = currentUserService.hasRole("ADMIN") || currentUserService.hasRole("HR");
        Page<LeaveRequest> leaveRequestsPage;
        
        if (isAdminOrHR) {
            // Admin and HR can see all leave requests with pagination
            if (!status.isEmpty()) {
                try {
                    LeaveRequest.LeaveStatus statusEnum = LeaveRequest.LeaveStatus.valueOf(status.toUpperCase());
                    leaveRequestsPage = leaveService.findByStatusWithPagination(statusEnum, pageable);
                } catch (IllegalArgumentException e) {
                    leaveRequestsPage = leaveService.findAllLeaveRequests(pageable);
                }
            } else {
                leaveRequestsPage = leaveService.findAllLeaveRequests(pageable);
            }
        } else {
            // Regular employees can only see their own requests with pagination
            Optional<Employee> currentEmployeeOpt = currentUserService.getCurrentEmployee();
            if (currentEmployeeOpt.isPresent()) {
                if (!status.isEmpty()) {
                    try {
                        LeaveRequest.LeaveStatus statusEnum = LeaveRequest.LeaveStatus.valueOf(status.toUpperCase());
                        leaveRequestsPage = leaveService.findByEmployeeAndStatusWithPagination(currentEmployeeOpt.get(), statusEnum, pageable);
                    } catch (IllegalArgumentException e) {
                        leaveRequestsPage = leaveService.findByEmployeeWithPagination(currentEmployeeOpt.get(), pageable);
                    }
                } else {
                    leaveRequestsPage = leaveService.findByEmployeeWithPagination(currentEmployeeOpt.get(), pageable);
                }
            } else {
                leaveRequestsPage = Page.empty(pageable);
            }
        }
        
        model.addAttribute("leaveRequests", leaveRequestsPage.getContent());
        model.addAttribute("currentPage", page);
        model.addAttribute("totalPages", leaveRequestsPage.getTotalPages());
        model.addAttribute("totalElements", leaveRequestsPage.getTotalElements());
        model.addAttribute("size", size);
        model.addAttribute("status", status);
        
        // Statistics - also role-based
        if (isAdminOrHR) {
            List<LeaveRequest> pendingRequests = leaveService.findPendingRequests();
            List<LeaveRequest> approvedRequests = leaveService.findByStatus(LeaveRequest.LeaveStatus.APPROVED);
            List<LeaveRequest> rejectedRequests = leaveService.findByStatus(LeaveRequest.LeaveStatus.REJECTED);
            
            model.addAttribute("pendingCount", pendingRequests.size());
            model.addAttribute("approvedCount", approvedRequests.size());
            model.addAttribute("rejectedCount", rejectedRequests.size());
            model.addAttribute("pageTitle", "Leave Management");
            model.addAttribute("pageDescription", "Manage employee leave requests and approvals");
        } else {
            // For employees, show their own statistics
            Optional<Employee> currentEmployeeOpt = currentUserService.getCurrentEmployee();
            if (currentEmployeeOpt.isPresent()) {
                List<LeaveRequest> myRequests = leaveService.findByEmployee(currentEmployeeOpt.get());
                long pendingCount = myRequests.stream().filter(r -> r.getStatus() == LeaveRequest.LeaveStatus.PENDING).count();
                long approvedCount = myRequests.stream().filter(r -> r.getStatus() == LeaveRequest.LeaveStatus.APPROVED).count();
                long rejectedCount = myRequests.stream().filter(r -> r.getStatus() == LeaveRequest.LeaveStatus.REJECTED).count();
                
                model.addAttribute("pendingCount", pendingCount);
                model.addAttribute("approvedCount", approvedCount);
                model.addAttribute("rejectedCount", rejectedCount);
            } else {
                model.addAttribute("pendingCount", 0);
                model.addAttribute("approvedCount", 0);
                model.addAttribute("rejectedCount", 0);
            }
            model.addAttribute("pageTitle", "My Leave Requests");
            model.addAttribute("pageDescription", "View and manage your leave requests");
        }
        
        model.addAttribute("activePage", "leave");
        model.addAttribute("selectedStatus", status);
        model.addAttribute("isAdminOrHR", isAdminOrHR);
        
        return "leave/list";
    }
    
    @GetMapping("/my-requests")
    public String myLeaveRequests(@RequestParam(defaultValue = "0") int page,
                                 @RequestParam(defaultValue = "3") int size,
                                 Authentication authentication, 
                                 Model model) {
        // Get the current logged-in employee
        Optional<Employee> currentEmployeeOpt = currentUserService.getCurrentEmployee();
        
        if (currentEmployeeOpt.isEmpty()) {
            model.addAttribute("error", "No employee record found for current user. Please contact HR.");
            return "error/403";
        }
        
        Employee currentEmployee = currentEmployeeOpt.get();
        
        // Get paginated requests for the current employee only
        List<LeaveRequest> allMyRequests = leaveService.findByEmployee(currentEmployee);
        
        // Create pagination manually since we need to filter by employee first
        Pageable pageable = PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "createdAt"));
        int start = (int) pageable.getOffset();
        int end = Math.min((start + pageable.getPageSize()), allMyRequests.size());
        
        List<LeaveRequest> pageContent = allMyRequests.subList(start, end);
        int totalElements = allMyRequests.size();
        int totalPages = (int) Math.ceil((double) totalElements / size);
        
        // Calculate leave balances
        int vacationBalance = leaveService.getLeaveBalance(currentEmployee, LeaveRequest.LeaveType.VACATION);
        int sickBalance = leaveService.getLeaveBalance(currentEmployee, LeaveRequest.LeaveType.SICK_LEAVE);
        int personalBalance = leaveService.getLeaveBalance(currentEmployee, LeaveRequest.LeaveType.PERSONAL);
        long usedLeave = leaveService.getUsedLeaveThisYear(currentEmployee);
        
        model.addAttribute("myRequests", pageContent);
        model.addAttribute("currentEmployee", currentEmployee);
        model.addAttribute("vacationBalance", vacationBalance);
        model.addAttribute("sickBalance", sickBalance);
        model.addAttribute("personalBalance", personalBalance);
        model.addAttribute("usedLeave", usedLeave);
        
        // Pagination attributes
        model.addAttribute("currentPage", page);
        model.addAttribute("totalPages", totalPages);
        model.addAttribute("totalElements", totalElements);
        model.addAttribute("pageSize", size);
        model.addAttribute("hasPrevious", page > 0);
        model.addAttribute("hasNext", page < totalPages - 1);
        
        model.addAttribute("pageTitle", "My Leave Requests");
        model.addAttribute("pageDescription", "View and manage your leave requests");
        model.addAttribute("activePage", "leave");
        
        return "leave/my-requests";
    }
    
    @GetMapping("/new")
    public String showLeaveRequestForm(Model model, Authentication authentication) {
        LeaveRequest leaveRequest = new LeaveRequest();
        
        // Check if current user is an employee
        boolean isEmployee = currentUserService.hasRole("EMPLOYEE");
        
        if (isEmployee) {
            // For employees, automatically set their employee record
            Optional<Employee> currentEmployeeOpt = currentUserService.getCurrentEmployee();
            if (currentEmployeeOpt.isEmpty()) {
                model.addAttribute("error", "No employee record found for current user. Please contact HR.");
                return "error/403";
            }
            
            Employee currentEmployee = currentEmployeeOpt.get();
            leaveRequest.setEmployee(currentEmployee);
            model.addAttribute("currentEmployee", currentEmployee);
            model.addAttribute("isEmployee", true);
        } else {
            // For HR/Admin, show all employees
            model.addAttribute("employees", employeeService.getAllActiveEmployees());
            model.addAttribute("isEmployee", false);
        }
        
        model.addAttribute("pageTitle", "New Leave Request");
        model.addAttribute("pageDescription", "Submit a new leave request");
        model.addAttribute("activePage", "leave");
        model.addAttribute("leaveRequest", leaveRequest);
        model.addAttribute("leaveTypes", LeaveRequest.LeaveType.values());
        
        return "leave/form";
    }
    
    @PostMapping("/new")
    public String submitLeaveRequest(@Valid @ModelAttribute LeaveRequest leaveRequest,
                                   BindingResult result,
                                   RedirectAttributes redirectAttributes,
                                   Model model,
                                   Authentication authentication,
                                   @RequestParam(value = "submitAnother", required = false) String submitAnother) {
        
        if (result.hasErrors()) {
            // Reload form data based on user role
            boolean isEmployee = currentUserService.hasRole("EMPLOYEE");
            
            if (isEmployee) {
                Optional<Employee> currentEmployeeOpt = currentUserService.getCurrentEmployee();
                if (currentEmployeeOpt.isPresent()) {
                    model.addAttribute("currentEmployee", currentEmployeeOpt.get());
                    model.addAttribute("isEmployee", true);
                }
            } else {
                model.addAttribute("employees", employeeService.getAllActiveEmployees());
                model.addAttribute("isEmployee", false);
            }
            
            model.addAttribute("pageTitle", "New Leave Request");
            model.addAttribute("pageDescription", "Submit a new leave request");
            model.addAttribute("activePage", "leave");
            model.addAttribute("leaveTypes", LeaveRequest.LeaveType.values());
            return "leave/form";
        }
        
        try {
            LeaveRequest savedRequest = leaveService.createLeaveRequest(leaveRequest);
            
            // Check if user wants to submit another request
            if ("true".equals(submitAnother)) {
                redirectAttributes.addFlashAttribute("success", 
                    "Leave request submitted successfully! Request ID: " + savedRequest.getId() + 
                    ". You can submit another request below.");
                return "redirect:/leave/new";
            } else {
                // Redirect employees to their requests page, admins to all requests
                boolean isEmployee = currentUserService.hasRole("EMPLOYEE");
                if (isEmployee) {
                    redirectAttributes.addFlashAttribute("success", 
                        "Leave request submitted successfully! Request ID: " + savedRequest.getId() + 
                        ". Your request is pending approval and you will be notified once reviewed.");
                    return "redirect:/leave/my-requests";
                } else {
                    redirectAttributes.addFlashAttribute("success", 
                        "Leave request submitted successfully! Request ID: " + savedRequest.getId());
                    return "redirect:/leave";
                }
            }
        } catch (RuntimeException e) {
            // Reload form data based on user role for error case
            boolean isEmployee = currentUserService.hasRole("EMPLOYEE");
            
            if (isEmployee) {
                Optional<Employee> currentEmployeeOpt = currentUserService.getCurrentEmployee();
                if (currentEmployeeOpt.isPresent()) {
                    model.addAttribute("currentEmployee", currentEmployeeOpt.get());
                    model.addAttribute("isEmployee", true);
                }
            } else {
                model.addAttribute("employees", employeeService.getAllActiveEmployees());
                model.addAttribute("isEmployee", false);
            }
            
            model.addAttribute("error", e.getMessage());
            model.addAttribute("pageTitle", "New Leave Request");
            model.addAttribute("pageDescription", "Submit a new leave request");
            model.addAttribute("activePage", "leave");
            model.addAttribute("leaveTypes", LeaveRequest.LeaveType.values());
            return "leave/form";
        }
    }
    
    @GetMapping("/view/{id}")
    public String viewLeaveRequest(@PathVariable Long id, Authentication authentication, Model model) {
        LeaveRequest leaveRequest = leaveService.findById(id);
        
        // Check if current user can approve/reject (Admin or HR roles)
        boolean canApproveReject = authentication.getAuthorities().stream()
                .anyMatch(auth -> auth.getAuthority().equals("ROLE_ADMIN") || auth.getAuthority().equals("ROLE_HR"));
        
        model.addAttribute("pageTitle", "Leave Request Details");
        model.addAttribute("pageDescription", "View leave request information");
        model.addAttribute("activePage", "leave");
        model.addAttribute("leaveRequest", leaveRequest);
        model.addAttribute("canApproveReject", canApproveReject);
        
        return "leave/view";
    }
    
    @PostMapping("/approve/{id}")
    @PreAuthorize("hasRole('ADMIN') or hasRole('HR')")
    public String approveLeaveRequest(@PathVariable Long id,
                                    @RequestParam(required = false) String comments,
                                    Authentication authentication,
                                    RedirectAttributes redirectAttributes) {
        try {
            String approvedBy = authentication.getName();
            leaveService.approveLeaveRequest(id, approvedBy, comments);
            redirectAttributes.addFlashAttribute("success", "Leave request approved successfully!");
        } catch (RuntimeException e) {
            redirectAttributes.addFlashAttribute("error", "Error approving leave request: " + e.getMessage());
        }
        
        return "redirect:/leave/view/" + id;
    }
    
    @PostMapping("/reject/{id}")
    @PreAuthorize("hasRole('ADMIN') or hasRole('HR')")
    public String rejectLeaveRequest(@PathVariable Long id,
                                   @RequestParam(required = false) String comments,
                                   Authentication authentication,
                                   RedirectAttributes redirectAttributes) {
        try {
            String rejectedBy = authentication.getName();
            leaveService.rejectLeaveRequest(id, rejectedBy, comments);
            redirectAttributes.addFlashAttribute("success", "Leave request rejected.");
        } catch (RuntimeException e) {
            redirectAttributes.addFlashAttribute("error", "Error rejecting leave request: " + e.getMessage());
        }
        
        return "redirect:/leave/view/" + id;
    }
    
    @PostMapping("/cancel/{id}")
    public String cancelLeaveRequest(@PathVariable Long id,
                                   RedirectAttributes redirectAttributes) {
        try {
            leaveService.cancelLeaveRequest(id);
            redirectAttributes.addFlashAttribute("success", "Leave request cancelled successfully!");
        } catch (RuntimeException e) {
            redirectAttributes.addFlashAttribute("error", "Error cancelling leave request: " + e.getMessage());
        }
        
        return "redirect:/leave/my-requests";
    }
    
    @GetMapping("/calendar")
    public String leaveCalendar(Model model) {
        // Get departments for filter dropdown
        List<String> departments = employeeService.getAllDepartments();
        
        model.addAttribute("pageTitle", "Leave Calendar");
        model.addAttribute("pageDescription", "View team leave calendar");
        model.addAttribute("activePage", "leave-calendar");
        model.addAttribute("departments", departments);
        
        return "leave/calendar";
    }
    
    @GetMapping("/calendar/data")
    @ResponseBody
    public List<Object> getCalendarData(@RequestParam int month, @RequestParam int year,
                                       @RequestParam(required = false) String department) {
        // Get all approved and pending leave requests for the specified month/year
        List<LeaveRequest> leaveRequests = leaveService.findLeaveRequestsForMonth(year, month);
        
        // Role-based filtering: EMPLOYEE role only sees their own leave requests
        if (currentUserService.hasRole("EMPLOYEE")) {
            Optional<Employee> currentEmployee = currentUserService.getCurrentEmployee();
            if (currentEmployee.isPresent()) {
                leaveRequests = leaveRequests.stream()
                    .filter(leave -> leave.getEmployee().getId().equals(currentEmployee.get().getId()))
                    .collect(java.util.stream.Collectors.toList());
            } else {
                // If no employee record found, return empty list
                leaveRequests = java.util.Collections.emptyList();
            }
        } else {
            // For ADMIN and HR roles, filter by department if specified
            if (department != null && !department.isEmpty()) {
                leaveRequests = leaveRequests.stream()
                    .filter(leave -> leave.getEmployee().getDepartment().equals(department))
                    .collect(java.util.stream.Collectors.toList());
            }
        }
        
        // Convert to JSON-friendly format for the calendar
        return leaveRequests.stream()
            .map(leave -> new Object() {
                public final Long id = leave.getId();
                public final String employeeName = leave.getEmployee().getFullName();
                public final String leaveType = leave.getLeaveType().toString().replace('_', ' ');
                public final String startDate = leave.getStartDate().toString();
                public final String endDate = leave.getEndDate().toString();
                public final String status = leave.getStatus().toString();
                public final long duration = leave.getLeaveDuration();
                public final String reason = leave.getReason();
            })
            .collect(java.util.stream.Collectors.toList());
    }
    
    @GetMapping("/{id}")
    @ResponseBody
    public Object getLeaveDetails(@PathVariable Long id) {
        try {
            LeaveRequest leave = leaveService.findById(id);
            
            return new Object() {
                public final Long id = leave.getId();
                public final String employeeName = leave.getEmployee().getFullName();
                public final String leaveType = leave.getLeaveType().toString().replace('_', ' ');
                public final String startDate = leave.getStartDate().toString();
                public final String endDate = leave.getEndDate().toString();
                public final String status = leave.getStatus().toString();
                public final long duration = leave.getLeaveDuration();
                public final String reason = leave.getReason();
                public final String appliedDate = leave.getCreatedAt().toLocalDate().toString();
                public final String comments = leave.getManagerComments() != null ? leave.getManagerComments() : "";
            };
        } catch (Exception e) {
            return new Object() {
                public final String error = e.getMessage();
            };
        }
    }
}
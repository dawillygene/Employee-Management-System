package com.employeemanagementsystem.v1.controller;

import com.employeemanagementsystem.v1.entity.PerformanceReview;
import com.employeemanagementsystem.v1.service.EmployeeService;
import com.employeemanagementsystem.v1.repository.PerformanceReviewRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.time.LocalDate;

@Controller
@RequestMapping("/performance")
@RequiredArgsConstructor
public class PerformanceController {
    
    private final PerformanceReviewRepository performanceReviewRepository;
    private final EmployeeService employeeService;
    
    @GetMapping
    public String listReviews(@RequestParam(defaultValue = "") String search,
                             @RequestParam(defaultValue = "") String department,
                             @RequestParam(defaultValue = "") String period,
                             @RequestParam(defaultValue = "0") int page,
                             @RequestParam(defaultValue = "10") int size,
                             Model model) {
        
        Pageable pageable = PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "reviewDate"));
        
        // Get performance reviews with filters
        Page<PerformanceReview> reviews;
        if (search.isEmpty() && department.isEmpty() && period.isEmpty()) {
            reviews = performanceReviewRepository.findAll(pageable);
        } else {
            reviews = performanceReviewRepository.findBySearchCriteria(search, department, period, pageable);
        }
        
        model.addAttribute("pageTitle", "Performance Reviews");
        model.addAttribute("pageDescription", "Manage employee performance evaluations and reviews");
        model.addAttribute("activePage", "performance");
        model.addAttribute("reviews", reviews);
        model.addAttribute("employees", employeeService.getAllActiveEmployees());
        model.addAttribute("departments", employeeService.getAllDepartments());
        model.addAttribute("search", search);
        model.addAttribute("selectedDepartment", department);
        model.addAttribute("selectedPeriod", period);
        
        // Calculate statistics with null safety
        long totalReviews = performanceReviewRepository.count();
        long excellentPerformers = performanceReviewRepository.countByOverallRatingGreaterThanEqual(4.5);
        long needImprovement = performanceReviewRepository.countByOverallRatingLessThan(3.0);
        long pendingReviews = performanceReviewRepository.countByStatus(PerformanceReview.ReviewStatus.DRAFT);
        
        // Handle null average rating safely
        Double averageRatingFromDb = performanceReviewRepository.findAverageOverallRating();
        double averageRating = (averageRatingFromDb != null) ? averageRatingFromDb : 0.0;
        
        model.addAttribute("totalReviews", totalReviews);
        model.addAttribute("excellentPerformers", excellentPerformers);
        model.addAttribute("needImprovement", needImprovement);
        model.addAttribute("pendingReviews", pendingReviews);
        model.addAttribute("averageRating", Math.round(averageRating * 10.0) / 10.0);
        
        return "performance/list";
    }
    
    @GetMapping("/new")
    public String showCreateForm(Model model) {
        model.addAttribute("pageTitle", "New Performance Review");
        model.addAttribute("pageDescription", "Create a new performance evaluation");
        model.addAttribute("activePage", "performance");
        model.addAttribute("review", new PerformanceReview());
        model.addAttribute("employees", employeeService.getAllActiveEmployees());
        return "performance/form";
    }
    
    @PostMapping("/new")
    public String createReview(@ModelAttribute("review") PerformanceReview review,
                              BindingResult bindingResult,
                              RedirectAttributes redirectAttributes,
                              Model model) {
        
        // Set required fields BEFORE validation
        review.setReviewedBy("HR Manager");
        
        // Calculate overall rating BEFORE validation
        if (review.getTechnicalSkills() != null && review.getCommunication() != null &&
            review.getLeadership() != null && review.getPunctuality() != null &&
            review.getTeamwork() != null && review.getProblemSolving() != null) {
            double overallRating = calculateOverallRating(review);
            review.setOverallRating(overallRating);
        }
        
        // Now perform manual validation
        validatePerformanceReview(review, bindingResult);
        
        System.out.println("=== DEBUG: Form submission received ===");
        System.out.println("Review employee ID: " + (review.getEmployee() != null ? review.getEmployee().getId() : "NULL"));
        System.out.println("Review period: " + review.getReviewPeriod());
        System.out.println("Technical skills: " + review.getTechnicalSkills());
        System.out.println("Overall rating: " + review.getOverallRating());
        System.out.println("Reviewed by: " + review.getReviewedBy());
        System.out.println("Has binding errors: " + bindingResult.hasErrors());
        
        if (bindingResult.hasErrors()) {
            System.out.println("=== VALIDATION ERRORS ===");
            bindingResult.getAllErrors().forEach(error -> {
                System.out.println("Error: " + error.getDefaultMessage());
            });
            
            model.addAttribute("pageTitle", "New Performance Review");
            model.addAttribute("pageDescription", "Create a new performance evaluation");
            model.addAttribute("activePage", "performance");
            model.addAttribute("employees", employeeService.getAllActiveEmployees());
            return "performance/form";
        }
        
        try {
            // Set final fields
            review.setReviewDate(LocalDate.now());
            review.setStatus(PerformanceReview.ReviewStatus.COMPLETED);
            
            // Check if review already exists for this employee and date
            if (performanceReviewRepository.existsByEmployeeAndReviewDate(review.getEmployee(), review.getReviewDate())) {
                bindingResult.rejectValue("employee", "duplicate.date", 
                    "A performance review already exists for this employee on " + review.getReviewDate() + 
                    ". Only one review per employee per date is allowed.");
                model.addAttribute("pageTitle", "New Performance Review");
                model.addAttribute("pageDescription", "Create a new performance evaluation");
                model.addAttribute("activePage", "performance");
                model.addAttribute("employees", employeeService.getAllActiveEmployees());
                return "performance/form";
            }
            
            System.out.println("=== Saving review ===");
            PerformanceReview savedReview = performanceReviewRepository.save(review);
            System.out.println("Saved review ID: " + savedReview.getId());
            
            redirectAttributes.addFlashAttribute("success", 
                "Performance review created successfully for " + review.getEmployee().getFullName());
            
            System.out.println("=== Redirecting to /performance ===");
            
        } catch (org.springframework.dao.DataIntegrityViolationException e) {
            // Handle database constraint violation
            if (e.getMessage().contains("uk_employee_review_date")) {
                bindingResult.rejectValue("employee", "duplicate.date", 
                    "A performance review already exists for this employee on this date. Only one review per employee per date is allowed.");
            } else {
                bindingResult.rejectValue("employee", "database.error", "Database constraint violation: " + e.getMessage());
            }
            model.addAttribute("pageTitle", "New Performance Review");
            model.addAttribute("pageDescription", "Create a new performance evaluation");
            model.addAttribute("activePage", "performance");
            model.addAttribute("employees", employeeService.getAllActiveEmployees());
            return "performance/form";
        } catch (Exception e) {
            System.out.println("=== ERROR OCCURRED ===");
            e.printStackTrace();
            redirectAttributes.addFlashAttribute("error", "Error creating performance review: " + e.getMessage());
        }
        
        return "redirect:/performance";
    }
    
    private void validatePerformanceReview(PerformanceReview review, BindingResult bindingResult) {
        if (review.getEmployee() == null) {
            bindingResult.rejectValue("employee", "required", "Employee is required");
        }
        
        if (review.getReviewPeriod() == null || review.getReviewPeriod().trim().isEmpty()) {
            bindingResult.rejectValue("reviewPeriod", "required", "Review period is required");
        }
        
        if (review.getTechnicalSkills() == null || review.getTechnicalSkills() < 1.0 || review.getTechnicalSkills() > 5.0) {
            bindingResult.rejectValue("technicalSkills", "invalid", "Technical skills rating must be between 1.0 and 5.0");
        }
        
        if (review.getCommunication() == null || review.getCommunication() < 1.0 || review.getCommunication() > 5.0) {
            bindingResult.rejectValue("communication", "invalid", "Communication rating must be between 1.0 and 5.0");
        }
        
        if (review.getLeadership() == null || review.getLeadership() < 1.0 || review.getLeadership() > 5.0) {
            bindingResult.rejectValue("leadership", "invalid", "Leadership rating must be between 1.0 and 5.0");
        }
        
        if (review.getPunctuality() == null || review.getPunctuality() < 1.0 || review.getPunctuality() > 5.0) {
            bindingResult.rejectValue("punctuality", "invalid", "Punctuality rating must be between 1.0 and 5.0");
        }
        
        if (review.getTeamwork() == null || review.getTeamwork() < 1.0 || review.getTeamwork() > 5.0) {
            bindingResult.rejectValue("teamwork", "invalid", "Teamwork rating must be between 1.0 and 5.0");
        }
        
        if (review.getProblemSolving() == null || review.getProblemSolving() < 1.0 || review.getProblemSolving() > 5.0) {
            bindingResult.rejectValue("problemSolving", "invalid", "Problem solving rating must be between 1.0 and 5.0");
        }
        
        if (review.getManagerComments() == null || review.getManagerComments().trim().isEmpty()) {
            bindingResult.rejectValue("managerComments", "required", "Manager comments are required");
        }
    }
    
    @GetMapping("/edit/{id}")
    public String showEditForm(@PathVariable Long id, Model model) {
        PerformanceReview review = performanceReviewRepository.findById(id)
            .orElseThrow(() -> new RuntimeException("Performance review not found"));
        
        model.addAttribute("pageTitle", "Edit Performance Review");
        model.addAttribute("pageDescription", "Update performance evaluation");
        model.addAttribute("activePage", "performance");
        model.addAttribute("review", review);
        model.addAttribute("employees", employeeService.getAllActiveEmployees());
        model.addAttribute("isEdit", true);
        
        return "performance/form";
    }
    
    @PostMapping("/edit/{id}")
    public String updateReview(@PathVariable Long id,
                              @ModelAttribute("review") PerformanceReview review,
                              BindingResult bindingResult,
                              RedirectAttributes redirectAttributes,
                              Model model) {
        
        if (bindingResult.hasErrors()) {
            model.addAttribute("pageTitle", "Edit Performance Review");
            model.addAttribute("pageDescription", "Update performance evaluation");
            model.addAttribute("activePage", "performance");
            model.addAttribute("employees", employeeService.getAllActiveEmployees());
            model.addAttribute("isEdit", true);
            return "performance/form";
        }
        
        try {
            PerformanceReview existingReview = performanceReviewRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Performance review not found"));
            
            // Update fields
            existingReview.setEmployee(review.getEmployee());
            existingReview.setReviewPeriod(review.getReviewPeriod());
            existingReview.setTechnicalSkills(review.getTechnicalSkills());
            existingReview.setCommunication(review.getCommunication());
            existingReview.setLeadership(review.getLeadership());
            existingReview.setPunctuality(review.getPunctuality());
            existingReview.setTeamwork(review.getTeamwork());
            existingReview.setProblemSolving(review.getProblemSolving());
            existingReview.setManagerComments(review.getManagerComments());
            existingReview.setGoalsForNextPeriod(review.getGoalsForNextPeriod());
            
            // Recalculate overall rating
            double overallRating = calculateOverallRating(existingReview);
            existingReview.setOverallRating(overallRating);
            
            performanceReviewRepository.save(existingReview);
            redirectAttributes.addFlashAttribute("success", 
                "Performance review updated successfully for " + existingReview.getEmployee().getFullName());
            
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("error", "Error updating performance review: " + e.getMessage());
        }
        
        return "redirect:/performance";
    }
    
    @GetMapping("/view/{id}")
    public String viewReview(@PathVariable Long id, Model model) {
        PerformanceReview review = performanceReviewRepository.findById(id)
            .orElseThrow(() -> new RuntimeException("Performance review not found"));
        
        model.addAttribute("pageTitle", "Performance Review Details");
        model.addAttribute("pageDescription", "View performance evaluation details");
        model.addAttribute("activePage", "performance");
        model.addAttribute("review", review);
        
        return "performance/view";
    }
    
    @PostMapping("/delete/{id}")
    public String deleteReview(@PathVariable Long id, RedirectAttributes redirectAttributes) {
        try {
            PerformanceReview review = performanceReviewRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Performance review not found"));
            
            String employeeName = review.getEmployee().getFullName();
            performanceReviewRepository.deleteById(id);
            
            redirectAttributes.addFlashAttribute("success", 
                "Performance review deleted successfully for " + employeeName);
            
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("error", "Error deleting performance review: " + e.getMessage());
        }
        
        return "redirect:/performance";
    }
    
    private double calculateOverallRating(PerformanceReview review) {
        double total = review.getTechnicalSkills() + review.getCommunication() + 
                      review.getLeadership() + review.getPunctuality() + 
                      review.getTeamwork() + review.getProblemSolving();
        return Math.round((total / 6.0) * 10.0) / 10.0;
    }
}
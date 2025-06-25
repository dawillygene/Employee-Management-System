package com.employeemanagementsystem.v1.controller;

import com.employeemanagementsystem.v1.entity.User;
import com.employeemanagementsystem.v1.service.UserService;
import com.employeemanagementsystem.v1.service.EmployeeService;
import com.employeemanagementsystem.v1.service.CurrentUserService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import jakarta.validation.Valid;
import java.util.List;

@Controller
@RequestMapping("/admin")
@RequiredArgsConstructor
@PreAuthorize("hasRole('ADMIN')")
public class AdminController {

    private final UserService userService;
    private final EmployeeService employeeService;
    private final CurrentUserService currentUserService;

    /**
     * Admin Dashboard - Overview of system statistics
     */
    @GetMapping({ "", "/dashboard" })
    public String adminDashboard(Model model) {
        model.addAttribute("pageTitle", "Admin Dashboard");
        model.addAttribute("pageDescription", "System administration and user management");
        model.addAttribute("activePage", "admin-dashboard");

        // System Statistics
        model.addAttribute("totalUsers", userService.getTotalUsers());
        model.addAttribute("totalEmployees", employeeService.getTotalEmployees());
        model.addAttribute("adminCount", userService.getUsersCountByRole(User.Role.ADMIN));
        model.addAttribute("hrCount", userService.getUsersCountByRole(User.Role.HR));
        model.addAttribute("employeeCount", userService.getUsersCountByRole(User.Role.EMPLOYEE));
        model.addAttribute("activeUsers", userService.getActiveUsers().size());
        model.addAttribute("inactiveUsers", userService.getInactiveUsers().size());

        // Recent Users (last 5)
        Pageable recentUsersPageable = PageRequest.of(0, 5, Sort.by(Sort.Direction.DESC, "id"));
        Page<User> recentUsers = userService.getAllUsers(recentUsersPageable);
        model.addAttribute("recentUsers", recentUsers.getContent());

        return "admin/dashboard";
    }

    /**
     * User Management - List all users with search and filtering
     */
    @GetMapping("/users")
    public String listUsers(@RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(defaultValue = "id") String sortBy,
            @RequestParam(defaultValue = "asc") String sortDir,
            @RequestParam(required = false) String search,
            @RequestParam(required = false) String role,
            @RequestParam(required = false) Boolean enabled,
            Model model) {

        model.addAttribute("pageTitle", "User Management");
        model.addAttribute("pageDescription", "Manage system users and their roles");
        model.addAttribute("activePage", "user-management");

        // Create pageable object
        Sort sort = sortDir.equalsIgnoreCase("desc") ? Sort.by(sortBy).descending() : Sort.by(sortBy).ascending();
        Pageable pageable = PageRequest.of(page, size, sort);

        // Get users with pagination
        Page<User> usersPage;

        // Apply filters if provided
        if (search != null && !search.trim().isEmpty() ||
                role != null && !role.trim().isEmpty() ||
                enabled != null) {

            User.Role roleEnum = null;
            if (role != null && !role.trim().isEmpty()) {
                try {
                    roleEnum = User.Role.valueOf(role.toUpperCase());
                } catch (IllegalArgumentException e) {
                    // Invalid role, ignore filter
                }
            }
            List<User> filteredUsers = userService.findUsersWithFilters(
                    search != null && !search.trim().isEmpty() ? search.trim() : null,
                    roleEnum,
                    enabled);

            // Manual pagination for filtered results
            int start = (int) pageable.getOffset();
            int end = Math.min((start + pageable.getPageSize()), filteredUsers.size());
            List<User> pageContent = filteredUsers.subList(start, end);

            usersPage = new org.springframework.data.domain.PageImpl<>(
                    pageContent, pageable, filteredUsers.size());
        } else {
            usersPage = userService.getAllUsers(pageable);
        }

        model.addAttribute("usersPage", usersPage);
        model.addAttribute("users", usersPage.getContent());
        model.addAttribute("currentPage", page);
        model.addAttribute("totalPages", usersPage.getTotalPages());
        model.addAttribute("totalElements", usersPage.getTotalElements());
        model.addAttribute("sortBy", sortBy);
        model.addAttribute("sortDir", sortDir);
        model.addAttribute("reverseSortDir", sortDir.equals("asc") ? "desc" : "asc");
        model.addAttribute("search", search);
        model.addAttribute("role", role);
        model.addAttribute("enabled", enabled);
        model.addAttribute("roles", User.Role.values());

        return "admin/users/list";
    }

    /**
     * Show Create User Form
     */
    @GetMapping("/users/new")
    public String showCreateUserForm(Model model) {
        model.addAttribute("pageTitle", "Create New User");
        model.addAttribute("pageDescription", "Add a new user to the system");
        model.addAttribute("activePage", "user-management");
        model.addAttribute("user", new User());
        model.addAttribute("roles", User.Role.values());
        model.addAttribute("isEdit", false);

        return "admin/users/form";
    }

    /**
     * Create New User
     */
    @PostMapping("/users/new")
    public String createUser(@Valid @ModelAttribute User user,
            BindingResult result,
            RedirectAttributes redirectAttributes,
            Model model) {

        if (result.hasErrors()) {
            model.addAttribute("pageTitle", "Create New User");
            model.addAttribute("pageDescription", "Add a new user to the system");
            model.addAttribute("activePage", "user-management");
            model.addAttribute("roles", User.Role.values());
            model.addAttribute("isEdit", false);
            return "admin/users/form";
        }

        try {
            userService.createUser(user);
            redirectAttributes.addFlashAttribute("success",
                    "User '" + user.getUsername() + "' created successfully!");
            return "redirect:/admin/users";

        } catch (RuntimeException e) {
            model.addAttribute("error", e.getMessage());
            model.addAttribute("pageTitle", "Create New User");
            model.addAttribute("pageDescription", "Add a new user to the system");
            model.addAttribute("activePage", "user-management");
            model.addAttribute("roles", User.Role.values());
            model.addAttribute("isEdit", false);
            return "admin/users/form";
        }
    }

    /**
     * Show Edit User Form
     */
    @GetMapping("/users/{id}/edit")
    public String showEditUserForm(@PathVariable Long id, Model model, RedirectAttributes redirectAttributes) {
        try {
            User user = userService.findById(id);

            // Prevent editing own account through this interface
            User currentUser = currentUserService.getCurrentUser();
            if (currentUser != null && currentUser.getId().equals(id)) {
                redirectAttributes.addFlashAttribute("warning",
                        "You cannot edit your own account through this interface. Use profile settings instead.");
                return "redirect:/admin/users";
            }

            model.addAttribute("pageTitle", "Edit User");
            model.addAttribute("pageDescription", "Modify user information and settings");
            model.addAttribute("activePage", "user-management");
            model.addAttribute("user", user);
            model.addAttribute("roles", User.Role.values());
            model.addAttribute("isEdit", true);

            return "admin/users/form";

        } catch (RuntimeException e) {
            redirectAttributes.addFlashAttribute("error", e.getMessage());
            return "redirect:/admin/users";
        }
    }

    /**
     * Update User
     */
    @PostMapping("/users/{id}/edit")
    public String updateUser(@PathVariable Long id,
            @Valid @ModelAttribute User user,
            BindingResult result,
            RedirectAttributes redirectAttributes,
            Model model) {

        user.setId(id); // Ensure the ID is set

        if (result.hasErrors()) {
            model.addAttribute("pageTitle", "Edit User");
            model.addAttribute("pageDescription", "Modify user information and settings");
            model.addAttribute("activePage", "user-management");
            model.addAttribute("roles", User.Role.values());
            model.addAttribute("isEdit", true);
            return "admin/users/form";
        }

        try {
            // Prevent editing own account
            User currentUser = currentUserService.getCurrentUser();
            if (currentUser != null && currentUser.getId().equals(id)) {
                redirectAttributes.addFlashAttribute("warning",
                        "You cannot edit your own account through this interface.");
                return "redirect:/admin/users";
            }

            userService.updateUser(user);
            redirectAttributes.addFlashAttribute("success",
                    "User '" + user.getUsername() + "' updated successfully!");
            return "redirect:/admin/users";

        } catch (RuntimeException e) {
            model.addAttribute("error", e.getMessage());
            model.addAttribute("pageTitle", "Edit User");
            model.addAttribute("pageDescription", "Modify user information and settings");
            model.addAttribute("activePage", "user-management");
            model.addAttribute("roles", User.Role.values());
            model.addAttribute("isEdit", true);
            return "admin/users/form";
        }
    }

    /**
     * View User Details
     */
    @GetMapping("/users/{id}")
    public String viewUser(@PathVariable Long id, Model model, RedirectAttributes redirectAttributes) {
        try {
            User user = userService.findById(id);

            model.addAttribute("pageTitle", "User Details");
            model.addAttribute("pageDescription", "View user information and activity");
            model.addAttribute("activePage", "user-management");
            model.addAttribute("user", user);

            return "admin/users/view";

        } catch (RuntimeException e) {
            redirectAttributes.addFlashAttribute("error", e.getMessage());
            return "redirect:/admin/users";
        }
    }

    /**
     * Toggle User Status (Enable/Disable)
     */
    @PostMapping("/users/{id}/toggle-status")
    public String toggleUserStatus(@PathVariable Long id, RedirectAttributes redirectAttributes) {
        try {
            // Prevent disabling own account
            User currentUser = currentUserService.getCurrentUser();
            if (currentUser != null && currentUser.getId().equals(id)) {
                redirectAttributes.addFlashAttribute("warning",
                        "You cannot disable your own account!");
                return "redirect:/admin/users";
            }

            User user = userService.findById(id);
            userService.toggleUserStatus(id);

            String status = user.isEnabled() ? "disabled" : "enabled";
            redirectAttributes.addFlashAttribute("success",
                    "User '" + user.getUsername() + "' has been " + status + " successfully!");

        } catch (RuntimeException e) {
            redirectAttributes.addFlashAttribute("error", e.getMessage());
        }

        return "redirect:/admin/users";
    }

    /**
     * Delete User
     */
    @PostMapping("/users/{id}/delete")
    public String deleteUser(@PathVariable Long id, RedirectAttributes redirectAttributes) {
        try {
            // Prevent deleting own account
            User currentUser = currentUserService.getCurrentUser();
            if (currentUser != null && currentUser.getId().equals(id)) {
                redirectAttributes.addFlashAttribute("warning",
                        "You cannot delete your own account!");
                return "redirect:/admin/users";
            }

            User user = userService.findById(id);

            // Prevent deleting the last admin user
            if (user.getRole() == User.Role.ADMIN) {
                long adminCount = userService.getUsersCountByRole(User.Role.ADMIN);
                if (adminCount <= 1) {
                    redirectAttributes.addFlashAttribute("warning",
                            "Cannot delete the last admin user in the system!");
                    return "redirect:/admin/users";
                }
            }

            String username = user.getUsername();
            userService.deleteUser(id);

            redirectAttributes.addFlashAttribute("success",
                    "User '" + username + "' deleted successfully!");

        } catch (RuntimeException e) {
            redirectAttributes.addFlashAttribute("error", e.getMessage());
        }

        return "redirect:/admin/users";
    }

    /**
     * Bulk User Operations
     */
    @PostMapping("/users/bulk-action")
    public String bulkUserAction(@RequestParam String action,
            @RequestParam(required = false) List<Long> userIds,
            RedirectAttributes redirectAttributes) {

        if (userIds == null || userIds.isEmpty()) {
            redirectAttributes.addFlashAttribute("warning", "No users selected for bulk action!");
            return "redirect:/admin/users";
        }

        try {
            User currentUser = currentUserService.getCurrentUser();
            int actionCount = 0;

            for (Long userId : userIds) {
                // Skip current user for safety
                if (currentUser != null && currentUser.getId().equals(userId)) {
                    continue;
                }

                switch (action.toLowerCase()) {
                    case "enable":
                        User user = userService.findById(userId);
                        if (!user.isEnabled()) {
                            userService.toggleUserStatus(userId);
                            actionCount++;
                        }
                        break;

                    case "disable":
                        user = userService.findById(userId);
                        if (user.isEnabled()) {
                            userService.toggleUserStatus(userId);
                            actionCount++;
                        }
                        break;

                    case "delete":
                        user = userService.findById(userId);
                        // Don't delete admin users in bulk for safety
                        if (user.getRole() != User.Role.ADMIN) {
                            userService.deleteUser(userId);
                            actionCount++;
                        }
                        break;
                }
            }

            if (actionCount > 0) {
                redirectAttributes.addFlashAttribute("success",
                        actionCount + " user(s) " + action + "d successfully!");
            } else {
                redirectAttributes.addFlashAttribute("info", "No users were affected by the bulk action.");
            }

        } catch (RuntimeException e) {
            redirectAttributes.addFlashAttribute("error", "Bulk action failed: " + e.getMessage());
        }

        return "redirect:/admin/users";
    }
}

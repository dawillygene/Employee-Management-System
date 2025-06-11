package com.employeemanagementsystem.v1.controller;

import com.employeemanagementsystem.v1.entity.User;
import com.employeemanagementsystem.v1.service.UserService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import jakarta.validation.Valid;

@Controller
@RequiredArgsConstructor
public class AuthController {
    
    private final UserService userService;
    
    @GetMapping("/login")
    public String showLoginForm(@RequestParam(value = "error", required = false) String error,
                               @RequestParam(value = "logout", required = false) String logout,
                               Model model) {
        if (error != null) {
            model.addAttribute("error", "Invalid username or password!");
        }
        if (logout != null) {
            model.addAttribute("message", "You have been logged out successfully.");
        }
        return "auth/login";
    }
    
    @GetMapping("/register")
    public String showRegistrationForm(Model model) {
        model.addAttribute("user", new User());
        model.addAttribute("roles", User.Role.values());
        return "auth/register";
    }
    
    @PostMapping("/register")
    public String registerUser(@Valid @ModelAttribute User user, 
                              BindingResult result,
                              RedirectAttributes redirectAttributes,
                              Model model) {
        if (result.hasErrors()) {
            model.addAttribute("roles", User.Role.values());
            return "auth/register";
        }
        
        try {
            userService.createUser(user);
            redirectAttributes.addFlashAttribute("success", "User registered successfully! Please login.");
            return "redirect:/login";
        } catch (RuntimeException e) {
            model.addAttribute("error", e.getMessage());
            model.addAttribute("roles", User.Role.values());
            return "auth/register";
        }
    }
}
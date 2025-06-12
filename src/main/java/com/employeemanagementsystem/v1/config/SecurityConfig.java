package com.employeemanagementsystem.v1.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.config.annotation.authentication.configuration.AuthenticationConfiguration;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;

@Configuration
@EnableWebSecurity
@EnableMethodSecurity(prePostEnabled = true)
public class SecurityConfig {

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    @Bean
    public AuthenticationManager authenticationManager(AuthenticationConfiguration config) throws Exception {
        return config.getAuthenticationManager();
    }

    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        http
            .authorizeHttpRequests(authz -> authz
                .requestMatchers("/css/**", "/js/**", "/images/**", "/webjars/**").permitAll()
                .requestMatchers("/login", "/register").permitAll()
                .requestMatchers("/admin/**").hasRole("ADMIN")
                .requestMatchers("/employees/add", "/employees/*/edit", "/employees/*/delete").hasAnyRole("ADMIN", "HR")
                .requestMatchers("/employees", "/employees/*").hasAnyRole("ADMIN", "HR", "EMPLOYEE")
                .requestMatchers("/leave/approve/**", "/leave/reject/**").hasAnyRole("ADMIN", "HR")
                .requestMatchers("/leave/**").hasAnyRole("ADMIN", "HR", "EMPLOYEE")
                .requestMatchers("/attendance/checkin", "/attendance/mark-absent").hasAnyRole("ADMIN", "HR")
                .requestMatchers("/attendance/**").hasAnyRole("ADMIN", "HR", "EMPLOYEE")
                .requestMatchers("/performance/**").hasAnyRole("ADMIN", "HR")
                .requestMatchers("/reports/**").hasAnyRole("ADMIN", "HR")
                .requestMatchers("/employee-dashboard").hasRole("EMPLOYEE")
                .anyRequest().authenticated()
            )
            .formLogin(form -> form
                .loginPage("/login")
                .defaultSuccessUrl("/dashboard", true)
                .successHandler((request, response, authentication) -> {
                    String role = authentication.getAuthorities().iterator().next().getAuthority();
                    if (role.equals("ROLE_EMPLOYEE")) {
                        response.sendRedirect("/employee-dashboard");
                    } else {
                        response.sendRedirect("/dashboard");
                    }
                })
                .failureUrl("/login?error=true")
                .permitAll()
            )
            .logout(logout -> logout
                .logoutUrl("/logout")
                .logoutSuccessUrl("/login?logout=true")
                .invalidateHttpSession(true)
                .deleteCookies("JSESSIONID")
                .permitAll()
            )
            .sessionManagement(session -> session
                .maximumSessions(1)
                .maxSessionsPreventsLogin(false)
            );

        return http.build();
    }
}
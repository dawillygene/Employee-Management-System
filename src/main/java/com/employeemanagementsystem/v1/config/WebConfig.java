package com.employeemanagementsystem.v1.config;

import com.employeemanagementsystem.v1.entity.Employee;
import com.employeemanagementsystem.v1.repository.EmployeeRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.convert.converter.Converter;
import org.springframework.format.FormatterRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

@Configuration
@RequiredArgsConstructor
public class WebConfig implements WebMvcConfigurer {
    
    private final EmployeeRepository employeeRepository;
    
    @Override
    public void addFormatters(FormatterRegistry registry) {
        registry.addConverter(new StringToEmployeeConverter());
    }
    
    private class StringToEmployeeConverter implements Converter<String, Employee> {
        @Override
        public Employee convert(String id) {
            if (id == null || id.trim().isEmpty()) {
                return null;
            }
            try {
                return employeeRepository.findById(Long.valueOf(id)).orElse(null);
            } catch (NumberFormatException e) {
                return null;
            }
        }
    }
}
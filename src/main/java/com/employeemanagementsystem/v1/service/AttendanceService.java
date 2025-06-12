package com.employeemanagementsystem.v1.service;

import com.employeemanagementsystem.v1.entity.Attendance;
import com.employeemanagementsystem.v1.entity.Employee;
import com.employeemanagementsystem.v1.repository.AttendanceRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;

@Service
@RequiredArgsConstructor
public class AttendanceService {
    
    private final AttendanceRepository attendanceRepository;
    
    public Page<Attendance> findByEmployeeAndDateRange(Employee employee, LocalDate startDate, LocalDate endDate, Pageable pageable) {
        return attendanceRepository.findByEmployeeAndDateBetween(employee, startDate, endDate, pageable);
    }
    
    public double calculateAttendancePercentage(Employee employee, LocalDate startDate, LocalDate endDate) {
        long totalWorkingDays = countWorkingDaysBetween(startDate, endDate);
        long presentDays = countPresentDays(employee, startDate, endDate);
        
        if (totalWorkingDays == 0) {
            return 0.0;
        }
        
        return (double) presentDays / totalWorkingDays * 100.0;
    }
    
    public long countWorkingDaysBetween(LocalDate startDate, LocalDate endDate) {
        long workingDays = 0;
        LocalDate current = startDate;
        
        while (!current.isAfter(endDate)) {
            DayOfWeek dayOfWeek = current.getDayOfWeek();
            if (dayOfWeek != DayOfWeek.SATURDAY && dayOfWeek != DayOfWeek.SUNDAY) {
                workingDays++;
            }
            current = current.plusDays(1);
        }
        
        return workingDays;
    }
    
    public long countPresentDays(Employee employee, LocalDate startDate, LocalDate endDate) {
        return attendanceRepository.countByEmployeeAndDateBetweenAndStatus(
            employee, startDate, endDate, Attendance.AttendanceStatus.PRESENT);
    }
}
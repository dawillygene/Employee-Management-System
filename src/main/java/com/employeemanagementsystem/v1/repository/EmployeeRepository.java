package com.employeemanagementsystem.v1.repository;

import com.employeemanagementsystem.v1.entity.Employee;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface EmployeeRepository extends JpaRepository<Employee, Long> {
    Optional<Employee> findByEmployeeId(String employeeId);
    Optional<Employee> findByEmail(String email);
    boolean existsByEmployeeId(String employeeId);
    boolean existsByEmail(String email);
    
    List<Employee> findByDepartment(String department);
    List<Employee> findByPosition(String position);
    List<Employee> findByStatus(Employee.EmployeeStatus status);
    long countByStatus(Employee.EmployeeStatus status);
    
    @Query("SELECT e FROM Employee e WHERE " +
           "(:search IS NULL OR " +
           "LOWER(e.fullName) LIKE LOWER(CONCAT('%', :search, '%')) OR " +
           "LOWER(e.employeeId) LIKE LOWER(CONCAT('%', :search, '%'))) AND " +
           "(:department IS NULL OR e.department = :department) AND " +
           "(:position IS NULL OR e.position = :position)")
    Page<Employee> findBySearchCriteria(@Param("search") String search,
                                      @Param("department") String department,
                                      @Param("position") String position,
                                      Pageable pageable);
    
    @Query("SELECT DISTINCT e.department FROM Employee e WHERE e.department IS NOT NULL ORDER BY e.department")
    List<String> findDistinctDepartments();
    
    @Query("SELECT DISTINCT e.position FROM Employee e WHERE e.position IS NOT NULL ORDER BY e.position")
    List<String> findDistinctPositions();
}
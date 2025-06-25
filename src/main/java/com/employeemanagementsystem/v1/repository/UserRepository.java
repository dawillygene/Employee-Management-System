package com.employeemanagementsystem.v1.repository;

import com.employeemanagementsystem.v1.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface UserRepository extends JpaRepository<User, Long> {
    Optional<User> findByUsername(String username);

    Optional<User> findByEmail(String email);

    boolean existsByUsername(String username);

    boolean existsByEmail(String email);

    // Additional query methods for User Management
    List<User> findByRole(User.Role role);

    List<User> findByEnabled(boolean enabled);

    long countByRole(User.Role role);

    long countByEnabled(boolean enabled);

    @Query("SELECT u FROM User u WHERE " +
            "(:search IS NULL OR " +
            "LOWER(u.username) LIKE LOWER(CONCAT('%', :search, '%')) OR " +
            "LOWER(u.email) LIKE LOWER(CONCAT('%', :search, '%'))) AND " +
            "(:role IS NULL OR u.role = :role) AND " +
            "(:enabled IS NULL OR u.enabled = :enabled)")
    List<User> findUsersWithFilters(@Param("search") String search,
            @Param("role") User.Role role,
            @Param("enabled") Boolean enabled);
}
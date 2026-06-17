package com.forum.repository;

import com.forum.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface UserRepository extends JpaRepository<User, Long> {
    Optional<User> findByUsername(String username);
    Optional<User> findFirstByEmail(String email);
    Optional<User> findFirstByOrderByIdDesc();

    @Query("SELECT COUNT(u) FROM User u JOIN u.roles r WHERE r = :role")
    long countByRole(@Param("role") String role);

    @Query("SELECT u FROM User u WHERE LOWER(u.displayName) LIKE LOWER(CONCAT('%', :keyword, '%')) AND u.username <> :currentUsername")
    org.springframework.data.domain.Page<User> searchUsersByDisplayName(
            @Param("keyword") String keyword,
            @Param("currentUsername") String currentUsername,
            org.springframework.data.domain.Pageable pageable);

    Optional<User> findFirstByDisplayNameIgnoreCase(String displayName);
}

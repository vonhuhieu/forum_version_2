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

    @Query("SELECT u FROM User u WHERE LOWER(u.displayName) LIKE LOWER(CONCAT('%', :keyword, '%')) " +
           "AND u.username <> :currentUsername " +
           "AND NOT EXISTS (SELECT 1 FROM u.roles r WHERE r = '" + com.forum.utils.Constants.ROLE_ADMIN + "' OR r = '" + com.forum.utils.Constants.ROLE_SUPER_ADMIN + "')")
    org.springframework.data.domain.Page<User> searchUsersByDisplayName(
            @Param("keyword") String keyword,
            @Param("currentUsername") String currentUsername,
            org.springframework.data.domain.Pageable pageable);

    @Query("SELECT u FROM User u WHERE LOWER(u.displayName) LIKE LOWER(CONCAT('%', :keyword, '%')) " +
           "AND NOT EXISTS (SELECT 1 FROM u.roles r WHERE r = '" + com.forum.utils.Constants.ROLE_ADMIN + "' OR r = '" + com.forum.utils.Constants.ROLE_SUPER_ADMIN + "')")
    org.springframework.data.domain.Page<User> searchUsersPublic(
            @Param("keyword") String keyword,
            org.springframework.data.domain.Pageable pageable);

    @Query("SELECT u FROM User u WHERE u.id <> :currentUserId " +
           "AND (:isSuperAdmin = true OR NOT EXISTS (SELECT 1 FROM u.roles r WHERE r = '" + com.forum.utils.Constants.ROLE_ADMIN + "' OR r = '" + com.forum.utils.Constants.ROLE_SUPER_ADMIN + "')) " +
           "AND (:keyword IS NULL OR LOWER(u.username) LIKE LOWER(CONCAT('%', :keyword, '%')) OR LOWER(u.displayName) LIKE LOWER(CONCAT('%', :keyword, '%')) OR LOWER(u.email) LIKE LOWER(CONCAT('%', :keyword, '%'))) " +
           "AND (:roleFilter IS NULL OR :roleFilter MEMBER OF u.roles)")
    org.springframework.data.domain.Page<User> findAdminUsersPaged(
            @Param("currentUserId") Long currentUserId,
            @Param("isSuperAdmin") boolean isSuperAdmin,
            @Param("keyword") String keyword,
            @Param("roleFilter") String roleFilter,
            org.springframework.data.domain.Pageable pageable);

    Optional<User> findFirstByDisplayNameIgnoreCase(String displayName);
}

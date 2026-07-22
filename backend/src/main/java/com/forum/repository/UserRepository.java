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

    @Query("SELECT u FROM User u WHERE '" + com.forum.utils.Constants.ROLE_USER + "' MEMBER OF u.roles AND SIZE(u.roles) = 1 ORDER BY u.createdAt DESC, u.id DESC")
    java.util.List<User> findNewestRoleUsers(org.springframework.data.domain.Pageable pageable);

    @Query(value = "SELECT u.id FROM users u " +
           "LEFT JOIN (SELECT author_id, COUNT(*) as cnt FROM threads GROUP BY author_id) t ON u.id = t.author_id " +
           "LEFT JOIN (SELECT author_id, COUNT(*) as cnt FROM posts GROUP BY author_id) p ON u.id = p.author_id " +
           "WHERE u.id NOT IN (SELECT user_id FROM user_roles WHERE role IN ('" + com.forum.utils.Constants.ROLE_ADMIN + "', '" + com.forum.utils.Constants.ROLE_SUPER_ADMIN + "')) " +
           "ORDER BY (COALESCE(t.cnt, 0) + COALESCE(p.cnt, 0)) DESC",
           nativeQuery = true)
    java.util.List<Long> findTopPosterUserIds(org.springframework.data.domain.Pageable pageable);

    @Query(value = "SELECT u.id FROM users u " +
           "LEFT JOIN (" +
           "  SELECT author_id, COUNT(*) as cnt FROM (" +
           "    SELECT t.author_id FROM reactions r JOIN threads t ON r.thread_id = t.id WHERE r.post_id IS NULL " +
           "    UNION ALL " +
           "    SELECT p.author_id FROM reactions r JOIN posts p ON r.post_id = p.id " +
           "  ) rx GROUP BY author_id" +
           ") r_total ON u.id = r_total.author_id " +
           "WHERE u.id NOT IN (SELECT user_id FROM user_roles WHERE role IN ('" + com.forum.utils.Constants.ROLE_ADMIN + "', '" + com.forum.utils.Constants.ROLE_SUPER_ADMIN + "')) " +
           "ORDER BY COALESCE(r_total.cnt, 0) DESC",
           nativeQuery = true)
    java.util.List<Long> findTopInteractionUserIds(org.springframework.data.domain.Pageable pageable);

    @Query(value = "SELECT u.id FROM users u " +
           "LEFT JOIN (SELECT author_id, COUNT(*) as cnt FROM threads GROUP BY author_id) t ON u.id = t.author_id " +
           "LEFT JOIN (SELECT author_id, COUNT(*) as cnt FROM posts GROUP BY author_id) p ON u.id = p.author_id " +
           "LEFT JOIN (" +
           "  SELECT author_id, COUNT(*) as cnt FROM (" +
           "    SELECT t.author_id FROM reactions r JOIN threads t ON r.thread_id = t.id WHERE r.post_id IS NULL " +
           "    UNION ALL " +
           "    SELECT p.author_id FROM reactions r JOIN posts p ON r.post_id = p.id " +
           "  ) rx GROUP BY author_id" +
           ") r_total ON u.id = r_total.author_id " +
           "WHERE u.id NOT IN (SELECT user_id FROM user_roles WHERE role IN ('" + com.forum.utils.Constants.ROLE_ADMIN + "', '" + com.forum.utils.Constants.ROLE_SUPER_ADMIN + "')) " +
           "ORDER BY ((COALESCE(t.cnt, 0) + COALESCE(p.cnt, 0)) * 0.1 + COALESCE(r_total.cnt, 0) * 0.2) DESC",
           nativeQuery = true)
    java.util.List<Long> findTopTrophyPointUserIds(org.springframework.data.domain.Pageable pageable);
}

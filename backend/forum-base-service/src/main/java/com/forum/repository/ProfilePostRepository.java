package com.forum.repository;

import com.forum.entity.ProfilePost;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface ProfilePostRepository extends JpaRepository<ProfilePost, Long> {

    @EntityGraph(attributePaths = {"author", "profileUser"})
    @Query(
        value = "SELECT p FROM ProfilePost p WHERE p.profileUser.id = :profileUserId ORDER BY p.createdAt DESC",
        countQuery = "SELECT COUNT(p) FROM ProfilePost p WHERE p.profileUser.id = :profileUserId"
    )
    Page<ProfilePost> findByProfileUserIdOrderByCreatedAtDesc(@Param("profileUserId") Long profileUserId, Pageable pageable);

    @EntityGraph(attributePaths = {"author", "profileUser"})
    Optional<ProfilePost> findWithDetailsById(Long id);

    long countByProfileUserId(Long profileUserId);

    @Modifying
    @Query("DELETE FROM ProfilePost p WHERE p.profileUser.id = :profileUserId")
    void deleteByProfileUserId(@Param("profileUserId") Long profileUserId);
}

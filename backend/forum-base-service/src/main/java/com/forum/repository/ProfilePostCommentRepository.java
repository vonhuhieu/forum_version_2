package com.forum.repository;

import com.forum.entity.ProfilePostComment;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface ProfilePostCommentRepository extends JpaRepository<ProfilePostComment, Long> {

    @EntityGraph(attributePaths = {"author"})
    @Query(
        value = "SELECT c FROM ProfilePostComment c WHERE c.profilePost.id = :profilePostId ORDER BY c.createdAt DESC, c.id DESC",
        countQuery = "SELECT COUNT(c) FROM ProfilePostComment c WHERE c.profilePost.id = :profilePostId"
    )
    Page<ProfilePostComment> findByProfilePostIdOrderByCreatedAtDesc(@Param("profilePostId") Long profilePostId, Pageable pageable);

    @EntityGraph(attributePaths = {"author"})
    List<ProfilePostComment> findTop3ByProfilePostIdOrderByCreatedAtDescIdDesc(@Param("profilePostId") Long profilePostId);

    long countByProfilePostId(Long profilePostId);

    @Modifying
    @Query("DELETE FROM ProfilePostComment c WHERE c.profilePost.id = :profilePostId")
    void deleteByProfilePostId(@Param("profilePostId") Long profilePostId);
}

package com.forum.repository;

import com.forum.entity.PollVote;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface PollVoteRepository extends JpaRepository<PollVote, Long> {
    List<PollVote> findByPollIdAndUserId(Long pollId, Long userId);
    boolean existsByPollIdAndOptionIdAndUserId(Long pollId, Long optionId, Long userId);
    int countByOptionId(Long optionId);
    
    void deleteByPollId(Long pollId);
    void deleteByOptionId(Long optionId);

    @org.springframework.data.jpa.repository.Query("SELECT new com.forum.dto.PollVoteDetailDTO(pv.id, u.username, u.displayName, po.id, po.optionText) " +
            "FROM PollVote pv " +
            "JOIN pv.user u " +
            "JOIN pv.option po " +
            "WHERE pv.poll.id = :pollId " +
            "AND (:keyword IS NULL OR :keyword = '' OR LOWER(u.username) LIKE LOWER(CONCAT('%', :keyword, '%')) OR LOWER(u.displayName) LIKE LOWER(CONCAT('%', :keyword, '%'))) " +
            "AND (:optionId IS NULL OR po.id = :optionId)")
    org.springframework.data.domain.Page<com.forum.dto.PollVoteDetailDTO> findVotesByPollIdWithFilters(
            @org.springframework.data.repository.query.Param("pollId") Long pollId,
            @org.springframework.data.repository.query.Param("keyword") String keyword,
            @org.springframework.data.repository.query.Param("optionId") Long optionId,
            org.springframework.data.domain.Pageable pageable);
}

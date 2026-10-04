package com.forum.repository;

import com.forum.entity.SearchHistory;
import com.forum.entity.User;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface SearchHistoryRepository extends JpaRepository<SearchHistory, Long> {

    List<SearchHistory> findByUserUsernameOrderBySearchTimeDesc(String username, Pageable pageable);

    Optional<SearchHistory> findByUserAndKeywordIgnoreCase(User user, String keyword);

    void deleteByUserUsernameAndKeywordIgnoreCase(String username, String keyword);

    void deleteByUserUsername(String username);

    long countByUser(User user);

    List<SearchHistory> findByUserOrderBySearchTimeAsc(User user);
}

package com.forum.repository;

import com.forum.entity.ThreadSubscription;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface ThreadSubscriptionRepository extends JpaRepository<ThreadSubscription, Long> {
    Optional<ThreadSubscription> findByThreadIdAndUserId(Long threadId, Long userId);
    Optional<ThreadSubscription> findByThreadIdAndUserUsername(Long threadId, String username);
    List<ThreadSubscription> findByThreadIdAndIsFollowingTrue(Long threadId);
    List<ThreadSubscription> findByUserIdAndThreadIdIn(Long userId, List<Long> threadIds);
    
    @org.springframework.transaction.annotation.Transactional
    void deleteByThreadId(Long threadId);
}

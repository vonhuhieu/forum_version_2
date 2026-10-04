package com.forum.repository;

import com.forum.entity.Poll;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface PollRepository extends JpaRepository<Poll, Long> {
    Optional<Poll> findByThreadId(Long threadId);
}

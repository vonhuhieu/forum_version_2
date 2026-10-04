package com.forum.service;

import com.forum.repository.ThreadRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class ThreadViewIncrementer {
    private final ThreadRepository threadRepository;

    @Async
    @Transactional
    public void incrementViewCountAsync(Long threadId) {
        try {
            threadRepository.incrementViewCount(threadId);
        } catch (Exception e) {
            // ignore
        }
    }
}

package com.forum.lab.service;

import com.forum.lab.utils.Constants;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;

@Slf4j
@Service
public class QuotaService {

    @Value("${lab.quota.guest.daily-requests:5}")
    private int guestDailyLimit;

    @Value("${lab.quota.user.daily-requests:30}")
    private int userDailyLimit;

    private final ConcurrentHashMap<String, DailyCounter> requestCounts = new ConcurrentHashMap<>();

    private static class DailyCounter {
        private final LocalDate date;
        private final AtomicInteger count;

        public DailyCounter(LocalDate date) {
            this.date = date;
            this.count = new AtomicInteger(0);
        }
    }

    public boolean checkAndIncrement(String identifier, boolean isRegisteredUser) {
        int maxLimit = isRegisteredUser ? userDailyLimit : guestDailyLimit;
        LocalDate today = LocalDate.now();

        DailyCounter counter = requestCounts.compute(identifier, (key, existing) -> {
            if (existing == null || !existing.date.equals(today)) {
                return new DailyCounter(today);
            }
            return existing;
        });

        int current = counter.count.incrementAndGet();
        if (current > maxLimit) {
            log.warn("Định danh {} đã vượt hạn mức ngày: {}/{}", identifier, current, maxLimit);
            return false;
        }
        return true;
    }

    public int getRemainingQuota(String identifier, boolean isRegisteredUser) {
        int maxLimit = isRegisteredUser ? userDailyLimit : guestDailyLimit;
        LocalDate today = LocalDate.now();
        DailyCounter counter = requestCounts.get(identifier);
        if (counter == null || !counter.date.equals(today)) {
            return maxLimit;
        }
        return Math.max(0, maxLimit - counter.count.get());
    }
}

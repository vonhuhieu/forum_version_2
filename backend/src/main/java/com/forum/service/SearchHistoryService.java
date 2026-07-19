package com.forum.service;

import com.forum.entity.SearchHistory;
import com.forum.entity.User;
import com.forum.repository.SearchHistoryRepository;
import com.forum.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Transactional
@Slf4j
public class SearchHistoryService {

    private final SearchHistoryRepository searchHistoryRepository;
    private final UserRepository userRepository;

    /**
     * Lưu từ khóa tìm kiếm cho user.
     */
    public void saveSearchKeyword(String username, String keyword) {
        if (username == null || "anonymousUser".equals(username) || keyword == null || keyword.trim().isEmpty()) {
            return;
        }

        String cleanedKeyword = keyword.trim();
        Optional<User> userOpt = userRepository.findByUsername(username);
        if (userOpt.isEmpty()) {
            return;
        }

        User user = userOpt.get();

        // Kiểm tra xem từ khóa này đã tồn tại trong lịch sử của user chưa
        Optional<SearchHistory> existingHistory = searchHistoryRepository.findByUserAndKeywordIgnoreCase(user, cleanedKeyword);

        if (existingHistory.isPresent()) {
            // Cập nhật lại thời gian tìm kiếm mới nhất
            SearchHistory history = existingHistory.get();
            history.setSearchTime(LocalDateTime.now());
            searchHistoryRepository.save(history);
        } else {
            // Tạo bản ghi mới
            SearchHistory history = new SearchHistory();
            history.setUser(user);
            history.setKeyword(cleanedKeyword);
            searchHistoryRepository.save(history);

            // Giới hạn số lượng bản ghi tối đa (nâng lên 200 bản ghi)
            long count = searchHistoryRepository.countByUser(user);
            if (count > 200) {
                List<SearchHistory> oldestList = searchHistoryRepository.findByUserOrderBySearchTimeAsc(user);
                if (!oldestList.isEmpty()) {
                    // Xóa bản ghi cũ nhất
                    searchHistoryRepository.delete(oldestList.get(0));
                }
            }
        }
    }

    /**
     * Lấy danh sách lịch sử tìm kiếm (tối đa 200 từ khóa) của user.
     */
    @Transactional(readOnly = true)
    public List<String> getRecentKeywords(String username) {
        if (username == null || "anonymousUser".equals(username)) {
            return List.of();
        }

        Pageable limitTwoHundred = PageRequest.of(0, 200);
        List<SearchHistory> historyList = searchHistoryRepository.findByUserUsernameOrderBySearchTimeDesc(username, limitTwoHundred);
        return historyList.stream()
                .map(SearchHistory::getKeyword)
                .collect(Collectors.toList());
    }

    /**
     * Xóa một từ khóa cụ thể của user.
     */
    public void deleteKeyword(String username, String keyword) {
        if (username == null || "anonymousUser".equals(username) || keyword == null) {
            return;
        }
        searchHistoryRepository.deleteByUserUsernameAndKeywordIgnoreCase(username, keyword.trim());
    }

    /**
     * Xóa toàn bộ lịch sử tìm kiếm của user.
     */
    public void clearHistory(String username) {
        if (username == null || "anonymousUser".equals(username)) {
            return;
        }
        searchHistoryRepository.deleteByUserUsername(username);
    }

    /**
     * Đồng bộ hóa danh sách từ khóa vãng lai lên database của user.
     */
    public void syncSearchKeywords(String username, List<String> keywords) {
        if (username == null || "anonymousUser".equals(username) || keywords == null || keywords.isEmpty()) {
            return;
        }
        Optional<User> userOpt = userRepository.findByUsername(username);
        if (userOpt.isEmpty()) {
            return;
        }
        User user = userOpt.get();

        // Lấy tất cả lịch sử hiện tại của user để đối chiếu trong RAM
        List<SearchHistory> currentHistory = searchHistoryRepository.findByUserOrderBySearchTimeAsc(user);
        
        // Duyệt từ cũ nhất đến mới nhất (danh sách gửi từ frontend có từ khóa mới nhất ở index 0)
        for (int i = keywords.size() - 1; i >= 0; i--) {
            String keyword = keywords.get(i);
            if (keyword == null || keyword.trim().isEmpty()) {
                continue;
            }
            String cleanedKeyword = keyword.trim();
            
            Optional<SearchHistory> existing = currentHistory.stream()
                    .filter(h -> h.getKeyword().equalsIgnoreCase(cleanedKeyword))
                    .findFirst();
            
            if (existing.isPresent()) {
                SearchHistory history = existing.get();
                history.setSearchTime(LocalDateTime.now().plusNanos((keywords.size() - i) * 1000000L));
                searchHistoryRepository.save(history);
            } else {
                SearchHistory history = new SearchHistory();
                history.setUser(user);
                history.setKeyword(cleanedKeyword);
                history.setSearchTime(LocalDateTime.now().plusNanos((keywords.size() - i) * 1000000L));
                searchHistoryRepository.save(history);
            }
        }
        
        // Dọn dẹp bản ghi cũ nếu tổng số lượng vượt quá 200
        long count = searchHistoryRepository.countByUser(user);
        if (count > 200) {
            List<SearchHistory> oldestList = searchHistoryRepository.findByUserOrderBySearchTimeAsc(user);
            int toDelete = oldestList.size() - 200;
            for (int i = 0; i < toDelete; i++) {
                searchHistoryRepository.delete(oldestList.get(i));
            }
        }
    }
}

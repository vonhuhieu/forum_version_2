package com.forum.service;

import com.forum.entity.TitleType;
import com.forum.entity.User;
import com.forum.entity.UserTitle;
import com.forum.repository.UserRepository;
import com.forum.repository.UserTitleRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;

@Service
@RequiredArgsConstructor
public class UserTitleService {

    private final UserTitleRepository userTitleRepository;
    private final UserRepository userRepository;

    // Lấy toàn bộ danh sách Title (Cache trong RAM)
    @Cacheable(value = "userTitles")
    public List<UserTitle> getAllTitles() {
        return userTitleRepository.findAll();
    }

    // Lấy các mốc điểm từ cao xuống thấp (Cache trong RAM)
    @Cacheable(value = "userTitles_point_based")
    public List<UserTitle> getPointBasedTitlesDesc() {
        return userTitleRepository.findByTypeOrderByMinPointsDesc(TitleType.POINT_BASED);
    }

    // Lấy title cho trạng thái chưa xác thực (Cache trong RAM)
    @Cacheable(value = "userTitles_unverified")
    public Optional<UserTitle> getUnverifiedDefaultTitle() {
        return userTitleRepository.findFirstByType(TitleType.UNVERIFIED_DEFAULT);
    }

    // Xóa cache mỗi khi Admin thêm/sửa/xóa Title
    @CacheEvict(value = {"userTitles", "userTitles_point_based", "userTitles_unverified"}, allEntries = true)
    @Transactional
    public UserTitle createTitle(UserTitle title) {
        if (title.getType() == TitleType.UNVERIFIED_DEFAULT) {
            // Nếu tạo UNVERIFIED_DEFAULT mới, chuyển các UNVERIFIED_DEFAULT cũ thành CUSTOM_ASSIGNABLE để chỉ duy trì 1 default
            List<UserTitle> existingDefaults = userTitleRepository.findByType(TitleType.UNVERIFIED_DEFAULT);
            for (UserTitle existing : existingDefaults) {
                existing.setType(TitleType.CUSTOM_ASSIGNABLE);
                userTitleRepository.save(existing);
            }
        }
        if (title.getType() == TitleType.POINT_BASED && title.getMinPoints() == null) {
            title.setMinPoints(0);
        }
        return userTitleRepository.save(title);
    }

    @CacheEvict(value = {"userTitles", "userTitles_point_based", "userTitles_unverified"}, allEntries = true)
    @Transactional
    public UserTitle updateTitle(Long id, UserTitle titleDetails) {
        UserTitle title = userTitleRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Không tìm thấy Cấp bậc với ID: " + id));

        if (titleDetails.getType() == TitleType.UNVERIFIED_DEFAULT && title.getType() != TitleType.UNVERIFIED_DEFAULT) {
            List<UserTitle> existingDefaults = userTitleRepository.findByType(TitleType.UNVERIFIED_DEFAULT);
            for (UserTitle existing : existingDefaults) {
                existing.setType(TitleType.CUSTOM_ASSIGNABLE);
                userTitleRepository.save(existing);
            }
        }

        title.setName(titleDetails.getName());
        title.setType(titleDetails.getType());
        title.setMinPoints(titleDetails.getType() == TitleType.POINT_BASED ? titleDetails.getMinPoints() : null);
        title.setDescription(titleDetails.getDescription());

        return userTitleRepository.save(title);
    }

    @CacheEvict(value = {"userTitles", "userTitles_point_based", "userTitles_unverified"}, allEntries = true)
    @Transactional
    public void deleteTitle(Long id) {
        UserTitle title = userTitleRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Không tìm thấy Cấp bậc với ID: " + id));
        
        // Gỡ title này khỏi những user đang được gán trực tiếp
        userRepository.findAll().stream()
                .filter(u -> u.getAssignedTitle() != null && u.getAssignedTitle().getId().equals(id))
                .forEach(u -> {
                    u.setAssignedTitle(null);
                    userRepository.save(u);
                });

        userTitleRepository.delete(title);
    }

    // Admin gán/bỏ gán Title trực tiếp cho User
    @Transactional
    public User assignTitleToUser(Long userId, Long titleId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new IllegalArgumentException("Không tìm thấy người dùng với ID: " + userId));

        if (titleId == null) {
            user.setAssignedTitle(null);
        } else {
            UserTitle title = userTitleRepository.findById(titleId)
                    .orElseThrow(() -> new IllegalArgumentException("Không tìm thấy Cấp bậc với ID: " + titleId));
            user.setAssignedTitle(title);
        }
        return userRepository.save(user);
    }

    /**
     * Logic giải mã Title hiển thị (Plain Text) cho User dựa trên thứ tự ưu tiên:
     * 1. Quyền lực tối cao: Admin Assign Title (nếu assignedTitle != null)
     * 2. Trạng thái chưa xác thực -> Title UNVERIFIED_DEFAULT
     * 3. Trạng thái đã xác thực -> Title mốc điểm trophyPoints (mốc minPoints cao nhất thỏa mãn)
     */
    public String resolveDisplayTitle(User user, Long trophyPoints) {
        if (user == null) return null;

        // Ưu tiên 1: Title do Admin gán trực tiếp
        if (user.getAssignedTitle() != null) {
            return user.getAssignedTitle().getName();
        }

        // Ưu tiên 2: Chưa xác thực
        if (!user.isVerified()) {
            Optional<UserTitle> unverifiedOpt = getUnverifiedDefaultTitle();
            return unverifiedOpt.map(UserTitle::getName).orElse(null);
        }

        // Ưu tiên 3: Đã xác thực - Xét duyệt theo mốc trophyPoints
        long points = trophyPoints != null ? trophyPoints : 0L;
        List<UserTitle> pointBasedTitles = getPointBasedTitlesDesc();
        for (UserTitle title : pointBasedTitles) {
            if (title.getMinPoints() != null && points >= title.getMinPoints()) {
                return title.getName();
            }
        }

        return null;
    }
}

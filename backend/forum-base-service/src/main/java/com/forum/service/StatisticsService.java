package com.forum.service;

import com.forum.dto.StatisticsDTO;
import com.forum.entity.User;
import com.forum.repository.CategoryRepository;
import com.forum.repository.ThreadRepository;
import com.forum.repository.UserRepository;
import com.forum.utils.Constants;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional(readOnly = true)
@RequiredArgsConstructor
public class StatisticsService {

    private final CategoryRepository categoryRepository;
    private final ThreadRepository threadRepository;
    private final UserRepository userRepository;

    public StatisticsDTO getStatistics() {
        long totalCategories = categoryRepository.count();
        long totalThreads = threadRepository.count();
        Long totalReplies = threadRepository.countReplies();
        if (totalReplies == null) {
            totalReplies = 0L;
        }
        
        long totalPosts = totalThreads + totalReplies;
        long totalMembers = userRepository.count();
        
        String latestMember = userRepository.findFirstByOrderByIdDesc()
                .map(u -> org.springframework.util.StringUtils.hasText(u.getDisplayName()) ? u.getDisplayName() : u.getUsername())
                .orElse("Chưa có");

        long officialMembers = userRepository.countByRole(Constants.ROLE_USER);
        long unofficialMembers = userRepository.countByRole(Constants.ROLE_NON_OFFICIAL_USER);
        long totalOfficialAndUnofficial = officialMembers + unofficialMembers;

        return StatisticsDTO.builder()
                .totalCategories(totalCategories)
                .totalThreads(totalThreads)
                .totalPosts(totalPosts)
                .totalMembers(totalMembers)
                .latestMember(latestMember)
                .officialMembers(officialMembers)
                .unofficialMembers(unofficialMembers)
                .totalOfficialAndUnofficial(totalOfficialAndUnofficial)
                .build();
    }
}

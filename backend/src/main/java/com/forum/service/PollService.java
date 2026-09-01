package com.forum.service;

import com.forum.dto.PollDTO;
import com.forum.dto.PollOptionDTO;
import com.forum.entity.*;
import com.forum.repository.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

@Slf4j
@Service
@Transactional
@RequiredArgsConstructor
public class PollService {

    private final PollRepository pollRepository;
    private final PollVoteRepository pollVoteRepository;
    private final UserRepository userRepository;

    public PollDTO toPollDTO(Poll poll) {
        if (poll == null) return null;

        // Lấy username hiện tại (có thể là anonymous)
        String username = null;
        try {
            Object principal = SecurityContextHolder.getContext().getAuthentication().getPrincipal();
            if (principal instanceof String && !principal.equals("anonymousUser")) {
                username = (String) principal;
            }
        } catch (Exception e) {
            log.debug("Không thể trích xuất thông tin authentication khi chuyển đổi PollDTO: {}", e.getMessage());
        }

        User currentUser = null;
        if (username != null) {
            currentUser = userRepository.findByUsername(username).orElse(null);
        }

        int totalVotes = poll.getOptions().stream().mapToInt(PollOption::getVoteCount).sum();

        List<PollOptionDTO> optionDTOs = poll.getOptions().stream().map(opt -> {
            PollOptionDTO dto = new PollOptionDTO();
            dto.setId(opt.getId());
            dto.setOptionText(opt.getOptionText());
            dto.setVoteCount(opt.getVoteCount());
            dto.setPercentage(totalVotes > 0 ? Math.round(opt.getVoteCount() * 1000.0 / totalVotes) / 10.0 : 0.0);
            return dto;
        }).collect(Collectors.toList());

        List<Long> userVotedOptionIds = new ArrayList<>();
        if (currentUser != null) {
            List<PollVote> votes = pollVoteRepository.findByPollIdAndUserId(poll.getId(), currentUser.getId());
            userVotedOptionIds = votes.stream().map(v -> v.getOption().getId()).collect(Collectors.toList());
        }

        PollDTO dto = new PollDTO();
        dto.setId(poll.getId());
        dto.setQuestion(poll.getQuestion());
        dto.setMaxChoices(poll.getMaxChoices());
        dto.setAllowChangeVote(poll.isAllowChangeVote());
        dto.setPublicVoting(poll.isPublicVoting());
        dto.setShowResultWithoutVote(poll.isShowResultWithoutVote());
        dto.setClosedAt(poll.getClosedAt());
        dto.setOptions(optionDTOs);
        dto.setTotalVotes(totalVotes);
        dto.setUserVotedOptionIds(userVotedOptionIds);
        return dto;
    }

    public PollDTO vote(Long pollId, List<Long> optionIds) {
        Poll poll = pollRepository.findById(pollId)
                .orElseThrow(() -> new RuntimeException("Poll not found"));

        String username = (String) SecurityContextHolder.getContext().getAuthentication().getPrincipal();
        User user = userRepository.findByUsername(username)
                .orElseThrow(() -> new RuntimeException("User not found"));

        // Kiểm tra poll còn mở
        if (poll.getClosedAt() != null && poll.getClosedAt().isBefore(java.time.LocalDateTime.now())) {
            throw new RuntimeException("Poll is closed");
        }

        // Validate số lựa chọn
        int maxChoices = poll.getMaxChoices();
        if (maxChoices > 0 && optionIds.size() > maxChoices) {
            throw new RuntimeException("Exceeded max choices");
        }

        // Xóa vote cũ nếu cho phép thay đổi
        List<PollVote> existingVotes = pollVoteRepository.findByPollIdAndUserId(pollId, user.getId());
        if (!existingVotes.isEmpty()) {
            if (!poll.isAllowChangeVote()) {
                throw new RuntimeException("Change vote not allowed");
            }
            // Giảm voteCount cũ
            for (PollVote v : existingVotes) {
                PollOption opt = v.getOption();
                opt.setVoteCount(Math.max(0, opt.getVoteCount() - 1));
            }
            pollVoteRepository.deleteAll(existingVotes);
        }

        // Thêm vote mới
        for (Long optionId : optionIds) {
            PollOption option = poll.getOptions().stream()
                    .filter(o -> o.getId().equals(optionId))
                    .findFirst()
                    .orElseThrow(() -> new RuntimeException("Option not found"));

            option.setVoteCount(option.getVoteCount() + 1);

            PollVote vote = new PollVote();
            vote.setPoll(poll);
            vote.setOption(option);
            vote.setUser(user);
            pollVoteRepository.save(vote);
        }

        poll = pollRepository.save(poll);
        return toPollDTO(poll);
    }

    public org.springframework.data.domain.Page<com.forum.dto.PollVoteDetailDTO> getPollVotes(
            Long pollId, String keyword, Long optionId, int page, int size) {
        
        Poll poll = pollRepository.findById(pollId)
                .orElseThrow(() -> new RuntimeException("Poll not found"));

        if (!poll.isPublicVoting()) {
            throw new RuntimeException("This poll does not allow public viewing of votes");
        }

        org.springframework.data.domain.Pageable pageable = 
            org.springframework.data.domain.PageRequest.of(page, size);

        return pollVoteRepository.findVotesByPollIdWithFilters(pollId, keyword, optionId, pageable);
    }
}

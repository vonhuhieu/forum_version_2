package com.forum.service;

import com.forum.dto.ResponseDTO;
import com.forum.entity.User;
import com.forum.entity.UserFollow;
import com.forum.repository.UserFollowRepository;
import com.forum.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;

@Service
@RequiredArgsConstructor
@Transactional
public class UserFollowService {

    private final UserFollowRepository userFollowRepository;
    private final UserRepository userRepository;

    public ResponseDTO<Boolean> getFollowStatus(String username) {
        String currentUsername = (String) SecurityContextHolder.getContext().getAuthentication().getPrincipal();
        if ("anonymousUser".equals(currentUsername) || currentUsername == null) {
            return ResponseDTO.success(false);
        }

        User currentUser = userRepository.findByUsername(currentUsername)
                .orElseThrow(() -> new RuntimeException("User not found"));
        User targetUser = userRepository.findByUsername(username)
                .orElseThrow(() -> new RuntimeException("Target user not found"));

        boolean isFollowing = userFollowRepository.existsByFollowerIdAndFollowingId(currentUser.getId(), targetUser.getId());
        return ResponseDTO.success(isFollowing);
    }

    public ResponseDTO<Void> setFollowStatus(String username, boolean following) {
        String currentUsername = (String) SecurityContextHolder.getContext().getAuthentication().getPrincipal();
        if ("anonymousUser".equals(currentUsername) || currentUsername == null) {
            throw new RuntimeException("User must be logged in to follow members");
        }

        User currentUser = userRepository.findByUsername(currentUsername)
                .orElseThrow(() -> new RuntimeException("User not found"));
        User targetUser = userRepository.findByUsername(username)
                .orElseThrow(() -> new RuntimeException("Target user not found"));

        if (currentUser.getId().equals(targetUser.getId())) {
            throw new RuntimeException("Cannot follow yourself");
        }

        Optional<UserFollow> followOpt = userFollowRepository.findByFollowerIdAndFollowingId(currentUser.getId(), targetUser.getId());

        if (following) {
            if (followOpt.isEmpty()) {
                UserFollow follow = new UserFollow();
                follow.setFollower(currentUser);
                follow.setFollowing(targetUser);
                userFollowRepository.save(follow);
            }
        } else {
            if (followOpt.isPresent()) {
                userFollowRepository.deleteByFollowerIdAndFollowingId(currentUser.getId(), targetUser.getId());
            }
        }

        return ResponseDTO.success(null);
    }
}

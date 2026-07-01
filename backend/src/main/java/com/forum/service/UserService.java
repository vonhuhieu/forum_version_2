package com.forum.service;

import com.forum.dto.UserDTO;
import com.forum.entity.User;
import com.forum.repository.UserRepository;
import com.forum.repository.ThreadRepository;
import com.forum.repository.PostRepository;
import com.forum.repository.ReactionRepository;
import com.forum.utils.Constants;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import jakarta.persistence.EntityManager;
import java.text.Normalizer;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class UserService {

    private final UserRepository userRepository;
    private final ThreadRepository threadRepository;
    private final PostRepository postRepository;
    private final ReactionRepository reactionRepository;
    private final PasswordEncoder passwordEncoder;
    private final EntityManager entityManager;
    private final EmailService emailService;

    public Page<UserDTO> searchUsers(String keyword, String currentUsername, int page, int size) {
        String trimmedKeyword = keyword != null ? keyword.trim() : "";
        org.springframework.data.domain.Pageable pageable = org.springframework.data.domain.PageRequest.of(page, size);
        Page<User> userPage = userRepository.searchUsersByDisplayName(trimmedKeyword, currentUsername, pageable);
        return userPage.map(this::convertToDTO);
    }

    public Optional<UserDTO> getUserByName(String name) {
        Optional<User> userOpt = userRepository.findByUsername(name);
        if (userOpt.isEmpty()) {
            userOpt = userRepository.findFirstByDisplayNameIgnoreCase(name);
        }
        return userOpt.map(user -> {
            UserDTO dto = convertToDTO(user);
            enrichUserStats(dto);
            return dto;
        });
    }

    private void enrichUserStats(UserDTO dto) {
        if (dto == null || dto.getId() == null) return;
        Long userId = dto.getId();
        long threadCount = threadRepository.countByAuthorId(userId);
        long postCountInDb = postRepository.countByAuthorId(userId);
        long totalPosts = threadCount + postCountInDb;
        
        long interactionPoints = reactionRepository.countReactionsReceivedByUserId(userId);
        long trophyPoints = Math.round(totalPosts * 0.1 + interactionPoints * 0.2);
        
        dto.setPostCount(totalPosts);
        dto.setInteractionPoints(interactionPoints);
        dto.setTrophyPoints(trophyPoints);
        dto.setThreadCount(threadCount);
        dto.setCommentCount(postCountInDb);
    }

    public List<UserDTO> getAdminUsers(String currentUsername) {
        User currentUser = userRepository.findByUsername(currentUsername)
                .orElseThrow(() -> new IllegalArgumentException("User not found"));

        boolean isSuperAdmin = currentUser.getRoles().contains(Constants.ROLE_SUPER_ADMIN);
        boolean isAdmin = currentUser.getRoles().contains(Constants.ROLE_ADMIN);

        List<User> allUsers = userRepository.findAll();
        List<UserDTO> result = new ArrayList<>();

        for (User user : allUsers) {
            if (user.getId().equals(currentUser.getId())) {
                continue;
            }

            boolean match = false;
            if (isSuperAdmin) {
                match = true;
            } else if (isAdmin) {
                boolean hasAdmin = user.getRoles().contains(Constants.ROLE_ADMIN);
                boolean hasSuperAdmin = user.getRoles().contains(Constants.ROLE_SUPER_ADMIN);
                if (!hasAdmin && !hasSuperAdmin) {
                    match = true;
                }
            }

            if (match) {
                result.add(convertToDTO(user));
            }
        }

        return result;
    }

    public com.forum.dto.PageResponseDTO<UserDTO> getAdminUsersPaged(
            String currentUsername, String keyword, String roleFilter, String sortBy, String sortOrder, int page, int size) {
        
        User currentUser = userRepository.findByUsername(currentUsername)
                .orElseThrow(() -> new IllegalArgumentException("User not found"));

        boolean isSuperAdmin = currentUser.getRoles().contains(Constants.ROLE_SUPER_ADMIN);

        org.springframework.data.domain.Sort sort = org.springframework.data.domain.Sort.unsorted();
        if (sortBy != null && !sortBy.trim().isEmpty()) {
            org.springframework.data.domain.Sort.Direction direction = 
                "desc".equalsIgnoreCase(sortOrder) ? org.springframework.data.domain.Sort.Direction.DESC : org.springframework.data.domain.Sort.Direction.ASC;
            sort = org.springframework.data.domain.Sort.by(direction, sortBy);
        } else {
            sort = org.springframework.data.domain.Sort.by(org.springframework.data.domain.Sort.Direction.DESC, "createdAt");
        }

        org.springframework.data.domain.Pageable pageable = org.springframework.data.domain.PageRequest.of(page, size, sort);
        
        String trimmedKeyword = (keyword != null && !keyword.trim().isEmpty()) ? keyword.trim() : null;
        String trimmedRole = (roleFilter != null && !roleFilter.trim().isEmpty()) ? roleFilter.trim() : null;

        org.springframework.data.domain.Page<User> userPage = userRepository.findAdminUsersPaged(
                currentUser.getId(), isSuperAdmin, trimmedKeyword, trimmedRole, pageable);

        List<UserDTO> dtos = userPage.getContent().stream()
                .map(this::convertToDTO)
                .collect(Collectors.toList());

        return new com.forum.dto.PageResponseDTO<>(
                dtos,
                userPage.getTotalPages(),
                userPage.getTotalElements(),
                userPage.getNumber(),
                userPage.getSize()
        );
    }

    @Transactional
    public UserDTO adminCreateUser(Map<String, Object> payload, String currentUsername) {
        String username = (String) payload.get("username");
        String displayName = (String) payload.get("displayName");
        String email = (String) payload.get("email");
        String password = (String) payload.get("password");
        List<String> roles = (List<String>) payload.get("roles");

        if (username == null || username.trim().isEmpty()) {
            throw new IllegalArgumentException("Username is required");
        }
        if (password == null || password.trim().isEmpty()) {
            throw new IllegalArgumentException("Password is required");
        }

        if (userRepository.findByUsername(username).isPresent()) {
            throw new IllegalArgumentException("Username already exists");
        }

        User user = new User();
        user.setUsername(username.trim());
        user.setPassword(passwordEncoder.encode(password));
        user.setDisplayName(displayName != null && !displayName.trim().isEmpty() ? displayName.trim() : username.trim());
        user.setEmail(email != null && !email.trim().isEmpty() ? email.trim() : null);

        if (roles != null && !roles.isEmpty()) {
            Set<String> rolesSet = roles.stream()
                    .map(r -> r.startsWith("ROLE_") ? r : "ROLE_" + r)
                    .collect(Collectors.toSet());

            User currentUser = userRepository.findByUsername(currentUsername).orElseThrow();
            if (!currentUser.getRoles().contains(Constants.ROLE_SUPER_ADMIN)) {
                rolesSet.remove(Constants.ROLE_ADMIN);
                rolesSet.remove(Constants.ROLE_SUPER_ADMIN);
                if (rolesSet.isEmpty()) {
                    rolesSet.add(Constants.ROLE_USER);
                }
            }
            user.setRoles(rolesSet);
        } else {
            user.setRoles(Set.of(Constants.ROLE_USER));
        }

        user.setAvatar(getRandomColor());

        User saved = userRepository.save(user);
        return convertToDTO(saved);
    }

    @Transactional
    public UserDTO adminUpdateUser(Long id, Map<String, Object> payload, String currentUsername) {
        User user = userRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("User not found"));

        Set<String> oldRoles = Set.copyOf(user.getRoles());
        String userEmail = user.getEmail();

        User currentUser = userRepository.findByUsername(currentUsername).orElseThrow();
        boolean isSuperAdmin = currentUser.getRoles().contains(Constants.ROLE_SUPER_ADMIN);

        if (!isSuperAdmin) {
            boolean targetIsAdmin = user.getRoles().contains(Constants.ROLE_ADMIN);
            boolean targetIsSuperAdmin = user.getRoles().contains(Constants.ROLE_SUPER_ADMIN);
            if (targetIsAdmin || targetIsSuperAdmin) {
                throw new IllegalStateException("Access denied: cannot modify other administrators");
            }
        }

        String displayName = (String) payload.get("displayName");
        String email = (String) payload.get("email");
        String password = (String) payload.get("password");
        List<String> roles = (List<String>) payload.get("roles");

        if (displayName != null) {
            user.setDisplayName(displayName.trim().isEmpty() ? user.getUsername() : displayName.trim());
        }
        if (email != null) {
            user.setEmail(email.trim().isEmpty() ? null : email.trim());
        }
        if (password != null && !password.trim().isEmpty()) {
            user.setPassword(passwordEncoder.encode(password));
        }

        if (roles != null) {
            Set<String> rolesSet = roles.stream()
                    .map(r -> r.startsWith("ROLE_") ? r : "ROLE_" + r)
                    .collect(Collectors.toSet());

            if (!isSuperAdmin) {
                rolesSet.remove(Constants.ROLE_ADMIN);
                rolesSet.remove(Constants.ROLE_SUPER_ADMIN);
                if (rolesSet.isEmpty()) {
                    rolesSet.add(Constants.ROLE_USER);
                }
            }
            user.setRoles(rolesSet);
        }

        User saved = userRepository.save(user);
        Set<String> newRoles = saved.getRoles();

        if (userEmail != null && !userEmail.trim().isEmpty()) {
            boolean wasNonOfficial = oldRoles.contains(Constants.ROLE_NON_OFFICIAL_USER);
            boolean wasOfficial = oldRoles.contains(Constants.ROLE_USER);

            boolean isNonOfficial = newRoles.contains(Constants.ROLE_NON_OFFICIAL_USER);
            boolean isOfficial = newRoles.contains(Constants.ROLE_USER);

            if (wasNonOfficial && isOfficial) {
                emailService.sendEmailAsync(userEmail,
                    "Tài khoản của bạn đã được phê duyệt",
                    "Tài khoản của bạn đã được quản trị viên phê duyệt. Hãy tải lại trang hoặc đăng nhập lại để cập nhật sự thay đổi.");
            } else if (wasOfficial && isNonOfficial) {
                emailService.sendEmailAsync(userEmail,
                    "Thay đổi trạng thái tài khoản",
                    "Tài khoản của bạn đã bị tước quyền thành viên. Hãy liên lạc quản trị viên để biết thêm thông tin chi tiết!");
            }
        }

        return convertToDTO(saved);
    }

    @Transactional
    public void adminDeleteUser(Long id, String currentUsername) {
        User user = userRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("User not found"));

        User currentUser = userRepository.findByUsername(currentUsername).orElseThrow();
        boolean isSuperAdmin = currentUser.getRoles().contains(Constants.ROLE_SUPER_ADMIN);

        if (!isSuperAdmin) {
            boolean targetIsAdmin = user.getRoles().contains(Constants.ROLE_ADMIN);
            boolean targetIsSuperAdmin = user.getRoles().contains(Constants.ROLE_SUPER_ADMIN);
            if (targetIsAdmin || targetIsSuperAdmin) {
                throw new IllegalStateException("Access denied: cannot delete other administrators");
            }
        }

        if (user.getId().equals(currentUser.getId())) {
            throw new IllegalArgumentException("Cannot delete yourself");
        }

        // Clean up associations to prevent foreign key constraint violations
        entityManager.createQuery("DELETE FROM ThreadSubscription ts WHERE ts.user.id = :userId")
                .setParameter("userId", id)
                .executeUpdate();

        entityManager.createQuery("DELETE FROM Notification n WHERE n.recipient.id = :userId OR n.actor.id = :userId")
                .setParameter("userId", id)
                .executeUpdate();

        entityManager.createQuery("DELETE FROM Reaction r WHERE r.user.id = :userId")
                .setParameter("userId", id)
                .executeUpdate();

        entityManager.createQuery("DELETE FROM PollVote pv WHERE pv.user.id = :userId")
                .setParameter("userId", id)
                .executeUpdate();

        entityManager.createQuery("DELETE FROM ConversationParticipant cp WHERE cp.user.id = :userId")
                .setParameter("userId", id)
                .executeUpdate();

        // Clean up reactions on the conversation messages sent by this user to prevent FK constraint violation
        entityManager.createQuery("DELETE FROM Reaction r WHERE r.conversationMessage.id IN (SELECT cm.id FROM ConversationMessage cm WHERE cm.sender.id = :userId)")
                .setParameter("userId", id)
                .executeUpdate();

        // Clean up notifications pointing to the conversation messages sent by this user to prevent FK constraint violation
        entityManager.createQuery("DELETE FROM Notification n WHERE n.conversationMessage.id IN (SELECT cm.id FROM ConversationMessage cm WHERE cm.sender.id = :userId)")
                .setParameter("userId", id)
                .executeUpdate();

        entityManager.createQuery("DELETE FROM ConversationMessage cm WHERE cm.sender.id = :userId")
                .setParameter("userId", id)
                .executeUpdate();

        entityManager.createQuery("UPDATE Conversation c SET c.creator = null WHERE c.creator.id = :userId")
                .setParameter("userId", id)
                .executeUpdate();

        entityManager.createQuery("UPDATE Thread t SET t.author = null WHERE t.author.id = :userId")
                .setParameter("userId", id)
                .executeUpdate();

        entityManager.createQuery("UPDATE Post p SET p.author = null WHERE p.author.id = :userId")
                .setParameter("userId", id)
                .executeUpdate();

        userRepository.delete(user);
    }

    @Transactional
    public UserDTO updateMyAvatar(String username, String avatarUrl) {
        User user = userRepository.findByUsername(username)
                .orElseThrow(() -> new IllegalArgumentException("User not found"));
        user.setAvatar(avatarUrl);
        User saved = userRepository.save(user);
        com.forum.service.ThreadService.clearAllCaches();
        return convertToDTO(saved);
    }

    @Transactional
    public UserDTO updateMyBanner(String username, String bannerUrl) {
        User user = userRepository.findByUsername(username)
                .orElseThrow(() -> new IllegalArgumentException("User not found"));
        user.setProfileBanner(bannerUrl);
        User saved = userRepository.save(user);
        com.forum.service.ThreadService.clearAllCaches();
        return convertToDTO(saved);
    }

    @Transactional
    public void updateLastActive(String username) {
        userRepository.findByUsername(username).ifPresent(user -> {
            user.setLastActiveAt(java.time.LocalDateTime.now());
            userRepository.save(user);
        });
    }

    private UserDTO convertToDTO(User user) {
        UserDTO dto = new UserDTO();
        dto.setId(user.getId());
        dto.setUsername(user.getUsername());
        dto.setDisplayName(user.getDisplayName());
        dto.setEmail(user.getEmail());
        dto.setAvatar(user.getAvatar());
        dto.setProfileBanner(user.getProfileBanner());
        dto.setCreatedAt(user.getCreatedAt());
        dto.setLastActiveAt(user.getLastActiveAt());
        dto.setRoles(user.getRoles());
        return dto;
    }

    private String getRandomColor() {
        int hue = new java.util.Random().nextInt(360);
        return String.format("hsl(%d, 70%%, 45%%)", hue);
    }

    private static String removeDiacritics(String str) {
        if (str == null) {
            return "";
        }
        String nfdNormalizedString = Normalizer.normalize(str, Normalizer.Form.NFD);
        Pattern pattern = Pattern.compile("\\p{InCombiningDiacriticalMarks}+");
        String result = pattern.matcher(nfdNormalizedString).replaceAll("");
        result = result.replace('đ', 'd').replace('Đ', 'D');
        return result.toLowerCase();
    }
}

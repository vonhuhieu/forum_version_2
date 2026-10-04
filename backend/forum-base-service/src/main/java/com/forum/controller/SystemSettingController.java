package com.forum.controller;

import com.forum.dto.ResponseDTO;
import com.forum.entity.SystemSetting;
import com.forum.service.SystemSettingService;
import com.forum.utils.Constants;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/settings")
@RequiredArgsConstructor
public class SystemSettingController {
    private final SystemSettingService systemSettingService;
    private final com.forum.service.moderation.ProfanityFilterService profanityFilterService;

    @GetMapping("/public")
    public ResponseEntity<ResponseDTO<Map<String, Object>>> getPublicSettings() {
        Map<String, Object> settings = new HashMap<>();
        
        int threadLimit = Integer.parseInt(systemSettingService.getSetting(
                Constants.SETTING_THREAD_EDIT_LIMIT_MINUTES, 
                Constants.DEFAULT_THREAD_EDIT_LIMIT_MINUTES));
        settings.put(Constants.SETTING_THREAD_EDIT_LIMIT_MINUTES, threadLimit);
        
        int postLimit = Integer.parseInt(systemSettingService.getSetting(
                Constants.SETTING_POST_EDIT_LIMIT_MINUTES, 
                Constants.DEFAULT_POST_EDIT_LIMIT_MINUTES));
        settings.put(Constants.SETTING_POST_EDIT_LIMIT_MINUTES, postLimit);
        
        int convoLimit = Integer.parseInt(systemSettingService.getSetting(
                Constants.SETTING_CONVERSATION_EDIT_LIMIT_MINUTES, 
                Constants.DEFAULT_CONVERSATION_EDIT_LIMIT_MINUTES));
        settings.put(Constants.SETTING_CONVERSATION_EDIT_LIMIT_MINUTES, convoLimit);
        
        int convoReplyLimit = Integer.parseInt(systemSettingService.getSetting(
                Constants.SETTING_CONVERSATION_REPLY_EDIT_LIMIT_MINUTES, 
                Constants.DEFAULT_CONVERSATION_REPLY_EDIT_LIMIT_MINUTES));
        settings.put(Constants.SETTING_CONVERSATION_REPLY_EDIT_LIMIT_MINUTES, convoReplyLimit);
        
        settings.put(Constants.SETTING_POST_BUTTON_HOME, systemSettingService.getSetting(
                Constants.SETTING_POST_BUTTON_HOME, Constants.DEFAULT_POST_BUTTON_HOME));
        settings.put(Constants.SETTING_POST_BUTTON_LATEST, systemSettingService.getSetting(
                Constants.SETTING_POST_BUTTON_LATEST, Constants.DEFAULT_POST_BUTTON_LATEST));
        settings.put(Constants.SETTING_POST_BUTTON_PINNED, systemSettingService.getSetting(
                Constants.SETTING_POST_BUTTON_PINNED, Constants.DEFAULT_POST_BUTTON_PINNED));
        settings.put(Constants.SETTING_POST_BUTTON_CATEGORY, systemSettingService.getSetting(
                Constants.SETTING_POST_BUTTON_CATEGORY, Constants.DEFAULT_POST_BUTTON_CATEGORY));

        return ResponseEntity.ok(ResponseDTO.success(settings));
    }

    @GetMapping
    public ResponseEntity<ResponseDTO<Map<String, String>>> getAllSettings() {
        // Cần đảm bảo các giá trị mặc định được khởi tạo nếu chưa có trong DB
        systemSettingService.getSetting(
                Constants.SETTING_THREAD_EDIT_LIMIT_MINUTES, 
                Constants.DEFAULT_THREAD_EDIT_LIMIT_MINUTES);
        systemSettingService.getSetting(
                Constants.SETTING_POST_EDIT_LIMIT_MINUTES, 
                Constants.DEFAULT_POST_EDIT_LIMIT_MINUTES);
        systemSettingService.getSetting(
                Constants.SETTING_CONVERSATION_EDIT_LIMIT_MINUTES, 
                Constants.DEFAULT_CONVERSATION_EDIT_LIMIT_MINUTES);
        systemSettingService.getSetting(
                Constants.SETTING_CONVERSATION_REPLY_EDIT_LIMIT_MINUTES, 
                Constants.DEFAULT_CONVERSATION_REPLY_EDIT_LIMIT_MINUTES);
        systemSettingService.getSetting(
                Constants.SETTING_POST_BUTTON_HOME, 
                Constants.DEFAULT_POST_BUTTON_HOME);
        systemSettingService.getSetting(
                Constants.SETTING_POST_BUTTON_LATEST, 
                Constants.DEFAULT_POST_BUTTON_LATEST);
        systemSettingService.getSetting(
                Constants.SETTING_POST_BUTTON_PINNED, 
                Constants.DEFAULT_POST_BUTTON_PINNED);
        systemSettingService.getSetting(
                Constants.SETTING_POST_BUTTON_CATEGORY, 
                Constants.DEFAULT_POST_BUTTON_CATEGORY);
        
        Map<String, String> map = systemSettingService.getAllSettings().stream()
                .collect(Collectors.toMap(SystemSetting::getSettingKey, SystemSetting::getSettingValue));
        return ResponseEntity.ok(ResponseDTO.success(map));
    }

    @PutMapping
    public ResponseEntity<ResponseDTO<Void>> updateSettings(@RequestBody Map<String, String> payload) {
        payload.forEach(systemSettingService::updateSetting);
        if (payload.keySet().stream().anyMatch(k -> k.startsWith("profanity_"))) {
            profanityFilterService.reloadRules();
        }
        return ResponseEntity.ok(ResponseDTO.success(null));
    }

    /**
     * Lấy cấu hình bộ lọc từ cấm (Bao gồm từ khóa mặc định từ JSON và từ khóa tùy biến từ DB)
     */
    @GetMapping("/moderation")
    @org.springframework.security.access.prepost.PreAuthorize("hasAnyRole('ROLE_ADMIN', 'ROLE_SUPER_ADMIN')")
    public ResponseEntity<ResponseDTO<Map<String, Object>>> getModerationConfig() {
        return ResponseEntity.ok(ResponseDTO.success(profanityFilterService.getModerationConfigSummary()));
    }

    /**
     * Cập nhật từ khóa cấm tùy biến và trạng thái bộ lọc
     */
    @PutMapping("/moderation")
    @org.springframework.security.access.prepost.PreAuthorize("hasAnyRole('ROLE_ADMIN', 'ROLE_SUPER_ADMIN')")
    public ResponseEntity<ResponseDTO<Map<String, Object>>> updateModerationConfig(@RequestBody Map<String, String> payload) {
        payload.forEach(systemSettingService::updateSetting);
        profanityFilterService.reloadRules();
        return ResponseEntity.ok(ResponseDTO.success(profanityFilterService.getModerationConfigSummary()));
    }

    /**
     * Endpoint kiểm tra thử nghiệm văn bản với bộ lọc hiện hành
     */
    @PostMapping("/moderation/test")
    @org.springframework.security.access.prepost.PreAuthorize("hasAnyRole('ROLE_ADMIN', 'ROLE_SUPER_ADMIN')")
    public ResponseEntity<ResponseDTO<com.forum.service.moderation.ProfanityFilterService.ValidationResult>> testModeration(@RequestBody Map<String, String> request) {
        String text = request.getOrDefault("content", "");
        var result = profanityFilterService.validateAndClean(text);
        return ResponseEntity.ok(ResponseDTO.success(result));
    }
}

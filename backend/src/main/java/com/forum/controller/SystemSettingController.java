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

    @GetMapping("/public")
    public ResponseEntity<ResponseDTO<Map<String, Object>>> getPublicSettings() {
        Map<String, Object> settings = new HashMap<>();
        
        int threadLimit = Integer.parseInt(systemSettingService.getSetting(
                Constants.SETTING_THREAD_EDIT_LIMIT_MINUTES, 
                Constants.DEFAULT_THREAD_EDIT_LIMIT_MINUTES));
        settings.put(Constants.SETTING_THREAD_EDIT_LIMIT_MINUTES, threadLimit);
        
        return ResponseEntity.ok(ResponseDTO.success(settings));
    }

    @GetMapping
    public ResponseEntity<ResponseDTO<Map<String, String>>> getAllSettings() {
        // Cần đảm bảo các giá trị mặc định được khởi tạo nếu chưa có trong DB
        systemSettingService.getSetting(
                Constants.SETTING_THREAD_EDIT_LIMIT_MINUTES, 
                Constants.DEFAULT_THREAD_EDIT_LIMIT_MINUTES);
        
        Map<String, String> map = systemSettingService.getAllSettings().stream()
                .collect(Collectors.toMap(SystemSetting::getSettingKey, SystemSetting::getSettingValue));
        return ResponseEntity.ok(ResponseDTO.success(map));
    }

    @PutMapping
    public ResponseEntity<ResponseDTO<Void>> updateSettings(@RequestBody Map<String, String> payload) {
        payload.forEach(systemSettingService::updateSetting);
        return ResponseEntity.ok(ResponseDTO.success(null));
    }
}

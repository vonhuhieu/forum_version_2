package com.forum.service;

import com.forum.entity.SystemSetting;
import com.forum.repository.SystemSettingRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Service
@RequiredArgsConstructor
public class SystemSettingService {
    private final SystemSettingRepository systemSettingRepository;
    private final Map<String, String> cache = new ConcurrentHashMap<>();

    public String getSetting(String key, String defaultValue) {
        return cache.computeIfAbsent(key, k -> 
            systemSettingRepository.findById(k)
                .map(SystemSetting::getSettingValue)
                .orElseGet(() -> {
                    SystemSetting setting = new SystemSetting(k, defaultValue, "Cấu hình hệ thống");
                    systemSettingRepository.save(setting);
                    return defaultValue;
                })
        );
    }

    public List<SystemSetting> getAllSettings() {
        return systemSettingRepository.findAll();
    }

    @Transactional
    public void updateSetting(String key, String value) {
        SystemSetting setting = systemSettingRepository.findById(key)
                .orElse(new SystemSetting(key, value, "Cấu hình hệ thống"));
        setting.setSettingValue(value);
        systemSettingRepository.save(setting);
        cache.put(key, value);
    }

    public void clearCache() {
        cache.clear();
    }
}

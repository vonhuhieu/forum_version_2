package com.forum.service.moderation;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.forum.service.SystemSettingService;
import jakarta.annotation.PostConstruct;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.ahocorasick.trie.Emit;
import org.ahocorasick.trie.Trie;
import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Service;

import java.io.InputStream;
import java.util.*;
import java.util.concurrent.atomic.AtomicReference;

@Slf4j
@Service
@RequiredArgsConstructor
public class ProfanityFilterService {

    private final SystemSettingService systemSettingService;
    private final ObjectMapper objectMapper = new ObjectMapper();

    public enum SeverityLevel {
        CRITICAL, // Bị chặn lập tức (an ninh, chính trị, pháp luật)
        SEVERE,   // Bị chặn lập tức (bạo lực, khiêu dâm nặng)
        OFFENSIVE,// Bị che thành *** (chửi thề, tục tĩu)
        FRAUD     // Chuyển duyệt / Cảnh báo (cờ bạc, lừa đảo)
    }

    @Getter
    public static class ValidationResult {
        private final boolean blocked;
        private final SeverityLevel highestSeverity;
        private final String maskedContent;
        private final String violationMessage;
        private final List<String> detectedWords;

        public ValidationResult(boolean blocked, SeverityLevel highestSeverity, String maskedContent, String violationMessage, List<String> detectedWords) {
            this.blocked = blocked;
            this.highestSeverity = highestSeverity;
            this.maskedContent = maskedContent;
            this.violationMessage = violationMessage;
            this.detectedWords = detectedWords;
        }

        public static ValidationResult valid(String content) {
            return new ValidationResult(false, null, content, null, Collections.emptyList());
        }

        public static ValidationResult masked(String maskedContent, List<String> detectedWords) {
            return new ValidationResult(false, SeverityLevel.OFFENSIVE, maskedContent, null, detectedWords);
        }

        public static ValidationResult blocked(SeverityLevel severity, String message, List<String> detectedWords) {
            return new ValidationResult(true, severity, null, message, detectedWords);
        }
    }

    private static class KeywordRule {
        String keyword;
        SeverityLevel level;

        KeywordRule(String keyword, SeverityLevel level) {
            this.keyword = keyword.toLowerCase().trim();
            this.level = level;
        }
    }

    private final AtomicReference<Trie> trieHolder = new AtomicReference<>();
    private final AtomicReference<Map<String, SeverityLevel>> keywordMapHolder = new AtomicReference<>(new HashMap<>());

    @PostConstruct
    public void init() {
        reloadRules();
    }

    public synchronized void reloadRules() {
        try {
            Map<String, SeverityLevel> keywordMap = new HashMap<>();
            
            // 1. Tải từ file profanity-rules.json từ classpath
            ClassPathResource resource = new ClassPathResource("moderation/profanity-rules.json");
            if (resource.exists()) {
                try (InputStream is = resource.getInputStream()) {
                    Map<String, List<String>> fileRules = objectMapper.readValue(is, new TypeReference<>() {});
                    if (fileRules != null) {
                        registerKeywords(keywordMap, fileRules.get("critical"), SeverityLevel.CRITICAL);
                        registerKeywords(keywordMap, fileRules.get("severe"), SeverityLevel.SEVERE);
                        registerKeywords(keywordMap, fileRules.get("offensive"), SeverityLevel.OFFENSIVE);
                        registerKeywords(keywordMap, fileRules.get("fraud"), SeverityLevel.FRAUD);
                    }
                }
            } else {
                log.warn("Không tìm thấy tệp classpath: moderation/profanity-rules.json");
            }

            // 2. Tải thêm từ khóa cấm tùy biến từ system_settings
            String customKeywords = systemSettingService.getSetting("profanity_custom_keywords", "");
            if (customKeywords != null && !customKeywords.trim().isEmpty()) {
                String[] parts = customKeywords.split("[,;\\n]");
                for (String part : parts) {
                    String kw = part.trim().toLowerCase();
                    if (!kw.isEmpty()) {
                        keywordMap.put(kw, SeverityLevel.OFFENSIVE);
                    }
                }
            }

            // 3. Xây dựng Trie Aho-Corasick với cấu hình case-insensitive & match whole words if possible
            Trie.TrieBuilder builder = Trie.builder()
                    .ignoreCase()
                    .ignoreOverlaps();

            for (String kw : keywordMap.keySet()) {
                builder.addKeyword(kw);
            }

            trieHolder.set(builder.build());
            keywordMapHolder.set(keywordMap);
            log.info("Đã nạp thành công bộ lọc từ cấm với {} từ khóa.", keywordMap.size());
        } catch (Exception e) {
            log.error("Lỗi khi nạp danh sách từ cấm moderation: {}", e.getMessage(), e);
        }
    }

    private void registerKeywords(Map<String, SeverityLevel> map, List<String> list, SeverityLevel level) {
        if (list == null) return;
        for (String item : list) {
            if (item != null) {
                String kw = item.trim().toLowerCase();
                if (!kw.isEmpty()) {
                    map.put(kw, level);
                }
            }
        }
    }

    public boolean isFilterEnabled() {
        return "true".equalsIgnoreCase(systemSettingService.getSetting("profanity_filter_enabled", "true"));
    }

    /**
     * Kiểm tra nội dung toàn diện và làm sạch nếu cần
     */
    public ValidationResult validateAndClean(String rawContent) {
        if (rawContent == null || rawContent.trim().isEmpty() || !isFilterEnabled()) {
            return ValidationResult.valid(rawContent);
        }

        Trie trie = trieHolder.get();
        Map<String, SeverityLevel> map = keywordMapHolder.get();
        if (trie == null || map.isEmpty()) {
            return ValidationResult.valid(rawContent);
        }

        Collection<Emit> emits = trie.parseText(rawContent);
        if (emits.isEmpty()) {
            return ValidationResult.valid(rawContent);
        }

        List<String> detectedCritical = new ArrayList<>();
        List<String> detectedSevere = new ArrayList<>();
        List<String> detectedFraud = new ArrayList<>();
        List<String> detectedOffensive = new ArrayList<>();

        for (Emit emit : emits) {
            String kw = emit.getKeyword().toLowerCase();
            SeverityLevel level = map.get(kw);
            if (level == null) level = SeverityLevel.OFFENSIVE;

            switch (level) {
                case CRITICAL -> detectedCritical.add(kw);
                case SEVERE -> detectedSevere.add(kw);
                case FRAUD -> detectedFraud.add(kw);
                case OFFENSIVE -> detectedOffensive.add(kw);
            }
        }

        // Cấp 1: Vi phạm pháp luật nặng -> Chặn ngay
        if (!detectedCritical.isEmpty()) {
            return ValidationResult.blocked(
                    SeverityLevel.CRITICAL,
                    "Nội dung vi phạm nghiêm trọng quy định pháp luật và an ninh mạng (Nghị định 147/2024/NĐ-CP). Bài viết bị từ chối.",
                    detectedCritical
            );
        }

        // Cấp 2: Bạo lực, khiêu dâm nặng -> Chặn ngay
        if (!detectedSevere.isEmpty()) {
            return ValidationResult.blocked(
                    SeverityLevel.SEVERE,
                    "Nội dung chứa từ ngữ khiêu dâm, bạo lực hoặc không phù hợp với tiêu chuẩn cộng đồng.",
                    detectedSevere
            );
        }

        // Cấp 4: Cờ bạc, lừa đảo -> Chặn hoặc cảnh báo
        if (!detectedFraud.isEmpty()) {
            return ValidationResult.blocked(
                    SeverityLevel.FRAUD,
                    "Nội dung chứa từ khóa liên quan đến cờ bạc, cá cược hoặc lừa đảo tài chính.",
                    detectedFraud
            );
        }

        // Cấp 3: Từ ngữ thô tục -> Tự động che dấu sao ***
        if (!detectedOffensive.isEmpty()) {
            String masked = maskEmits(rawContent, emits);
            return ValidationResult.masked(masked, detectedOffensive);
        }

        return ValidationResult.valid(rawContent);
    }

    private String maskEmits(String text, Collection<Emit> emits) {
        StringBuilder sb = new StringBuilder(text);
        // Sắp xếp các emits từ cuối chuỗi về đầu chuỗi để không bị lệch index khi replace
        List<Emit> emitList = new ArrayList<>(emits);
        emitList.sort((a, b) -> Integer.compare(b.getStart(), a.getStart()));

        for (Emit emit : emitList) {
            int start = emit.getStart();
            int end = emit.getEnd() + 1;
            // Thay thế bằng ***
            sb.replace(start, end, "***");
        }
        return sb.toString();
    }
}

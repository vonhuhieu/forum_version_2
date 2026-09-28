package com.forum.service.moderation;

import com.forum.service.SystemSettingService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;

class ProfanityFilterServiceTest {

    private ProfanityFilterService filterService;
    private SystemSettingService systemSettingService;

    @BeforeEach
    void setUp() {
        systemSettingService = Mockito.mock(SystemSettingService.class);
        Mockito.when(systemSettingService.getSetting(eq("profanity_filter_enabled"), anyString())).thenReturn("true");
        Mockito.when(systemSettingService.getSetting(eq("profanity_custom_keywords"), anyString())).thenReturn("");

        filterService = new ProfanityFilterService(systemSettingService);
        filterService.reloadRules();
    }

    @Test
    void testCleanContentPasses() {
        var result = filterService.validateAndClean("Hôm nay trời đẹp và thảo luận công nghệ rất bổ ích.");
        assertFalse(result.isBlocked());
        assertEquals("Hôm nay trời đẹp và thảo luận công nghệ rất bổ ích.", result.getMaskedContent());
    }

    @Test
    void testCriticalWordsBlocked() {
        var result = filterService.validateAndClean("Kêu gọi bạo động và lật đổ chính quyền ngay!");
        assertTrue(result.isBlocked());
        assertEquals(ProfanityFilterService.SeverityLevel.CRITICAL, result.getHighestSeverity());
    }

    @Test
    void testSevereWordsBlocked() {
        var result = filterService.validateAndClean("Xem clip sex tại đây...");
        assertTrue(result.isBlocked());
        assertEquals(ProfanityFilterService.SeverityLevel.SEVERE, result.getHighestSeverity());
    }

    @Test
    void testOffensiveWordsMasked() {
        var result = filterService.validateAndClean("Thằng này nói chuyện vcl thật đấy");
        assertFalse(result.isBlocked());
        assertTrue(result.getMaskedContent().contains("***"));
        assertFalse(result.getMaskedContent().contains("vcl"));
    }

    @Test
    void testFraudWordsBlocked() {
        var result = filterService.validateAndClean("Vào nhóm tài xỉu kiếm tiền triệu mỗi ngày");
        assertTrue(result.isBlocked());
        assertEquals(ProfanityFilterService.SeverityLevel.FRAUD, result.getHighestSeverity());
    }
}

package com.forum.config;

import com.forum.entity.Label;
import com.forum.entity.Menu;
import com.forum.entity.TitleType;
import com.forum.entity.User;
import com.forum.entity.UserTitle;
import com.forum.repository.LabelRepository;
import com.forum.repository.MenuRepository;
import com.forum.repository.UserRepository;
import com.forum.repository.UserTitleRepository;
import com.forum.service.SystemSettingService;
import com.forum.utils.Constants;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.CommandLineRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.Set;
import java.util.UUID;

@Component
@RequiredArgsConstructor
@Slf4j
public class NewsBotDataInitializer implements CommandLineRunner {

    private final MenuRepository menuRepository;
    private final LabelRepository labelRepository;
    private final UserTitleRepository userTitleRepository;
    private final UserRepository userRepository;
    private final SystemSettingService systemSettingService;
    private final PasswordEncoder passwordEncoder;

    @Override
    @Transactional
    public void run(String... args) {
        try {
            initMenu();
            initLabel();
            UserTitle newsTitle = initUserTitle();
            initBotUser(newsTitle);
            initSystemSettings();
            log.info("Khởi tạo dữ liệu nền tảng cho Điểm Tin & Bot hoàn tất.");
        } catch (Exception e) {
            log.error("Lỗi khi khởi tạo dữ liệu Điểm Tin: {}", e.getMessage(), e);
        }
    }

    private void initMenu() {
        // Kiểm tra menu /diem-tin hoặc /leu-bao
        Menu menu = menuRepository.findByUrl("/diem-tin")
                .or(() -> menuRepository.findByUrl("/leu-bao"))
                .or(() -> menuRepository.findById(2L))
                .orElse(null);

        if (menu != null) {
            menu.setTitle("Điểm tin");
            menu.setUrl("/diem-tin");
            menu.setActive(true);
            menuRepository.save(menu);
            log.info("Đã cập nhật Menu Điểm tin (id: {}) thành công.", menu.getId());
        } else {
            Menu newMenu = new Menu(null, "Điểm tin", "/diem-tin", 2, true);
            menuRepository.save(newMenu);
            log.info("Đã tạo mới Menu Điểm tin.");
        }
    }

    private Label initLabel() {
        return labelRepository.findByName("Điểm Tin").orElseGet(() -> {
            Label label = new Label(null, "Điểm Tin", "#1a73e8", "#ffffff", "transparent", false);
            Label saved = labelRepository.save(label);
            log.info("Đã tạo Label Điểm Tin (id: {}).", saved.getId());
            return saved;
        });
    }

    private UserTitle initUserTitle() {
        return userTitleRepository.findByName("Biên Tập Viên Tin Tức").orElseGet(() -> {
            UserTitle title = new UserTitle(null, "Biên Tập Viên Tin Tức", TitleType.CUSTOM_ASSIGNABLE, 0, "Biên tập viên tổng hợp tin tức chính thống", true);
            UserTitle saved = userTitleRepository.save(title);
            log.info("Đã tạo UserTitle Biên Tập Viên Tin Tức (id: {}).", saved.getId());
            return saved;
        });
    }

    private void initBotUser(UserTitle title) {
        if (userRepository.findByUsername("diemtinbot").isEmpty()) {
            User bot = new User();
            bot.setUsername("diemtinbot");
            bot.setPassword(passwordEncoder.encode(UUID.randomUUID().toString()));
            bot.setDisplayName("Điểm Tin Bot 🤖");
            bot.setEmail("diemtinbot@hoptacxavuive.com");
            bot.setAvatar("https://api.dicebear.com/7.x/bottts/svg?seed=diemtinbot");
            bot.setRoles(Set.of(Constants.ROLE_USER));
            bot.setAssignedTitle(title);
            bot.setCreatedAt(LocalDateTime.now());
            bot.setLastActiveAt(LocalDateTime.now());
            userRepository.save(bot);
            log.info("Đã tạo User diemtinbot thành công.");
        }
    }

    private void initSystemSettings() {
        initSettingIfAbsent("news_bot_enabled", "true");
        initSettingIfAbsent("news_bot_cron", "0 0 7,12,18 * * *");
        initSettingIfAbsent("news_bot_max_daily_posts", "8");
        initSettingIfAbsent("profanity_filter_enabled", "true");
        initSettingIfAbsent("profanity_custom_keywords", "");
    }

    private void initSettingIfAbsent(String key, String defaultValue) {
        systemSettingService.getSetting(key, defaultValue);
    }
}

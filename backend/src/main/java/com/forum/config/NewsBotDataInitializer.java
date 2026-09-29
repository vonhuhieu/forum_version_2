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
        Menu menu = menuRepository.findByUrl(Constants.MENU_NEWS_URL)
                .or(() -> menuRepository.findByUrl(Constants.MENU_NEWS_OLD_URL))
                .or(() -> menuRepository.findById(Constants.MENU_NEWS_ID))
                .orElse(null);

        if (menu != null) {
            menu.setTitle(Constants.MENU_NEWS_TITLE);
            menu.setUrl(Constants.MENU_NEWS_URL);
            menu.setActive(true);
            menuRepository.save(menu);
            log.info("Đã cập nhật Menu Điểm tin (id: {}) thành công.", menu.getId());
        } else {
            Menu newMenu = new Menu(null, Constants.MENU_NEWS_TITLE, Constants.MENU_NEWS_URL, Constants.MENU_NEWS_ORDER, true);
            menuRepository.save(newMenu);
            log.info("Đã tạo mới Menu Điểm tin.");
        }
    }

    private Label initLabel() {
        return labelRepository.findByName(Constants.NEWS_LABEL_NAME).orElseGet(() -> {
            Label label = new Label(null, Constants.NEWS_LABEL_NAME, Constants.NEWS_LABEL_COLOR, Constants.NEWS_LABEL_TEXT_COLOR, Constants.NEWS_LABEL_BORDER_COLOR, false);
            Label saved = labelRepository.save(label);
            log.info("Đã tạo Label Điểm Tin (id: {}).", saved.getId());
            return saved;
        });
    }

    private UserTitle initUserTitle() {
        return userTitleRepository.findByName(Constants.NEWS_BOT_TITLE_NAME).orElseGet(() -> {
            UserTitle title = new UserTitle(null, Constants.NEWS_BOT_TITLE_NAME, TitleType.CUSTOM_ASSIGNABLE, 0, Constants.NEWS_BOT_TITLE_DESCRIPTION, true);
            UserTitle saved = userTitleRepository.save(title);
            log.info("Đã tạo UserTitle Biên Tập Viên Tin Tức (id: {}).", saved.getId());
            return saved;
        });
    }

    private void initBotUser(UserTitle title) {
        if (userRepository.findByUsername(Constants.NEWS_BOT_USERNAME).isEmpty()) {
            User bot = new User();
            bot.setUsername(Constants.NEWS_BOT_USERNAME);
            bot.setPassword(passwordEncoder.encode(UUID.randomUUID().toString()));
            bot.setDisplayName(Constants.NEWS_BOT_DISPLAY_NAME);
            bot.setEmail(Constants.NEWS_BOT_EMAIL);
            bot.setAvatar(Constants.NEWS_BOT_AVATAR_BASE_URL + Constants.NEWS_BOT_USERNAME);
            bot.setRoles(Set.of(Constants.ROLE_USER));
            bot.setAssignedTitle(title);
            bot.setCreatedAt(LocalDateTime.now());
            bot.setLastActiveAt(LocalDateTime.now());
            userRepository.save(bot);
            log.info("Đã tạo User diemtinbot thành công.");
        }
    }

    private void initSystemSettings() {
        initSettingIfAbsent(Constants.SETTING_NEWS_BOT_ENABLED, Constants.DEFAULT_NEWS_BOT_ENABLED);
        initSettingIfAbsent(Constants.SETTING_NEWS_BOT_CRON, Constants.DEFAULT_NEWS_BOT_CRON);
        initSettingIfAbsent(Constants.SETTING_NEWS_BOT_MAX_DAILY_POSTS, Constants.DEFAULT_NEWS_BOT_MAX_DAILY_POSTS);
        initSettingIfAbsent(Constants.SETTING_PROFANITY_FILTER_ENABLED, Constants.DEFAULT_PROFANITY_FILTER_ENABLED);
        initSettingIfAbsent(Constants.SETTING_PROFANITY_CUSTOM_KEYWORDS, Constants.DEFAULT_PROFANITY_CUSTOM_KEYWORDS);
    }

    private void initSettingIfAbsent(String key, String defaultValue) {
        systemSettingService.getSetting(key, defaultValue);
    }
}

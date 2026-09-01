package com.forum.config;

import org.springframework.boot.CommandLineRunner;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

@Component
public class DatabaseSchemaPatcher implements CommandLineRunner {

    private final JdbcTemplate jdbcTemplate;

    public DatabaseSchemaPatcher(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    @Override
    public void run(String... args) {
        // 1. Tạo bảng user_titles nếu chưa có
        try {
            jdbcTemplate.execute("""
                CREATE TABLE IF NOT EXISTS user_titles (
                    id BIGINT AUTO_INCREMENT PRIMARY KEY,
                    name VARCHAR(255) NOT NULL,
                    type VARCHAR(50) NOT NULL,
                    min_points INT NULL,
                    description VARCHAR(255) NULL,
                    is_trusted BOOLEAN DEFAULT FALSE
                ) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci
            """);
        } catch (Exception e) {
            System.err.println(">>> Notice on user_titles table patch: " + e.getMessage());
        }

        // 2. Thêm các cột còn thiếu vào bảng users
        String[] userAlterColumns = {
            "ALTER TABLE users ADD COLUMN IF NOT EXISTS assigned_title_id BIGINT NULL",
            "ALTER TABLE users ADD COLUMN IF NOT EXISTS profile_banner VARCHAR(255) NULL",
            "ALTER TABLE users ADD COLUMN IF NOT EXISTS reset_code VARCHAR(255) NULL",
            "ALTER TABLE users ADD COLUMN IF NOT EXISTS reset_code_expiry DATETIME NULL",
            "ALTER TABLE users ADD COLUMN IF NOT EXISTS email_confirmation_token VARCHAR(255) NULL",
            "ALTER TABLE users ADD COLUMN IF NOT EXISTS email_confirmation_expiry DATETIME NULL",
            "ALTER TABLE users ADD COLUMN IF NOT EXISTS last_active_at DATETIME NULL",
            "ALTER TABLE users ADD COLUMN IF NOT EXISTS created_at DATETIME NULL"
        };
        for (String sql : userAlterColumns) {
            try {
                jdbcTemplate.execute(sql);
            } catch (Exception e) {
                System.err.println(">>> Notice on users column patch: " + e.getMessage());
            }
        }

        // 3. Thêm các cột còn thiếu vào bảng threads & posts
        String[] threadPostAlterColumns = {
            "ALTER TABLE threads ADD COLUMN IF NOT EXISTS scope VARCHAR(50) DEFAULT 'PUBLIC'",
            "ALTER TABLE threads ADD COLUMN IF NOT EXISTS attached_images TEXT NULL",
            "ALTER TABLE threads ADD COLUMN IF NOT EXISTS reaction_count INT DEFAULT 0",
            "ALTER TABLE threads ADD COLUMN IF NOT EXISTS locked BOOLEAN DEFAULT FALSE",
            "ALTER TABLE posts ADD COLUMN IF NOT EXISTS attached_images TEXT NULL"
        };
        for (String sql : threadPostAlterColumns) {
            try {
                jdbcTemplate.execute(sql);
            } catch (Exception e) {
                System.err.println(">>> Notice on thread/post column patch: " + e.getMessage());
            }
        }

        // 4. Tạo các bảng hỗ trợ nếu chưa có
        String[] additionalTables = {
            """
            CREATE TABLE IF NOT EXISTS search_history (
                id BIGINT AUTO_INCREMENT PRIMARY KEY,
                user_id BIGINT NOT NULL,
                keyword VARCHAR(255) NOT NULL,
                created_at DATETIME NULL
            ) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci
            """,
            """
            CREATE TABLE IF NOT EXISTS user_follows (
                id BIGINT AUTO_INCREMENT PRIMARY KEY,
                follower_id BIGINT NOT NULL,
                followed_id BIGINT NOT NULL,
                created_at DATETIME NULL
            ) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci
            """,
            """
            CREATE TABLE IF NOT EXISTS thread_subscriptions (
                id BIGINT AUTO_INCREMENT PRIMARY KEY,
                user_id BIGINT NOT NULL,
                thread_id BIGINT NOT NULL,
                created_at DATETIME NULL
            ) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci
            """
        };
        for (String sql : additionalTables) {
            try {
                jdbcTemplate.execute(sql);
            } catch (Exception e) {
                System.err.println(">>> Notice on additional table patch: " + e.getMessage());
            }
        }

        // 5. Cập nhật notifications và display_name
        try {
            jdbcTemplate.execute("ALTER TABLE notifications MODIFY COLUMN type VARCHAR(50)");
            jdbcTemplate.execute("UPDATE users SET display_name = username WHERE display_name IS NULL OR TRIM(display_name) = ''");
            System.out.println(">>> Database schema patched and synchronized successfully.");
        } catch (Exception e) {
            System.err.println(">>> Failed to patch notifications/display_name: " + e.getMessage());
        }
    }
}

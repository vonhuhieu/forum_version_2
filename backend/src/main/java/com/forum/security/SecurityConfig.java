package com.forum.security;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.web.filter.CorsFilter;

@Configuration
@EnableWebSecurity
public class SecurityConfig {

    @Value("${app.cors.allowed-origins}")
    private String allowedOrigins;

    @Autowired
    private JwtAuthenticationFilter jwtAuthenticationFilter;

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        http.csrf(csrf -> csrf.disable())
            .authorizeHttpRequests(auth -> auth
                .requestMatchers("/api/auth/**").permitAll()
                .requestMatchers(org.springframework.http.HttpMethod.GET, "/api/menus", "/api/menus/**").permitAll() 
                .requestMatchers(org.springframework.http.HttpMethod.GET, "/api/statistics", "/api/statistics/**").permitAll() // Cho phép công khai xem thống kê diễn đàn
                .requestMatchers("/uploads/**").permitAll()
                .requestMatchers("/ws/**").permitAll()
                .requestMatchers("/error").permitAll()
                // Cấu hình GET công khai
                .requestMatchers(org.springframework.http.HttpMethod.GET, "/api/categories", "/api/categories/**").permitAll()
                .requestMatchers(org.springframework.http.HttpMethod.GET, "/api/category-groups", "/api/category-groups/**").permitAll()
                .requestMatchers(org.springframework.http.HttpMethod.GET, "/api/threads", "/api/threads/**").permitAll()
                .requestMatchers(org.springframework.http.HttpMethod.GET, "/api/polls", "/api/polls/**").permitAll()
                .requestMatchers(org.springframework.http.HttpMethod.GET, "/api/labels", "/api/labels/**").permitAll()
                .requestMatchers(org.springframework.http.HttpMethod.GET, "/api/posts", "/api/posts/**").permitAll()
                .requestMatchers(org.springframework.http.HttpMethod.GET, "/api/reaction-icons", "/api/reaction-icons/**").permitAll()
                .requestMatchers(org.springframework.http.HttpMethod.GET, "/api/reactions", "/api/reactions/**").permitAll()
                .requestMatchers(org.springframework.http.HttpMethod.POST, "/api/search/reindex").permitAll()
                .requestMatchers(org.springframework.http.HttpMethod.GET, "/api/search", "/api/search/**").permitAll()
                .requestMatchers(org.springframework.http.HttpMethod.GET, "/api/users/by-name/public").permitAll()
                .requestMatchers(org.springframework.http.HttpMethod.GET, "/api/users/search/public").permitAll()
                .requestMatchers(org.springframework.http.HttpMethod.GET, "/api/users/members/**").permitAll()
                .requestMatchers(org.springframework.http.HttpMethod.GET, "/api/settings/public").permitAll()
                // Cấu hình bảo vệ cho tác vụ ADMIN / SUPER_ADMIN
                .requestMatchers("/api/menus/**").hasAnyRole("ADMIN", "SUPER_ADMIN")
                .requestMatchers("/api/categories/**").hasAnyRole("ADMIN", "SUPER_ADMIN")
                .requestMatchers("/api/category-groups/**").hasAnyRole("ADMIN", "SUPER_ADMIN")
                .requestMatchers("/api/labels/**").hasAnyRole("ADMIN", "SUPER_ADMIN")
                .requestMatchers("/api/reaction-icons/**").hasAnyRole("ADMIN", "SUPER_ADMIN")
                .requestMatchers("/api/users/admin/**").hasAnyRole("ADMIN", "SUPER_ADMIN")
                .requestMatchers("/api/settings/**").hasAnyRole("ADMIN", "SUPER_ADMIN")
                .requestMatchers("/api/notifications/**").hasAnyRole("USER", "ADMIN", "SUPER_ADMIN", "NON_OFFICIAL_USER")
                .requestMatchers("/api/conversations/**").hasAnyRole("USER", "ADMIN", "SUPER_ADMIN", "NON_OFFICIAL_USER")
                .requestMatchers("/api/users/search", "/api/users/by-name", "/api/users/me/active").hasAnyRole("USER", "ADMIN", "SUPER_ADMIN", "NON_OFFICIAL_USER")
                // Còn lại yêu cầu đăng nhập và thuộc các nhóm quyền chính thức
                .anyRequest().hasAnyRole("USER", "ADMIN", "SUPER_ADMIN")
            )
            .addFilterBefore(jwtAuthenticationFilter, org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter.class);
        return http.build();
    }

    @Bean
    public CorsFilter corsFilter() {
        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        CorsConfiguration config = new CorsConfiguration();
        config.setAllowCredentials(true);

        if (allowedOrigins != null && !allowedOrigins.trim().isEmpty()) {
            for (String origin : allowedOrigins.split(",")) {
                String trimmed = origin.trim();
                if ("*".equals(trimmed)) {
                    // Wildcard '*' không hoạt động với allowCredentials=true
                    // Dùng allowedOriginPatterns thay thế
                    config.addAllowedOriginPattern("*");
                } else {
                    config.addAllowedOrigin(trimmed);
                }
            }
        } else {
            config.addAllowedOrigin("http://localhost:5173");
        }

        config.addAllowedHeader("*");
        config.addAllowedMethod("*");
        source.registerCorsConfiguration("/**", config);
        return new CorsFilter(source);
    }
}


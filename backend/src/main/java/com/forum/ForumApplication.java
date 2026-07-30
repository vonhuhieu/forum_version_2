package com.forum;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

import org.springframework.scheduling.annotation.EnableAsync;
import java.util.TimeZone;

@SpringBootApplication(excludeName = {
    "org.springframework.boot.autoconfigure.elasticsearch.ElasticsearchAutoConfiguration",
    "org.springframework.boot.autoconfigure.data.elasticsearch.ElasticsearchDataAutoConfiguration",
    "org.opensearch.spring.boot.autoconfigure.OpenSearchRestHighLevelClientAutoConfiguration",
    "org.opensearch.spring.boot.autoconfigure.OpenSearchRestClientAutoConfiguration",
    "org.opensearch.spring.boot.autoconfigure.OpenSearchClientAutoConfiguration"
})
@EnableAsync
@org.springframework.cache.annotation.EnableCaching
public class ForumApplication {

    public static void main(String[] args) {
        // Đặt timezone mặc định về giờ Việt Nam để LocalDateTime.now() và
        // @CreationTimestamp luôn ghi đúng giờ UTC+7 ở mọi môi trường (local, VPS)
        TimeZone.setDefault(TimeZone.getTimeZone("Asia/Ho_Chi_Minh"));
        SpringApplication.run(ForumApplication.class, args);
    }

}

package com.forum;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

import org.springframework.scheduling.annotation.EnableAsync;

@SpringBootApplication(excludeName = {
    "org.springframework.boot.autoconfigure.elasticsearch.ElasticsearchAutoConfiguration",
    "org.springframework.boot.autoconfigure.data.elasticsearch.ElasticsearchDataAutoConfiguration",
    "org.opensearch.spring.boot.autoconfigure.OpenSearchRestHighLevelClientAutoConfiguration",
    "org.opensearch.spring.boot.autoconfigure.OpenSearchRestClientAutoConfiguration",
    "org.opensearch.spring.boot.autoconfigure.OpenSearchClientAutoConfiguration"
})
@EnableAsync
public class ForumApplication {

    public static void main(String[] args) {
        SpringApplication.run(ForumApplication.class, args);
    }

}

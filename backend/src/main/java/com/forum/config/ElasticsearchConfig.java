package com.forum.config;

import org.apache.http.Header;
import org.apache.http.HttpRequest;
import org.apache.http.HttpRequestInterceptor;
import org.apache.http.message.BasicHeader;
import org.springframework.boot.autoconfigure.elasticsearch.RestClientBuilderCustomizer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class ElasticsearchConfig {

    @Bean
    public RestClientBuilderCustomizer restClientBuilderCustomizer() {
        return builder -> builder.setHttpClientConfigCallback(httpClientBuilder ->
            httpClientBuilder.addInterceptorLast((HttpRequestInterceptor) (request, context) -> {
                cleanHeader(request, "Content-Type");
                cleanHeader(request, "Accept");
            })
        );
    }

    private void cleanHeader(HttpRequest request, String headerName) {
        Header header = request.getFirstHeader(headerName);
        if (header != null && header.getValue() != null) {
            String value = header.getValue();
            String newValue = value
                .replace("application/vnd.elasticsearch+json", "application/json")
                .replace("application/vnd.elasticsearch+x-ndjson", "application/x-ndjson")
                .replaceAll(";\\s*compatible-with=\\d+", "");
            request.setHeader(headerName, newValue);
        }
    }
}

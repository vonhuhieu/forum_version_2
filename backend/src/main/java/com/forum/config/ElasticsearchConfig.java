package com.forum.config;

import org.apache.http.Header;
import org.apache.http.HttpEntity;
import org.apache.http.HttpEntityEnclosingRequest;
import org.apache.http.HttpRequest;
import org.apache.http.HttpRequestInterceptor;
import org.apache.http.entity.HttpEntityWrapper;
import org.apache.http.message.BasicHeader;
import org.springframework.boot.autoconfigure.elasticsearch.RestClientBuilderCustomizer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class ElasticsearchConfig {

    @Bean
    public RestClientBuilderCustomizer restClientBuilderCustomizer() {
        System.out.println("====== ElasticsearchConfig: Registering RestClientBuilderCustomizer ======");
        return builder -> builder.setHttpClientConfigCallback(httpClientBuilder -> {
            System.out.println("====== ElasticsearchConfig: Setting HttpClientConfigCallback ======");
            httpClientBuilder.addInterceptorFirst((HttpRequestInterceptor) (request, context) -> {
                String uri = request.getRequestLine().getUri();
                System.out.println("====== ElasticsearchConfig: Intercepting request to URI: " + uri + " ======");

                // Log all original headers
                for (Header h : request.getAllHeaders()) {
                    System.out.println("====== ElasticsearchConfig: Original Header: " + h.getName() + " = " + h.getValue() + " ======");
                }

                // 1. Clean Request Headers (Accept, Content-Type, etc.)
                cleanRequestHeader(request, "Content-Type");
                cleanRequestHeader(request, "Accept");

                // 2. Clean HttpEntity Content-Type if present
                if (request instanceof HttpEntityEnclosingRequest) {
                    HttpEntityEnclosingRequest entityRequest = (HttpEntityEnclosingRequest) request;
                    HttpEntity entity = entityRequest.getEntity();
                    if (entity != null && entity.getContentType() != null) {
                        Header contentType = entity.getContentType();
                        String value = contentType.getValue();
                        String newValue = cleanValue(value);
                        if (!value.equals(newValue)) {
                            System.out.println("====== ElasticsearchConfig: Cleaned Entity Content-Type from [" + value + "] to [" + newValue + "] ======");
                            entityRequest.setEntity(new HttpEntityWrapper(entity) {
                                @Override
                                public Header getContentType() {
                                    return new BasicHeader("Content-Type", newValue);
                                }
                            });
                        }
                    }
                }
            });
            return httpClientBuilder;
        });
    }

    private void cleanRequestHeader(HttpRequest request, String headerName) {
        Header header = request.getFirstHeader(headerName);
        if (header != null && header.getValue() != null) {
            String value = header.getValue();
            String newValue = cleanValue(value);
            if (!value.equals(newValue)) {
                request.setHeader(headerName, newValue);
                System.out.println("====== ElasticsearchConfig: Cleaned Request Header " + headerName + " from [" + value + "] to [" + newValue + "] ======");
            }
        }
    }

    private static String cleanValue(String value) {
        if (value == null) {
            return null;
        }
        return value
            .replace("application/vnd.elasticsearch+json", "application/json")
            .replace("application/vnd.elasticsearch+x-ndjson", "application/x-ndjson")
            .replaceAll(";\\s*compatible-with=\\d+", "");
    }
}


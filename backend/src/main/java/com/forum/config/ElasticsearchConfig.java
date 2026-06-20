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

    static {
        try {
            co.elastic.clients.util.ApiTypeHelper.DANGEROUS_disableRequiredPropertiesCheck(true);
            System.out.println("====== ElasticsearchConfig: DANGEROUS_disableRequiredPropertiesCheck(true) executed successfully ======");
        } catch (Throwable t) {
            System.out.println("====== ElasticsearchConfig: Failed to disable required properties check: " + t.getMessage() + " ======");
            t.printStackTrace();
        }
    }

    @Bean
    public RestClientBuilderCustomizer restClientBuilderCustomizer() {
        System.out.println("====== ElasticsearchConfig: Registering RestClientBuilderCustomizer ======");
        return builder -> builder.setHttpClientConfigCallback(httpClientBuilder -> {
            System.out.println("====== ElasticsearchConfig: Setting HttpClientConfigCallback ======");
            httpClientBuilder.addInterceptorFirst((HttpRequestInterceptor) (request, context) -> {
                String uri = request.getRequestLine().getUri();
                System.out.println("====== ElasticsearchConfig: Intercepting request to URI: " + uri + " ======");
                
                // Print all headers for debugging
                for (Header h : request.getAllHeaders()) {
                    System.out.println("====== ElasticsearchConfig: Request Header: " + h.getName() + " = " + h.getValue() + " ======");
                }
                
                cleanHeader(request, "Content-Type");
                cleanHeader(request, "Accept");
            });
            return httpClientBuilder;
        });
    }

    private void cleanHeader(HttpRequest request, String headerName) {
        Header header = request.getFirstHeader(headerName);
        if (header != null && header.getValue() != null) {
            String value = header.getValue();
            String newValue = value
                .replace("application/vnd.elasticsearch+json", "application/json")
                .replace("application/vnd.elasticsearch+x-ndjson", "application/x-ndjson")
                .replaceAll(";\\s*compatible-with=\\d+", "");
            if (!value.equals(newValue)) {
                request.setHeader(headerName, newValue);
                System.out.println("====== ElasticsearchConfig: Cleaned " + headerName + " from [" + value + "] to [" + newValue + "] ======");
            }
        }
    }
}


package com.forum.config;

import org.apache.http.Header;
import org.apache.http.HttpEntity;
import org.apache.http.HttpEntityEnclosingRequest;
import org.apache.http.HttpRequest;
import org.apache.http.HttpRequestInterceptor;
import org.apache.http.HttpResponseInterceptor;
import org.apache.http.entity.ByteArrayEntity;
import org.apache.http.entity.ContentType;
import org.apache.http.entity.HttpEntityWrapper;
import org.apache.http.impl.nio.client.HttpAsyncClientBuilder;
import org.apache.http.message.BasicHeader;
import org.apache.http.util.EntityUtils;
import org.elasticsearch.client.RestClientBuilder;
import org.springframework.boot.autoconfigure.elasticsearch.RestClientBuilderCustomizer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.nio.charset.StandardCharsets;

@Configuration
public class ElasticsearchConfig {

    @Bean
    public RestClientBuilderCustomizer restClientBuilderCustomizer() {
        System.out.println("====== ElasticsearchConfig: Registering RestClientBuilderCustomizer ======");

        return new RestClientBuilderCustomizer() {

            @Override
            public void customize(RestClientBuilder builder) {
                // Do NOT call builder.setHttpClientConfigCallback() here!
                // That would override Spring Boot's default callback which sets up Basic Auth credentials.
            }

            @Override
            public void customize(HttpAsyncClientBuilder builder) {
                // This method is called WITHIN the existing HttpClientConfigCallback,
                // so Basic Auth credentials from spring.elasticsearch.username/password are preserved.
                System.out.println("====== ElasticsearchConfig: Customizing HttpAsyncClientBuilder (credentials preserved) ======");

                // ===== REQUEST INTERCEPTOR: Clean outgoing headers =====
                builder.addInterceptorFirst((HttpRequestInterceptor) (request, context) -> {
                    String uri = request.getRequestLine().getUri();
                    String method = request.getRequestLine().getMethod();
                    System.out.println("====== ES-REQ: " + method + " " + uri + " ======");

                    // 1. Clean Request Headers
                    cleanRequestHeader(request, "Content-Type");
                    cleanRequestHeader(request, "Accept");

                    // 2. Clean HttpEntity Content-Type if present
                    if (request instanceof HttpEntityEnclosingRequest entityRequest) {
                        HttpEntity entity = entityRequest.getEntity();
                        if (entity != null && entity.getContentType() != null) {
                            String value = entity.getContentType().getValue();
                            String newValue = cleanValue(value);
                            if (!value.equals(newValue)) {
                                System.out.println("====== ES-REQ: Cleaned Entity CT: [" + value + "] -> [" + newValue + "] ======");
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

                // ===== RESPONSE INTERCEPTOR: Log error responses =====
                builder.addInterceptorLast((HttpResponseInterceptor) (response, context) -> {
                    int statusCode = response.getStatusLine().getStatusCode();
                    String reason = response.getStatusLine().getReasonPhrase();
                    System.out.println("====== ES-RESP: Status " + statusCode + " " + reason + " ======");

                    if (statusCode >= 400) {
                        HttpEntity entity = response.getEntity();
                        if (entity != null) {
                            byte[] content = EntityUtils.toByteArray(entity);
                            String body = new String(content, StandardCharsets.UTF_8);
                            System.out.println("====== ES-RESP: Error Body: " + body + " ======");

                            String ct = entity.getContentType() != null ? entity.getContentType().getValue() : "application/json";
                            response.setEntity(new ByteArrayEntity(content, ContentType.parse(ct)));
                        }
                    }
                });
            }
        };
    }

    private static void cleanRequestHeader(HttpRequest request, String headerName) {
        Header header = request.getFirstHeader(headerName);
        if (header != null && header.getValue() != null) {
            String value = header.getValue();
            String newValue = cleanValue(value);
            if (!value.equals(newValue)) {
                request.setHeader(headerName, newValue);
                System.out.println("====== ES-REQ: Cleaned " + headerName + ": [" + value + "] -> [" + newValue + "] ======");
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

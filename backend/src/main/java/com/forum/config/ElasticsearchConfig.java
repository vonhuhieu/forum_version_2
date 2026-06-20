package com.forum.config;

import org.apache.http.Header;
import org.apache.http.HttpEntity;
import org.apache.http.HttpEntityEnclosingRequest;
import org.apache.http.HttpRequest;
import org.apache.http.HttpRequestInterceptor;
import org.apache.http.HttpResponse;
import org.apache.http.HttpResponseInterceptor;
import org.apache.http.entity.ByteArrayEntity;
import org.apache.http.entity.ContentType;
import org.apache.http.entity.HttpEntityWrapper;
import org.apache.http.message.BasicHeader;
import org.apache.http.util.EntityUtils;
import org.springframework.boot.autoconfigure.elasticsearch.RestClientBuilderCustomizer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.nio.charset.StandardCharsets;

@Configuration
public class ElasticsearchConfig {

    @Bean
    public RestClientBuilderCustomizer restClientBuilderCustomizer() {
        System.out.println("====== ElasticsearchConfig: Registering RestClientBuilderCustomizer ======");
        return builder -> builder.setHttpClientConfigCallback(httpClientBuilder -> {
            System.out.println("====== ElasticsearchConfig: Setting HttpClientConfigCallback ======");

            // ===== REQUEST INTERCEPTOR: Clean outgoing headers =====
            httpClientBuilder.addInterceptorFirst((HttpRequestInterceptor) (request, context) -> {
                String uri = request.getRequestLine().getUri();
                String method = request.getRequestLine().getMethod();
                System.out.println("====== ES-REQ: " + method + " " + uri + " ======");

                // 1. Clean Request Headers
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
            httpClientBuilder.addInterceptorLast((HttpResponseInterceptor) (response, context) -> {
                int statusCode = response.getStatusLine().getStatusCode();
                String reason = response.getStatusLine().getReasonPhrase();
                System.out.println("====== ES-RESP: Status " + statusCode + " " + reason + " ======");

                // Log response headers
                for (Header h : response.getAllHeaders()) {
                    System.out.println("====== ES-RESP: Header: " + h.getName() + " = " + h.getValue() + " ======");
                }

                // For error responses, log the body (buffer it so the client can still read it)
                if (statusCode >= 400) {
                    HttpEntity entity = response.getEntity();
                    if (entity != null) {
                        byte[] content = EntityUtils.toByteArray(entity);
                        String body = new String(content, StandardCharsets.UTF_8);
                        System.out.println("====== ES-RESP: Error Body: " + body + " ======");

                        // Replace entity with buffered version so the client can still consume it
                        String ct = entity.getContentType() != null ? entity.getContentType().getValue() : "application/json";
                        ByteArrayEntity bufferedEntity = new ByteArrayEntity(content, ContentType.parse(ct));
                        response.setEntity(bufferedEntity);
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

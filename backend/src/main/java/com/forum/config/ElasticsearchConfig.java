package com.forum.config;

import org.apache.http.Header;
import org.apache.http.HttpEntity;
import org.apache.http.HttpEntityEnclosingRequest;
import org.apache.http.HttpRequest;
import org.apache.http.HttpRequestInterceptor;
import org.apache.http.HttpResponseInterceptor;
import org.apache.http.auth.AuthScope;
import org.apache.http.auth.UsernamePasswordCredentials;
import org.apache.http.client.CredentialsProvider;
import org.apache.http.entity.ByteArrayEntity;
import org.apache.http.entity.ContentType;
import org.apache.http.entity.HttpEntityWrapper;
import org.apache.http.impl.client.BasicCredentialsProvider;
import org.apache.http.impl.nio.client.HttpAsyncClientBuilder;
import org.apache.http.message.BasicHeader;
import org.apache.http.util.EntityUtils;
import org.elasticsearch.client.RestClientBuilder;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.elasticsearch.RestClientBuilderCustomizer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.nio.charset.StandardCharsets;

@Configuration
public class ElasticsearchConfig {

    @Value("${spring.elasticsearch.username:}")
    private String esUsername;

    @Value("${spring.elasticsearch.password:}")
    private String esPassword;

    @Bean
    public RestClientBuilderCustomizer restClientBuilderCustomizer() {
        return new RestClientBuilderCustomizer() {

            @Override
            public void customize(RestClientBuilder builder) {
                // intentionally empty — do NOT call setHttpClientConfigCallback here
            }

            @Override
            public void customize(HttpAsyncClientBuilder builder) {
                // 1. Set up Basic Auth credentials if provided
                if (esUsername != null && !esUsername.isEmpty()) {
                    CredentialsProvider credentialsProvider = new BasicCredentialsProvider();
                    credentialsProvider.setCredentials(AuthScope.ANY,
                            new UsernamePasswordCredentials(esUsername, esPassword));
                    builder.setDefaultCredentialsProvider(credentialsProvider);
                    System.out.println("====== ES-CONFIG: Basic Auth configured for user [" + esUsername + "] ======");
                } else {
                    System.out.println("====== ES-CONFIG: No credentials configured (local mode) ======");
                }

                // 2. Request interceptor — clean vendor-specific headers
                builder.addInterceptorFirst((HttpRequestInterceptor) (request, context) -> {
                    cleanRequestHeader(request, "Content-Type");
                    cleanRequestHeader(request, "Accept");

                    if (request instanceof HttpEntityEnclosingRequest entityRequest) {
                        HttpEntity entity = entityRequest.getEntity();
                        if (entity != null && entity.getContentType() != null) {
                            String value = entity.getContentType().getValue();
                            String newValue = cleanValue(value);
                            if (!value.equals(newValue)) {
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

                // 3. Response interceptor — log errors for debugging
                builder.addInterceptorLast((HttpResponseInterceptor) (response, context) -> {
                    int statusCode = response.getStatusLine().getStatusCode();
                    if (statusCode >= 400) {
                        String reason = response.getStatusLine().getReasonPhrase();
                        System.out.println("====== ES-RESP: " + statusCode + " " + reason + " ======");
                        HttpEntity entity = response.getEntity();
                        if (entity != null) {
                            byte[] content = EntityUtils.toByteArray(entity);
                            System.out.println("====== ES-RESP: Body: " + new String(content, StandardCharsets.UTF_8) + " ======");
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
            }
        }
    }

    private static String cleanValue(String value) {
        if (value == null) return null;
        return value
            .replace("application/vnd.elasticsearch+json", "application/json")
            .replace("application/vnd.elasticsearch+x-ndjson", "application/x-ndjson")
            .replaceAll(";\\s*compatible-with=\\d+", "");
    }
}

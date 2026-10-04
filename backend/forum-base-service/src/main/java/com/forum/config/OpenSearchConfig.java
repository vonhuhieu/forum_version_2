package com.forum.config;

import org.apache.http.HttpHost;
import org.apache.http.auth.AuthScope;
import org.apache.http.auth.UsernamePasswordCredentials;
import org.apache.http.client.CredentialsProvider;
import org.apache.http.impl.client.BasicCredentialsProvider;
import org.opensearch.client.RestClient;
import org.opensearch.client.RestClientBuilder;
import org.opensearch.client.json.jackson.JacksonJsonpMapper;
import org.opensearch.client.opensearch.OpenSearchClient;
import org.opensearch.client.transport.OpenSearchTransport;
import org.opensearch.client.transport.rest_client.RestClientTransport;
import org.opensearch.data.client.osc.OpenSearchTemplate;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.elasticsearch.core.ElasticsearchOperations;

import java.net.URI;

@Configuration
public class OpenSearchConfig {

    @Value("${opensearch.uris}")
    private String uris;

    @Value("${opensearch.username:}")
    private String username;

    @Value("${opensearch.password:}")
    private String password;

    @Bean
    public RestClient restClient() {
        String[] uriList = uris.split(",");
        HttpHost[] hosts = new HttpHost[uriList.length];
        for (int i = 0; i < uriList.length; i++) {
            URI uri = URI.create(uriList[i].trim());
            int port = uri.getPort();
            if (port == -1) {
                port = "https".equalsIgnoreCase(uri.getScheme()) ? 443 : 80;
            }
            hosts[i] = new HttpHost(uri.getHost(), port, uri.getScheme());
        }

        RestClientBuilder builder = RestClient.builder(hosts);

        final CredentialsProvider credentialsProvider = new BasicCredentialsProvider();
        boolean hasCredentials = false;

        if (username != null && !username.trim().isEmpty()) {
            credentialsProvider.setCredentials(AuthScope.ANY,
                    new UsernamePasswordCredentials(username, password));
            hasCredentials = true;
        }

        for (String u : uriList) {
            URI uri = URI.create(u.trim());
            String userInfo = uri.getUserInfo();
            if (userInfo != null && userInfo.contains(":")) {
                String[] creds = userInfo.split(":", 2);
                credentialsProvider.setCredentials(AuthScope.ANY,
                        new UsernamePasswordCredentials(creds[0], creds[1]));
                hasCredentials = true;
                break;
            }
        }

        if (hasCredentials) {
            builder.setHttpClientConfigCallback(httpClientBuilder -> 
                httpClientBuilder.setDefaultCredentialsProvider(credentialsProvider)
            );
        }

        return builder.build();
    }

    @Bean
    public OpenSearchTransport openSearchTransport(RestClient restClient) {
        return new RestClientTransport(restClient, new JacksonJsonpMapper());
    }

    @Bean
    public OpenSearchClient openSearchClient(OpenSearchTransport transport) {
        return new OpenSearchClient(transport);
    }

    @Bean
    public ElasticsearchOperations elasticsearchTemplate(OpenSearchClient openSearchClient) {
        return new OpenSearchTemplate(openSearchClient);
    }
}

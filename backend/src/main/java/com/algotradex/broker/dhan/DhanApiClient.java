package com.algotradex.broker.dhan;

import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;

@Component
public class DhanApiClient {

    private final WebClient webClient;
    private static final String BASE_URL = "https://api.dhan.co/v2";

    public DhanApiClient(WebClient.Builder webClientBuilder) {
        this.webClient = webClientBuilder
                .baseUrl(BASE_URL)
                .defaultHeader(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
                .defaultHeader(HttpHeaders.ACCEPT, MediaType.APPLICATION_JSON_VALUE)
                .build();
    }

    public WebClient getWebClient(String clientId, String accessToken) {
        return webClient.mutate()
                .defaultHeader("client-id", clientId)
                .defaultHeader("access-token", accessToken)
                .build();
    }
}

package com.algotradex.broker.angelone;

import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;
import java.net.InetAddress;
import java.net.NetworkInterface;
import java.util.Enumeration;

@Component
public class AngelOneApiClient {

    private final WebClient webClient;
    private static final String BASE_URL = "https://apiconnect.angelone.in";

    public AngelOneApiClient(WebClient.Builder webClientBuilder) {
        this.webClient = webClientBuilder
                .baseUrl(BASE_URL)
                .defaultHeader(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
                .defaultHeader(HttpHeaders.ACCEPT, MediaType.APPLICATION_JSON_VALUE)
                .build();
    }

    public WebClient getWebClient(String apiKey, String accessToken) {
        return webClient.mutate()
                .defaultHeader("X-PrivateKey", apiKey)
                .defaultHeader("X-UserType", "USER")
                .defaultHeader("X-SourceID", "WEB")
                .defaultHeader("X-ClientLocalIP", getLocalIp())
                .defaultHeader("X-ClientPublicIP", getLocalIp()) // Mocked for now
                .defaultHeader("X-MACAddress", getMacAddress())
                .defaultHeader(HttpHeaders.AUTHORIZATION, accessToken != null ? "Bearer " + accessToken : "")
                .build();
    }

    private String getLocalIp() {
        try {
            return InetAddress.getLocalHost().getHostAddress();
        } catch (Exception e) {
            return "192.168.1.1";
        }
    }

    private String getMacAddress() {
        try {
            InetAddress localHost = InetAddress.getLocalHost();
            NetworkInterface ni = NetworkInterface.getByInetAddress(localHost);
            if (ni != null) {
                byte[] hardwareAddress = ni.getHardwareAddress();
                if (hardwareAddress != null) {
                    String[] hexadecimal = new String[hardwareAddress.length];
                    for (int i = 0; i < hardwareAddress.length; i++) {
                        hexadecimal[i] = String.format("%02X", hardwareAddress[i]);
                    }
                    return String.join("-", hexadecimal);
                }
            }
            return "00-14-22-01-23-45";
        } catch (Exception e) {
            return "00-14-22-01-23-45";
        }
    }
}

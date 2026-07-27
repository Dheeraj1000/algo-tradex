package com.algotradex.broker.angelone;

import com.algotradex.broker.BrokerAdapter;
import com.algotradex.model.BrokerAccount;
import com.algotradex.model.Order;
import com.algotradex.model.Position;
import com.algotradex.model.enums.BrokerType;
import com.fasterxml.jackson.annotation.JsonProperty;
import dev.samstevens.totp.code.DefaultCodeGenerator;
import dev.samstevens.totp.exceptions.CodeGenerationException;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Slf4j
@Component
@RequiredArgsConstructor
public class AngelOneBrokerAdapter implements BrokerAdapter {

    private final AngelOneApiClient apiClient;
    private final DefaultCodeGenerator codeGenerator = new DefaultCodeGenerator();

    @Override
    public BrokerType getBrokerType() {
        return BrokerType.ANGEL_ONE;
    }

    @Override
    public void connect(BrokerAccount account) {
        String totpCode = "";
        if (account.getTotpSecretEnc() != null && !account.getTotpSecretEnc().isEmpty()) {
            try {
                long currentBucket = Math.floorDiv(System.currentTimeMillis(), 30000L);
                totpCode = codeGenerator.generate(account.getTotpSecretEnc(), currentBucket);
            } catch (CodeGenerationException e) {
                log.error("Failed to generate TOTP code", e);
                throw new IllegalStateException("Failed to generate TOTP code");
            }
        }

        LoginRequest request = new LoginRequest();
        request.setClientcode(account.getClientId());
        request.setPassword(account.getPinEnc());
        request.setTotp(totpCode);

        try {
            AngelOneResponse<LoginResponseData> response = apiClient.getWebClient(account.getApiKeyEnc(), null)
                    .post()
                    .uri("/rest/auth/angelbroking/user/v1/loginByPassword")
                    .bodyValue(request)
                    .retrieve()
                    .bodyToMono(AngelOneLoginResponse.class)
                    .block();

            if (response != null && response.isStatus() && response.getData() != null) {
                account.setAccessToken(response.getData().getJwtToken());
                account.setRefreshToken(response.getData().getRefreshToken());
                log.info("Successfully connected to AngelOne for client: {}", account.getClientId());
            } else {
                String errorMsg = response != null ? response.getMessage() : "Unknown error";
                log.error("AngelOne login failed: {}", errorMsg);
                throw new IllegalStateException("AngelOne login failed: " + errorMsg);
            }
        } catch (Exception e) {
            log.error("Exception during AngelOne login", e);
            throw new IllegalStateException("Failed to connect to AngelOne API", e);
        }
    }

    @Override
    public boolean isConnected() {
        // Technically, we should check if accessToken is present and not expired
        return true; 
    }

    @Override
    public String placeOrder(BrokerAccount account, Order order) {
        // Here we will map our Order to AngelOne's PlaceOrderRequest
        // For now, we simulate to ensure the architecture holds up before testing real orders
        String symbol = order.getInstrument() != null ? order.getInstrument().getTradingSymbol() : "UNKNOWN";
        log.info("Mock placing real AngelOne order for symbol {}", symbol);
        return "ANGEL-" + UUID.randomUUID().toString().substring(0, 8).toUpperCase();
    }

    @Override
    public void modifyOrder(BrokerAccount account, Order order) {
    }

    @Override
    public void cancelOrder(BrokerAccount account, Order order) {
    }

    @Override
    public List<Position> getPositions(BrokerAccount account) {
        return new ArrayList<>();
    }

    @Override
    public BigDecimal getAvailableMargin(BrokerAccount account) {
        return new BigDecimal("0.00");
    }

    // Inner classes for Request/Response mapping
    @Data
    private static class LoginRequest {
        private String clientcode;
        private String password;
        private String totp;
    }

    @Data
    private static class AngelOneResponse<T> {
        private boolean status;
        private String message;
        private String errorcode;
        private T data;
    }
    
    // Explicit subclass to help WebClient Jackson deserialization
    private static class AngelOneLoginResponse extends AngelOneResponse<LoginResponseData> {}

    @Data
    private static class LoginResponseData {
        private String jwtToken;
        private String refreshToken;
        private String feedToken;
    }
}

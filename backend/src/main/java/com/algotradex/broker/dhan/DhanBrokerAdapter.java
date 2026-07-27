package com.algotradex.broker.dhan;

import com.algotradex.broker.BrokerAdapter;
import com.algotradex.model.BrokerAccount;
import com.algotradex.model.Order;
import com.algotradex.model.Position;
import com.algotradex.model.enums.BrokerType;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import com.algotradex.model.Instrument;
import com.algotradex.model.enums.OrderSide;
import com.algotradex.model.enums.PositionStatus;
import com.algotradex.model.enums.ProductType;
import com.algotradex.model.enums.TradingMode;

import java.math.BigDecimal;
import java.time.ZonedDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Slf4j
@Component
@RequiredArgsConstructor
public class DhanBrokerAdapter implements BrokerAdapter {

    private final DhanApiClient apiClient;

    @Override
    public BrokerType getBrokerType() {
        return BrokerType.DHAN;
    }

    @Override
    public void connect(BrokerAccount account) {
        try {
            // Test the access token by fetching user profile
            DhanResponse response = apiClient.getWebClient(account.getClientId(), account.getAccessToken())
                    .get()
                    .uri("/profile")
                    .retrieve()
                    .bodyToMono(DhanResponse.class)
                    .block();

            if (response == null) {
                throw new IllegalStateException("Empty response from Dhan");
            }
            log.info("Successfully verified Dhan connection for client: {}", account.getClientId());
        } catch (Exception e) {
            log.error("Exception during Dhan connection verification", e);
            throw new IllegalStateException("Failed to connect to Dhan API: " + e.getMessage(), e);
        }
    }

    @Override
    public boolean isConnected() {
        return true; 
    }

    @Override
    public String placeOrder(BrokerAccount account, Order order) {
        String securityId = order.getInstrument() != null ? order.getInstrument().getToken() : "1333"; // 1333 is HDFC Bank as fallback
        String symbol = order.getInstrument() != null ? order.getInstrument().getTradingSymbol() : "UNKNOWN";
        log.info("Placing real Dhan order for symbol {} (securityId: {})", symbol, securityId);

        DhanOrderRequest request = new DhanOrderRequest();
        request.setDhanClientId(account.getClientId());
        request.setCorrelationId(order.getId() != null ? order.getId().toString() : UUID.randomUUID().toString());
        request.setTransactionType(order.getSide().name()); // BUY or SELL
        request.setExchangeSegment("NSE_EQ"); // Hardcoded for now
        request.setProductType("INTRADAY"); // Assuming INTRADAY
        request.setOrderType(order.getOrderType().name()); // MARKET or LIMIT
        request.setValidity("DAY");
        request.setSecurityId(securityId);
        request.setQuantity(order.getQuantity());
        request.setPrice(order.getPrice() != null ? order.getPrice().doubleValue() : 0.0);
        request.setTriggerPrice(order.getTriggerPrice() != null ? order.getTriggerPrice().doubleValue() : 0.0);
        request.setDisclosedQuantity(0);
        request.setAfterMarketOrder(false);

        try {
            DhanOrderResponse response = apiClient.getWebClient(account.getClientId(), account.getAccessToken())
                    .post()
                    .uri("/orders")
                    .bodyValue(request)
                    .retrieve()
                    .bodyToMono(DhanOrderResponse.class)
                    .block();

            if (response != null && response.getOrderId() != null) {
                log.info("Dhan order placed successfully. OrderID: {}", response.getOrderId());
                return response.getOrderId();
            } else {
                String error = response != null ? response.getRemarks() : "Unknown error";
                log.error("Dhan order placement failed: {}", error);
                throw new IllegalStateException("Dhan order placement failed: " + error);
            }
        } catch (Exception e) {
            log.error("Exception during Dhan order placement", e);
            throw new IllegalStateException("Failed to place order via Dhan API: " + e.getMessage(), e);
        }
    }

    @Override
    public void modifyOrder(BrokerAccount account, Order order) {
        if (order.getBrokerOrderId() == null) {
            throw new IllegalArgumentException("Cannot modify order: Broker Order ID is missing");
        }

        String securityId = order.getInstrument() != null ? order.getInstrument().getToken() : "1333";

        DhanOrderRequest request = new DhanOrderRequest();
        request.setDhanClientId(account.getClientId());
        request.setCorrelationId(order.getId() != null ? order.getId().toString() : UUID.randomUUID().toString());
        request.setTransactionType(order.getSide().name());
        request.setExchangeSegment("NSE_EQ");
        request.setProductType("INTRADAY");
        request.setOrderType(order.getOrderType().name());
        request.setValidity("DAY");
        request.setSecurityId(securityId);
        request.setQuantity(order.getQuantity());
        request.setPrice(order.getPrice() != null ? order.getPrice().doubleValue() : 0.0);
        request.setTriggerPrice(order.getTriggerPrice() != null ? order.getTriggerPrice().doubleValue() : 0.0);

        try {
            DhanOrderResponse response = apiClient.getWebClient(account.getClientId(), account.getAccessToken())
                    .put()
                    .uri("/orders/" + order.getBrokerOrderId())
                    .bodyValue(request)
                    .retrieve()
                    .bodyToMono(DhanOrderResponse.class)
                    .block();

            if (response == null || response.getOrderId() == null) {
                String error = response != null ? response.getRemarks() : "Unknown error";
                throw new IllegalStateException("Dhan order modification failed: " + error);
            }
            log.info("Dhan order modified successfully. OrderID: {}", response.getOrderId());
        } catch (Exception e) {
            log.error("Exception during Dhan order modification", e);
            throw new IllegalStateException("Failed to modify order via Dhan API: " + e.getMessage(), e);
        }
    }

    @Override
    public void cancelOrder(BrokerAccount account, Order order) {
        if (order.getBrokerOrderId() == null) {
            throw new IllegalArgumentException("Cannot cancel order: Broker Order ID is missing");
        }

        try {
            DhanOrderResponse response = apiClient.getWebClient(account.getClientId(), account.getAccessToken())
                    .delete()
                    .uri("/orders/" + order.getBrokerOrderId())
                    .retrieve()
                    .bodyToMono(DhanOrderResponse.class)
                    .block();

            if (response == null || response.getOrderId() == null) {
                String error = response != null ? response.getRemarks() : "Unknown error";
                throw new IllegalStateException("Dhan order cancellation failed: " + error);
            }
            log.info("Dhan order cancelled successfully. OrderID: {}", response.getOrderId());
        } catch (Exception e) {
            log.error("Exception during Dhan order cancellation", e);
            throw new IllegalStateException("Failed to cancel order via Dhan API: " + e.getMessage(), e);
        }
    }

    @Override
    public List<Position> getPositions(BrokerAccount account) {
        try {
            DhanPositionResponse[] responses = apiClient.getWebClient(account.getClientId(), account.getAccessToken())
                    .get()
                    .uri("/positions")
                    .retrieve()
                    .bodyToMono(DhanPositionResponse[].class)
                    .block();

            List<Position> positions = new ArrayList<>();
            if (responses != null) {
                for (DhanPositionResponse res : responses) {
                    Position pos = new Position();
                    pos.setBrokerAccount(account);
                    pos.setUser(account.getUser());
                    pos.setTradingMode(TradingMode.LIVE);
                    
                    Instrument instrument = Instrument.builder()
                            .tradingSymbol(res.getTradingSymbol())
                            .symbol(res.getTradingSymbol())
                            .token(res.getSecurityId())
                            .name(res.getTradingSymbol())
                            .build();
                    pos.setInstrument(instrument);
                    
                    pos.setSide(mapPositionSide(res.getPositionType()));
                    pos.setQuantity(Math.abs(res.getNetQty()));
                    pos.setAvgEntryPrice(BigDecimal.valueOf(res.getBuyAvg()));
                    pos.setCurrentPrice(BigDecimal.valueOf(res.getSellAvg() > 0 ? res.getSellAvg() : res.getBuyAvg())); // fallback
                    pos.setUnrealizedPnl(BigDecimal.valueOf(res.getUnrealizedProfit()));
                    pos.setRealizedPnl(BigDecimal.valueOf(res.getRealizedProfit()));
                    pos.setProductType(mapProductType(res.getProductType()));
                    pos.setStatus(res.getNetQty() == 0 ? PositionStatus.CLOSED : PositionStatus.OPEN);
                    pos.setOpenedAt(ZonedDateTime.now()); // fallback as Dhan API might not return opened time in basic positions call
                    
                    positions.add(pos);
                }
            }
            return positions;
        } catch (org.springframework.web.reactive.function.client.WebClientResponseException e) {
            log.warn("Dhan API error: {} - {}. Is your API Key/Token valid?", e.getStatusCode(), e.getMessage());
            return new ArrayList<>();
        } catch (Exception e) {
            log.error("Failed to fetch positions from Dhan: {}", e.getMessage());
            return new ArrayList<>();
        }
    }

    private ProductType mapProductType(String dhanProduct) {
        if (dhanProduct == null) return ProductType.INTRADAY;
        return switch (dhanProduct.toUpperCase()) {
            case "CNC" -> ProductType.DELIVERY;
            case "CARRYFORWARD", "MARGIN", "NRML" -> ProductType.CARRYFORWARD;
            default -> ProductType.INTRADAY;
        };
    }

    private OrderSide mapPositionSide(String dhanPositionType) {
        if ("SHORT".equalsIgnoreCase(dhanPositionType)) {
            return OrderSide.SELL;
        }
        return OrderSide.BUY;
    }

    @Override
    public BigDecimal getAvailableMargin(BrokerAccount account) {
        try {
            DhanFundLimitResponse response = apiClient.getWebClient(account.getClientId(), account.getAccessToken())
                    .get()
                    .uri("/fundlimit")
                    .retrieve()
                    .bodyToMono(DhanFundLimitResponse.class)
                    .block();

            if (response != null && response.getAvailabelBalance() != null) {
                return response.getAvailabelBalance();
            }
            return BigDecimal.ZERO;
        } catch (Exception e) {
            log.error("Failed to fetch available margin from Dhan", e);
            return BigDecimal.ZERO;
        }
    }

    @Data
    private static class DhanResponse {
        private Object data;
    }

    @Data
    private static class DhanOrderRequest {
        private String dhanClientId;
        private String correlationId;
        private String transactionType;
        private String exchangeSegment;
        private String productType;
        private String orderType;
        private String validity;
        private String tradingSymbol;
        private String securityId;
        private int quantity;
        private int disclosedQuantity;
        private double price;
        private double triggerPrice;
        private boolean afterMarketOrder;
        private String amoTime;
        private double boProfitValue;
        private double boStopLossValue;
        private String drvExpiryDate;
        private String drvOptionType;
        private double drvStrikePrice;
    }

    @Data
    private static class DhanOrderResponse {
        private String orderId;
        private String orderStatus;
        private String errorCode;
        private String httpStatus;
        private String internalErrorCode;
        private String remarks;
        private Object data;
    }

    @Data
    private static class DhanFundLimitResponse {
        private String dhanClientId;
        private BigDecimal availabelBalance;
        private BigDecimal sodLimit;
        private BigDecimal collateralAmount;
        private BigDecimal utilizedAmount;
        private BigDecimal withdrawableBalance;
    }

    @Data
    private static class DhanPositionResponse {
        private String dhanClientId;
        private String tradingSymbol;
        private String securityId;
        private String positionType;
        private String exchangeSegment;
        private String productType;
        private double buyAvg;
        private int buyQty;
        private double sellAvg;
        private int sellQty;
        private int netQty;
        private double realizedProfit;
        private double unrealizedProfit;
    }
}

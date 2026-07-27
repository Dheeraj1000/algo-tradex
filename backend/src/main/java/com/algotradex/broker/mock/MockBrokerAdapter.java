package com.algotradex.broker.mock;

import com.algotradex.broker.BrokerAdapter;
import com.algotradex.model.BrokerAccount;
import com.algotradex.model.Order;
import com.algotradex.model.Position;
import com.algotradex.model.enums.BrokerType;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Component
public class MockBrokerAdapter implements BrokerAdapter {

    private boolean connected = false;

    @Override
    public BrokerType getBrokerType() {
        return BrokerType.MOCK;
    }

    @Override
    public void connect(BrokerAccount account) {
        // Simulate a connection
        this.connected = true;
    }

    @Override
    public boolean isConnected() {
        return connected;
    }

    @Override
    public String placeOrder(BrokerAccount account, Order order) {
        if (!connected) throw new IllegalStateException("Broker not connected");
        // Simulate placing order and getting a mock broker order ID
        return "MOCK-" + UUID.randomUUID().toString().substring(0, 8).toUpperCase();
    }

    @Override
    public void modifyOrder(BrokerAccount account, Order order) {
        if (!connected) throw new IllegalStateException("Broker not connected");
        // Simulate successful modification
    }

    @Override
    public void cancelOrder(BrokerAccount account, Order order) {
        if (!connected) throw new IllegalStateException("Broker not connected");
        // Simulate successful cancellation
    }

    @Override
    public List<Position> getPositions(BrokerAccount account) {
        if (!connected) throw new IllegalStateException("Broker not connected");
        // Return an empty list or mock data
        return new ArrayList<>();
    }

    @Override
    public BigDecimal getAvailableMargin(BrokerAccount account) {
        if (!connected) throw new IllegalStateException("Broker not connected");
        // Mock margin
        return new BigDecimal("1000000.00");
    }
}

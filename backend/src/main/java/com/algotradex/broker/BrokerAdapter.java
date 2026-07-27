package com.algotradex.broker;

import com.algotradex.model.BrokerAccount;
import com.algotradex.model.Order;
import com.algotradex.model.Position;
import com.algotradex.model.enums.BrokerType;

import java.math.BigDecimal;
import java.util.List;

public interface BrokerAdapter {
    
    BrokerType getBrokerType();
    
    void connect(BrokerAccount account);
    
    boolean isConnected();
    
    String placeOrder(BrokerAccount account, Order order);
    
    void modifyOrder(BrokerAccount account, Order order);
    
    void cancelOrder(BrokerAccount account, Order order);
    
    List<Position> getPositions(BrokerAccount account);
    
    BigDecimal getAvailableMargin(BrokerAccount account);
}

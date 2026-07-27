package com.algotradex.service;

import com.algotradex.broker.BrokerAdapter;
import com.algotradex.model.BrokerAccount;
import com.algotradex.model.Order;
import com.algotradex.model.enums.OrderStatus;
import com.algotradex.repository.OrderRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class OrderService {

    private final OrderRepository orderRepository;
    private final BrokerService brokerService;
    private final com.algotradex.repository.BrokerAccountRepository brokerAccountRepository;

    @Transactional
    public Order placeOrder(Order order) {
        if (order.getBrokerAccount() == null || order.getBrokerAccount().getId() == null) {
            throw new IllegalArgumentException("Broker account ID is required to place an order");
        }
        
        BrokerAccount account = brokerAccountRepository.findById(order.getBrokerAccount().getId())
                .orElseThrow(() -> new IllegalArgumentException("Broker account not found"));
        order.setBrokerAccount(account);
        
        BrokerAdapter adapter = brokerService.getAdapterForAccount(account);
        
        // Ensure connected
        if (!adapter.isConnected()) {
            brokerService.connectBroker(account.getId());
        }
        
        // Save initially as PENDING
        order.setStatus(OrderStatus.PENDING);
        Order savedOrder = orderRepository.save(order);
        
        try {
            String brokerOrderId = adapter.placeOrder(account, savedOrder);
            savedOrder.setBrokerOrderId(brokerOrderId);
            savedOrder.setStatus(OrderStatus.OPEN);
            return orderRepository.save(savedOrder);
        } catch (Exception e) {
            savedOrder.setStatus(OrderStatus.REJECTED);
            savedOrder.setErrorMessage(e.getMessage());
            return orderRepository.save(savedOrder);
        }
    }

    public List<Order> getUserOrders(UUID userId) {
        return orderRepository.findByUserIdOrderByCreatedAtDesc(userId);
    }

    @Transactional
    public Order cancelOrder(UUID orderId, UUID userId) {
        Order order = orderRepository.findById(orderId)
                .orElseThrow(() -> new IllegalArgumentException("Order not found"));
                
        if (!order.getUser().getId().equals(userId)) {
            throw new IllegalStateException("Unauthorized to cancel this order");
        }
        
        if (order.getStatus() != OrderStatus.OPEN && order.getStatus() != OrderStatus.PENDING) {
            throw new IllegalStateException("Order is not in open or pending state");
        }
        
        BrokerAccount account = order.getBrokerAccount();
        BrokerAdapter adapter = brokerService.getAdapterForAccount(account);
        
        if (!adapter.isConnected()) {
            brokerService.connectBroker(account.getId());
        }
        
        try {
            adapter.cancelOrder(account, order);
            order.setStatus(OrderStatus.CANCELLED);
            return orderRepository.save(order);
        } catch (Exception e) {
            throw new IllegalStateException("Failed to cancel order via broker: " + e.getMessage(), e);
        }
    }
}

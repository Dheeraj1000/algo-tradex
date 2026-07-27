package com.algotradex.controller;

import com.algotradex.model.Order;
import com.algotradex.model.User;
import com.algotradex.repository.UserRepository;
import com.algotradex.service.OrderService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/orders")
@RequiredArgsConstructor
public class OrderController {

    private final OrderService orderService;
    private final UserRepository userRepository;

    @PostMapping
    public ResponseEntity<Order> placeOrder(
            @AuthenticationPrincipal UserDetails userDetails,
            @RequestBody Order order) {
            
        User user = userRepository.findByEmail(userDetails.getUsername())
                .orElseThrow(() -> new IllegalArgumentException("User not found"));
                
        order.setUser(user); // Ensure the order is tied to the logged-in user
        Order savedOrder = orderService.placeOrder(order);
        
        return ResponseEntity.ok(savedOrder);
    }
    
    @GetMapping
    public ResponseEntity<List<Order>> getOrders(
            @AuthenticationPrincipal UserDetails userDetails) {
            
        User user = userRepository.findByEmail(userDetails.getUsername())
                .orElseThrow(() -> new IllegalArgumentException("User not found"));
                
        return ResponseEntity.ok(orderService.getUserOrders(user.getId()));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Order> cancelOrder(
            @AuthenticationPrincipal UserDetails userDetails,
            @PathVariable UUID id) {
            
        User user = userRepository.findByEmail(userDetails.getUsername())
                .orElseThrow(() -> new IllegalArgumentException("User not found"));
                
        Order cancelledOrder = orderService.cancelOrder(id, user.getId());
        return ResponseEntity.ok(cancelledOrder);
    }
}

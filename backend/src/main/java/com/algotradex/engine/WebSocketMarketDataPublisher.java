package com.algotradex.engine;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.event.EventListener;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class WebSocketMarketDataPublisher {

    private final SimpMessagingTemplate messagingTemplate;

    @EventListener
    public void handleMarketTick(MarketTickEvent event) {
        String symbol = event.getSymbol();
        messagingTemplate.convertAndSend("/topic/ticks/" + symbol, event);
        
        if (symbol.contains("-EQ")) {
            messagingTemplate.convertAndSend("/topic/ticks/" + symbol.replace("-EQ", ""), event);
        } else {
            messagingTemplate.convertAndSend("/topic/ticks/" + symbol + "-EQ", event);
        }
    }
}

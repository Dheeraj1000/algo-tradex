package com.algotradex.service;

import com.algotradex.broker.BrokerAdapter;
import com.algotradex.broker.BrokerAdapterFactory;
import com.algotradex.model.BrokerAccount;
import com.algotradex.model.User;
import com.algotradex.model.enums.BrokerStatus;
import com.algotradex.model.enums.BrokerType;
import com.algotradex.repository.BrokerAccountRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.ZonedDateTime;
import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class BrokerService {

    private final BrokerAccountRepository brokerAccountRepository;
    private final BrokerAdapterFactory brokerAdapterFactory;
    
    @Transactional
    public BrokerAccount linkBrokerAccount(User user, BrokerType brokerType, String clientId, String apiKey, String apiSecret, String pin, String totpSecret, String accessToken) {
        BrokerAccount account = BrokerAccount.builder()
                .user(user)
                .brokerType(brokerType)
                .clientId(clientId)
                .apiKeyEnc(apiKey) // Normally would encrypt these
                .apiSecretEnc(apiSecret) // Normally would encrypt these
                .pinEnc(pin)
                .totpSecretEnc(totpSecret)
                .accessToken(accessToken)
                .status(BrokerStatus.DISCONNECTED)
                .isPrimary(false) // Handle setting primary logic
                .build();
                
        return brokerAccountRepository.save(account);
    }
    
    public BrokerAccount connectBroker(UUID brokerAccountId) {
        BrokerAccount account = brokerAccountRepository.findById(brokerAccountId)
                .orElseThrow(() -> new IllegalArgumentException("Broker account not found"));
                
        try {
            BrokerAdapter adapter = brokerAdapterFactory.getAdapter(account.getBrokerType());
            adapter.connect(account);
            
            if (adapter.isConnected()) {
                account.setStatus(BrokerStatus.CONNECTED);
                account.setLastConnected(ZonedDateTime.now());
            } else {
                account.setStatus(BrokerStatus.ERROR);
            }
        } catch (Exception e) {
            account.setStatus(BrokerStatus.ERROR);
        }
        return brokerAccountRepository.save(account);
    }
    
    public List<BrokerAccount> getUserBrokerAccounts(UUID userId) {
        return brokerAccountRepository.findByUserIdAndDeletedAtIsNull(userId);
    }
    
    public BrokerAdapter getAdapterForAccount(BrokerAccount account) {
        return brokerAdapterFactory.getAdapter(account.getBrokerType());
    }
}

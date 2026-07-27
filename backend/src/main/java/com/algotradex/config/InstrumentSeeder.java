package com.algotradex.config;

import com.algotradex.model.Instrument;
import com.algotradex.model.enums.ExchangeType;
import com.algotradex.model.enums.InstrumentType;
import com.algotradex.repository.InstrumentRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.util.Arrays;
import java.util.List;
import com.algotradex.model.User;
import com.algotradex.model.BrokerAccount;
import com.algotradex.model.enums.BrokerType;
import com.algotradex.model.enums.BrokerStatus;
import com.algotradex.repository.UserRepository;
import com.algotradex.repository.BrokerAccountRepository;

@Slf4j
@Component
@RequiredArgsConstructor
public class InstrumentSeeder implements CommandLineRunner {

    private final InstrumentRepository instrumentRepository;
    private final UserRepository userRepository;
    private final BrokerAccountRepository brokerAccountRepository;

    @Override
    public void run(String... args) throws Exception {
        // Seed default Dhan account
        userRepository.findByEmail("test@algotradex.com").ifPresent(user -> {
            if (brokerAccountRepository.findByUserIdAndDeletedAtIsNull(user.getId()).stream().noneMatch(acc -> acc.getBrokerType() == BrokerType.DHAN)) {
                BrokerAccount dhanAcc = BrokerAccount.builder()
                        .user(user)
                        .brokerType(BrokerType.DHAN)
                        .clientId("1112521202")
                        .accessToken("eyJ0eXAiOiJKV1QiLCJhbGciOiJIUzUxMiJ9.eyJpc3MiOiJkaGFuIiwicGFydG5lcklkIjoiIiwiZXhwIjoxNzg0MjY0NDg5LCJpYXQiOjE3ODQxNzgwODksInRva2VuQ29uc3VtZXJUeXBlIjoiU0VMRiIsIndlYmhvb2tVcmwiOiIiLCJkaGFuQ2xpZW50SWQiOiIxMTEyNTIxMjAyIn0.0AXkolQ4OrWlrHc8kiz5biSlODOuRsM_HSQJnWeE7oGFd23Z8PqTP4lJRy1AcBnbkPT3_2ioyWGC44llY1uIIA")
                        .status(BrokerStatus.CONNECTED)
                        .isPrimary(true)
                        .build();
                brokerAccountRepository.save(dhanAcc);
                log.info("Seeded default Dhan Broker Account for test user.");
            }
        });
        if (instrumentRepository.count() == 0) {
            log.info("Instruments table is empty. Seeding top Indian stocks...");
            
            List<Instrument> seedInstruments = Arrays.asList(
                createEquity("RELIANCE", "RELIANCE-EQ", "RELIANCE INDUSTRIES LTD", "2885", "INE002A01018"),
                createEquity("HDFCBANK", "HDFCBANK-EQ", "HDFC BANK LTD", "1333", "INE040A01034"),
                createEquity("SBIN", "SBIN-EQ", "STATE BANK OF INDIA", "3045", "INE062A01020"),
                createEquity("TCS", "TCS-EQ", "TATA CONSULTANCY SERVICES LTD", "11536", "INE467B01029"),
                createEquity("INFY", "INFY-EQ", "INFOSYS LTD", "1594", "INE009A01021"),
                createEquity("ICICIBANK", "ICICIBANK-EQ", "ICICI BANK LTD", "18602", "INE090A01021"),
                createEquity("AXISBANK", "AXISBANK-EQ", "AXIS BANK LTD", "5900", "INE238A01034"),
                createEquity("BHARTIARTL", "BHARTIARTL-EQ", "BHARTI AIRTEL LTD", "10603", "INE397D01024"),
                createEquity("ITC", "ITC-EQ", "ITC LTD", "1660", "INE154A01025"),
                createEquity("LT", "LT-EQ", "LARSEN & TOUBRO LTD", "11483", "INE018A01030"),
                createEquity("NIFTYBEES", "NIFTYBEES-EQ", "NIPPON INDIA ETF NIFTY BEES", "10576", "INF204KB14I2"),
                createCrypto("BTCUSDT", "BTCUSDT", "Bitcoin Tether US", "BTC"),
                createCrypto("ETHUSDT", "ETHUSDT", "Ethereum Tether US", "ETH")
            );
            
            instrumentRepository.saveAll(seedInstruments);
            log.info("Successfully seeded {} stock instruments.", seedInstruments.size());
        } else {
            log.info("Instruments table already seeded. Total count: {}", instrumentRepository.count());
        }
        
        // Ensure index symbols exist
        if (instrumentRepository.findByTradingSymbol("^NSEI").isEmpty()) {
            instrumentRepository.save(createIndex("^NSEI", "^NSEI", "NIFTY 50", "NIFTY", ExchangeType.NSE, 50));
        }
        if (instrumentRepository.findByTradingSymbol("^BSESN").isEmpty()) {
            instrumentRepository.save(createIndex("^BSESN", "^BSESN", "BSE SENSEX", "SENSEX", ExchangeType.BSE, 10));
        }
    }

    private Instrument createIndex(String symbol, String tradingSymbol, String name, String token, ExchangeType exchange, int lotSize) {
        return Instrument.builder()
                .symbol(symbol)
                .tradingSymbol(tradingSymbol)
                .name(name)
                .exchange(exchange)
                .instrumentType(InstrumentType.INDEX)
                .segment(exchange.name() + "_INDEX")
                .lotSize(lotSize)
                .tickSize(new BigDecimal("0.05"))
                .token(token)
                .isin("")
                .isActive(true)
                .build();
    }

    private Instrument createEquity(String symbol, String tradingSymbol, String name, String token, String isin) {
        return Instrument.builder()
                .symbol(symbol)
                .tradingSymbol(tradingSymbol)
                .name(name)
                .exchange(ExchangeType.NSE)
                .instrumentType(InstrumentType.EQ)
                .segment("NSE_EQ")
                .lotSize(1)
                .tickSize(new BigDecimal("0.05"))
                .token(token)
                .isin(isin)
                .isActive(true)
                .build();
    }
    private Instrument createCrypto(String symbol, String tradingSymbol, String name, String token) {
        return Instrument.builder()
                .symbol(symbol)
                .tradingSymbol(tradingSymbol)
                .name(name)
                .exchange(ExchangeType.BINANCE)
                .instrumentType(InstrumentType.CRYPTO)
                .segment("BINANCE_PERP")
                .lotSize(1)
                .tickSize(new BigDecimal("0.01"))
                .token(token)
                .isin("")
                .isActive(true)
                .build();
    }
}

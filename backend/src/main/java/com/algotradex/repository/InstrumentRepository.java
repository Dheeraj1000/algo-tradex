package com.algotradex.repository;

import com.algotradex.model.Instrument;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface InstrumentRepository extends JpaRepository<Instrument, UUID> {
    
    @Query("SELECT i FROM Instrument i WHERE i.isActive = true AND " +
           "(LOWER(i.symbol) LIKE LOWER(CONCAT('%', :query, '%')) OR " +
           "LOWER(i.tradingSymbol) LIKE LOWER(CONCAT('%', :query, '%')) OR " +
           "LOWER(i.name) LIKE LOWER(CONCAT('%', :query, '%')))")
    List<Instrument> searchInstruments(@Param("query") String query);
    
    Optional<Instrument> findByTradingSymbol(String tradingSymbol);
}

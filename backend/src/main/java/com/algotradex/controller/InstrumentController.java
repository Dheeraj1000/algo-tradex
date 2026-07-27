package com.algotradex.controller;

import com.algotradex.model.Instrument;
import com.algotradex.repository.InstrumentRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/instruments")
@RequiredArgsConstructor
public class InstrumentController {

    private final InstrumentRepository instrumentRepository;

    @GetMapping("/search")
    public ResponseEntity<List<Instrument>> searchInstruments(@RequestParam("query") String query) {
        if (query == null || query.trim().length() < 2) {
            return ResponseEntity.ok(List.of());
        }
        return ResponseEntity.ok(instrumentRepository.searchInstruments(query.trim()));
    }
}

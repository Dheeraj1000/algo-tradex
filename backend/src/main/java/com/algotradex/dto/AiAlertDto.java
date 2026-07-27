package com.algotradex.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AiAlertDto {
    private String symbol;
    private double spotPrice;
    private String targetOption;
    private String signal;
    private double confidence;
    private double targetProfit;
    private double stopLoss;
    private double targetLtp;
    private double recommendedEntry;
}

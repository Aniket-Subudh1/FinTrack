package com.personalfinance.expense.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ExpenseSummary {

    private String category;

    private String period;

    private Double amount;

    private Double percentage;
}
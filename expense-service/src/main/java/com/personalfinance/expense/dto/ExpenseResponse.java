package com.personalfinance.expense.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ExpenseResponse {

    private Long id;

    private Double amount;

    private String category;

    private LocalDateTime date;

    private String description;

    private String userEmail;

    private LocalDateTime createdAt;

    private LocalDateTime updatedAt;
}
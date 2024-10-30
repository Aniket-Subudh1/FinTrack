package com.personalfinancetracker.backend.dto;

import jdk.jfr.DataAmount;

import java.time.LocalDate;

@DataAmount
public class IncomeDTO {

    private long id;

    private String userName;

    private Double amount;

    private String source;

    private LocalDate date;

    private String category;

    private String description;

}

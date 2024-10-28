package com.personalfinancetracker.backend.entities;

import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jdk.jfr.DataAmount;

import java.time.LocalDate;

@Entity
@DataAmount
public class Income {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private long id;

    private String userName;

    private Integer amount;

    private LocalDate date;

    private String category;

    private String description;


}

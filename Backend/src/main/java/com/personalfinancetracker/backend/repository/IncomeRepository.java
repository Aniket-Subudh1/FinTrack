package com.personalfinancetracker.backend.repository;

import com.personalfinancetracker.backend.entities.Expense;
import com.personalfinancetracker.backend.entities.Income;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;

@Repository

public interface IncomeRepository extends JpaRepository<Expense, Long> {

//    List<Expense> findByDateBetween(LocalDate startDate, LocalDate endDate);

}

package com.personalfinancetracker.backend.repository;

import com.personalfinancetracker.backend.entities.Income;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
@Repository

public interface IncomeRepository extends JpaRepository<Income, Long> {
}

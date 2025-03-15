package com.personalfinance.expense.repository;

import com.personalfinance.expense.entity.Expense;
import com.personalfinance.expense.entity.ExpenseCategory;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Repository
public interface ExpenseRepository extends JpaRepository<Expense, Long> {

    List<Expense> findByUserEmail(String userEmail);

    Page<Expense> findByUserEmail(String userEmail, Pageable pageable);

    Optional<Expense> findByIdAndUserEmail(Long id, String userEmail);

    List<Expense> findByUserEmailAndCategoryOrderByDateDesc(String userEmail, ExpenseCategory category);

    List<Expense> findByUserEmailAndDateBetweenOrderByDateDesc(String userEmail, LocalDateTime startDate, LocalDateTime endDate);

    List<Expense> findByUserEmailAndCategoryAndDateBetweenOrderByDateDesc(
            String userEmail, ExpenseCategory category, LocalDateTime startDate, LocalDateTime endDate);

    void deleteByIdAndUserEmail(Long id, String userEmail);

    @Query("SELECT e.category, SUM(e.amount) FROM Expense e WHERE e.userEmail = :userEmail AND e.date BETWEEN :startDate AND :endDate GROUP BY e.category ORDER BY SUM(e.amount) DESC")
    List<Object[]> findExpenseSummaryByCategory(@Param("userEmail") String userEmail,
                                                @Param("startDate") LocalDateTime startDate,
                                                @Param("endDate") LocalDateTime endDate);

    @Query("SELECT FUNCTION('DATE_FORMAT', e.date, '%Y-%m-01'), SUM(e.amount) FROM Expense e WHERE e.userEmail = :userEmail AND e.date BETWEEN :startDate AND :endDate GROUP BY FUNCTION('DATE_FORMAT', e.date, '%Y-%m-01') ORDER BY FUNCTION('DATE_FORMAT', e.date, '%Y-%m-01')")
    List<Object[]> findExpenseSummaryByMonth(@Param("userEmail") String userEmail,
                                             @Param("startDate") LocalDateTime startDate,
                                             @Param("endDate") LocalDateTime endDate);
}
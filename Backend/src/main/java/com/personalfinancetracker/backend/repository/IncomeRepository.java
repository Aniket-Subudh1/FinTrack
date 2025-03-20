package com.personalfinancetracker.backend.repository;

import com.personalfinancetracker.backend.dto.IncomeSummary;
import com.personalfinancetracker.backend.entities.Income;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDate;
import java.util.List;

public interface IncomeRepository extends JpaRepository<Income, Long> {
    List<Income> findByCustomerEmail(String email);

    List<Income> findByCustomerEmailAndDateBetween(String email, LocalDate startDate, LocalDate endDate);

    List<Income> findByCustomerEmailAndDateAfter(String email, LocalDate date);

    List<Income> findByCustomerEmailAndIsRecurring(String email, boolean isRecurring);

    @Query("SELECT new com.personalfinancetracker.backend.dto.IncomeSummary(i.source, SUM(i.amount)) " +
            "FROM Income i WHERE i.customerEmail = :email GROUP BY i.source")
    List<IncomeSummary> getIncomeSummaryBySource(@Param("email") String email);

    @Query("SELECT SUM(i.amount) FROM Income i WHERE i.customerEmail = :email AND i.date BETWEEN :startDate AND :endDate")
    Double getTotalIncomeForPeriod(@Param("email") String email, @Param("startDate") LocalDate startDate, @Param("endDate") LocalDate endDate);


    @Query("SELECT CONCAT(YEAR(i.date), '-', MONTH(i.date)) as month, SUM(i.amount) as total " +
            "FROM Income i WHERE i.customerEmail = :email AND i.date >= :startDate " +
            "GROUP BY YEAR(i.date), MONTH(i.date) ORDER BY YEAR(i.date), MONTH(i.date)")
    List<Object[]> getMonthlyIncomeTrend(@Param("email") String email, @Param("startDate") LocalDate startDate);
}
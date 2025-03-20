package com.personalfinancetracker.backend.repository;

import com.personalfinancetracker.backend.entities.SavedReport;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface SavedReportRepository extends JpaRepository<SavedReport, Long> {


    List<SavedReport> findByCustomerEmailOrderByCreatedDateDesc(String email);

    List<SavedReport> findByCustomerEmailAndReportType(String email, String reportType);


    void deleteByCustomerEmail(String email);
}
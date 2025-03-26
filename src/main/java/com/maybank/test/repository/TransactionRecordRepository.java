package com.maybank.test.repository;

import com.maybank.test.entity.TransactionRecord;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

public interface TransactionRecordRepository extends JpaRepository<TransactionRecord, Long> {
    Page<TransactionRecord> findByDescriptionContaining(String description, Pageable pageable);
}


package com.maybank.test.service;

import com.maybank.test.entity.TransactionRecord;
import com.maybank.test.repository.TransactionRecordRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

import java.util.Optional;

@Service
public class TransactionService {
    private final TransactionRecordRepository repository;

    public TransactionService(TransactionRecordRepository repository) {
        this.repository = repository;
    }

    public Page<TransactionRecord> getTransactions(int page, int size, String description) {
        Pageable pageable = PageRequest.of(page, size);

        if (description != null && !description.isEmpty()) {
            return repository.findByDescriptionContaining(description, pageable);
        }
        return repository.findAll(pageable);
    }


    public Optional<TransactionRecord> getTransactionById(Long id) {
        return repository.findById(id);
    }

    public TransactionRecord updateTransaction(Long id, TransactionRecord updatedRecord) {
        return repository.findById(id).map(record -> {
            record.setTrxAmount(updatedRecord.getTrxAmount());
            record.setDescription(updatedRecord.getDescription());
            record.setTrxDate(updatedRecord.getTrxDate());
            record.setTrxTime(updatedRecord.getTrxTime());
            return repository.save(record);
        }).orElseThrow(() -> new RuntimeException("Transaction not found"));
    }

    public boolean deleteTransaction(Long id) {
        if (repository.existsById(id)) {
            repository.deleteById(id);
            return true;
        }
        return false;
    }
}

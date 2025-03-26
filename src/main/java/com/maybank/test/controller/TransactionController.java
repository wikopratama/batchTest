package com.maybank.test.controller;

import com.maybank.test.entity.TransactionRecord;
import com.maybank.test.service.TransactionService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Optional;

@RestController
@RequestMapping("/api/transactions")
public class TransactionController {

    @Autowired
    private TransactionService transactionService;

    @GetMapping("/transactions")
    public Page<TransactionRecord> getTransactions(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(required = false) String description) {
        return transactionService.getTransactions(page, size, description);
    }


    @GetMapping("/{id}")
    public ResponseEntity<TransactionRecord> getTransactionById(@PathVariable Long id) {
        Optional<TransactionRecord> transaction = transactionService.getTransactionById(id);
        return transaction.map(ResponseEntity::ok).orElseGet(() -> ResponseEntity.notFound().build());
    }


    @PutMapping("/{id}")
    public ResponseEntity<TransactionRecord> updateTransaction(@PathVariable Long id, @RequestBody TransactionRecord transactionDetails) {
        Optional<TransactionRecord> updatedTransaction = Optional.ofNullable(transactionService.updateTransaction(id, transactionDetails));
        return updatedTransaction.map(ResponseEntity::ok).orElseGet(() -> ResponseEntity.notFound().build());
    }
}

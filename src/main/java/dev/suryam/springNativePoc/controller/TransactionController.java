package dev.suryam.springNativePoc.controller;

import dev.suryam.springNativePoc.HttpCallService;
import dev.suryam.springNativePoc.entity.Transaction;
import dev.suryam.springNativePoc.repository.TransactionRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/transactions")
public class TransactionController {

    @Autowired
    private TransactionRepository repository;

    @Autowired
    private HttpCallService httpCallService;

    @PostMapping()
    public ResponseEntity<Transaction> postTransaction(@RequestBody Transaction transaction) {
        Transaction savedTransaction = repository.save(transaction);
        return ResponseEntity.ok(savedTransaction);
    }

    @GetMapping("/all")
    public ResponseEntity<List<Transaction>> getAllTransactions() {
        List<Transaction> transactions = repository.findAll();
        return ResponseEntity.ok(transactions);
    }

    @GetMapping(value = "/states/{numberOfCalls}", produces = MediaType.APPLICATION_JSON_VALUE)
    public List<Map<String, Object>> getStates(@PathVariable("numberOfCalls") int numberOfCalls) throws Exception {
        List<Map<String, Object>> response = httpCallService.parallelHttpCalls(numberOfCalls);
        return response;
    }

    @GetMapping("/fibonacci/{fibNumber}")
    public String cpuIntensiveOperation(@PathVariable("fibNumber") int fibNumber) {
        long result = calculateFibonacci(fibNumber);
        return "Result: " + result;
    }

    private long calculateFibonacci(int n) {
        if (n <= 1) {
            return n;
        } else {
            return calculateFibonacci(n - 1) + calculateFibonacci(n - 2);
        }
    }
}

package dev.suryam.springNativePoc.controller;

import dev.suryam.springNativePoc.HttpCallService;
import dev.suryam.springNativePoc.entity.Transaction;
import dev.suryam.springNativePoc.repository.TransactionRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/transactions")
public class TransactionController {

    private static final Logger logger = LoggerFactory.getLogger(TransactionController.class);

    @Autowired
    private TransactionRepository repository;

    @Autowired
    private HttpCallService httpCallService;

    @Value("${config.config1}")
    private String config1;

    @PostMapping()
    public ResponseEntity<Transaction> postTransaction(@RequestBody Transaction transaction) {
        Random random = new Random();
        Double randomNumber = random.nextDouble(100) + 1;
        transaction.setPaymentAmount(randomNumber);
        transaction.setCustomerId(generateRandom());
        Transaction savedTransaction = repository.save(transaction);
        return ResponseEntity.ok(savedTransaction);
    }

    private String generateRandom() {
        int leftLimit = 97; // letter 'a'
        int rightLimit = 122; // letter 'z'
        int targetStringLength = 7;
        Random random = new Random();

        return random.ints(leftLimit, rightLimit + 1)
                .limit(targetStringLength)
                .collect(StringBuilder::new, StringBuilder::appendCodePoint, StringBuilder::append)
                .toString();
    }

    @GetMapping("/all")
    public ResponseEntity<List<Transaction>> getAllTransactions() {
        PageRequest pageRequest = PageRequest.of(0, 50, Sort.by("paymentAmount"));
        List<Transaction> transactions = repository.findByPaymentAmountLessThan(50.0, pageRequest);
        transactions.stream()
                .map(Transaction::getCustomerId)
                .map(String::toLowerCase)
                .forEach(logger::info);
        return ResponseEntity.ok(transactions);
    }

    @GetMapping(value = "/states/{numberOfCalls}", produces = MediaType.APPLICATION_JSON_VALUE)
    public List<Map<String, Object>> getStates(@PathVariable("numberOfCalls") int numberOfCalls) throws Exception {
        logger.error("Config based property: " + config1);
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

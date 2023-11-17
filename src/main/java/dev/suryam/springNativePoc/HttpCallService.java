package dev.suryam.springNativePoc;

import dev.suryam.springNativePoc.exception.CustomCBException;
import dev.suryam.springNativePoc.exception.CustomHttpException;
import io.github.resilience4j.circuitbreaker.CallNotPermittedException;
import io.github.resilience4j.circuitbreaker.annotation.CircuitBreaker;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.stream.Collectors;

@Service
public class HttpCallService {

    private final WebClient webClient;

    @Autowired
    public HttpCallService(WebClient.Builder webClientBuilder) {
        this.webClient = webClientBuilder.build();
    }

    @CircuitBreaker(name = "backendA", fallbackMethod = "fallback")
    public List<Map<String, Object>> parallelHttpCalls(int numberOfCalls) throws Exception {
        try {
            List<CompletableFuture<Map<String, Object>>> futures = new ArrayList<>();
            for (int i = 0; i < numberOfCalls; i++) {
                CompletableFuture<Map<String, Object>> future = makeAsyncHttpCall(i);
                futures.add(future);
            }

            CompletableFuture<Void> allOf = CompletableFuture.allOf(futures.toArray(new CompletableFuture[0]));
            allOf.get();

            return futures.stream()
                    .map(CompletableFuture::join)
                    .collect(Collectors.toList());
        } catch (Exception ex) {
            System.out.println("Exception calling external service");
            throw new CustomHttpException("");
        }
    }

    public CompletableFuture<Map<String, Object>> makeAsyncHttpCall(int index) {
        String url = "http://host.docker.internal:9000/states";
        return webClient.get()
                .uri(url)
                .retrieve()
                .bodyToMono(String.class)
                .map(responseBody -> {
                    Map<String, Object> result = new HashMap<>();
                    result.put("index", index);
                    result.put("response", responseBody);
                    return result;
                })
                .toFuture();
    }

    private List<Map<String, Object>> fallback(int index, CallNotPermittedException ex) {
        System.out.println("Circuit breaker fallback executed");
        throw new CustomCBException("Resilince4j circuit breaker fallback");
    }
}

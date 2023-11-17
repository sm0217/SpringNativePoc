package dev.suryam.springNativePoc;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;

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

    public List<Map<String, Object>> parallelHttpCalls(int numberOfCalls) throws Exception {

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
                .onErrorResume(throwable -> {
                    Map<String, Object> errorResult = new HashMap<>();
                    errorResult.put("index", index);
                    errorResult.put("error", throwable.getMessage());
                    return Mono.just(errorResult);
                })
                .toFuture();
    }
}

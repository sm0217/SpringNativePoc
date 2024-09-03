import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import javax.servlet.http.HttpServletRequest;
import java.io.BufferedReader;
import java.io.IOException;
import java.util.*;

@RestController
@RequestMapping("/mock")
public class MockController {

    // List to hold all registered mocks
    private final List<MockEndpoint> mockEndpoints = new ArrayList<>();

    // Data class to represent a mock endpoint
    private static class MockEndpoint {
        private String url;
        private String method;
        private Map<String, Object> requestBody;
        private Map<String, Object> responseBody;
        private int responseStatus;

        // Getters and Setters
        public String getUrl() { return url; }
        public void setUrl(String url) { this.url = url; }
        public String getMethod() { return method; }
        public void setMethod(String method) { this.method = method; }
        public Map<String, Object> getRequestBody() { return requestBody; }
        public void setRequestBody(Map<String, Object> requestBody) { this.requestBody = requestBody; }
        public Map<String, Object> getResponseBody() { return responseBody; }
        public void setResponseBody(Map<String, Object> responseBody) { this.responseBody = responseBody; }
        public int getResponseStatus() { return responseStatus; }
        public void setResponseStatus(int responseStatus) { this.responseStatus = responseStatus; }
    }

    // Endpoint to register a new mock
    @PostMapping("/register")
    public ResponseEntity<String> registerMock(@RequestBody MockEndpoint mockEndpoint) {
        mockEndpoints.add(mockEndpoint);
        return ResponseEntity.ok("Mock registered successfully");
    }

    // Catch-all endpoint to handle mock requests
    @RequestMapping(value = "/**", method = {RequestMethod.GET, RequestMethod.POST, RequestMethod.PUT, RequestMethod.DELETE})
    public ResponseEntity<Map<String, Object>> handleRequest(HttpServletRequest request) throws IOException {
        String url = request.getRequestURI().replaceFirst("/mock", ""); // Remove /mock prefix
        String method = request.getMethod();

        // Read the request body
        StringBuilder requestBodyBuilder = new StringBuilder();
        try (BufferedReader reader = request.getReader()) {
            String line;
            while ((line = reader.readLine()) != null) {
                requestBodyBuilder.append(line);
            }
        }
        String requestBodyStr = requestBodyBuilder.toString();
        Map<String, Object> requestBody = parseJson(requestBodyStr);

        // Find a matching mock
        Optional<MockEndpoint> matchingMock = findMatchingMock(url, method, requestBody);

        if (matchingMock.isPresent()) {
            MockEndpoint mock = matchingMock.get();
            return ResponseEntity
                    .status(mock.getResponseStatus())
                    .contentType(MediaType.APPLICATION_JSON) // Ensure response is JSON
                    .body(mock.getResponseBody());
        } else {
            Map<String, Object> errorResponse = new HashMap<>();
            errorResponse.put("error", "No mock found for the requested URL and request body");
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(errorResponse);
        }
    }

    // Helper method to parse JSON string into a Map
    private Map<String, Object> parseJson(String jsonString) {
        // This method uses a basic JSON parser. You may want to use a library like Jackson or Gson.
        if (jsonString == null || jsonString.isEmpty()) {
            return Collections.emptyMap();
        }
        try {
            // Using Jackson ObjectMapper for JSON parsing
            ObjectMapper objectMapper = new ObjectMapper();
            return objectMapper.readValue(jsonString, new TypeReference<Map<String, Object>>() {});
        } catch (IOException e) {
            throw new RuntimeException("Failed to parse JSON", e);
        }
    }

    // Helper method to find a matching mock
    private Optional<MockEndpoint> findMatchingMock(String url, String method, Map<String, Object> requestBody) {
        return mockEndpoints.stream()
                .filter(mock -> mock.getUrl().equals(url) && mock.getMethod().equalsIgnoreCase(method))
                .filter(mock -> mock.getRequestBody() == null || mock.getRequestBody().equals(requestBody))
                .findFirst();
    }
}

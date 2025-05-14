/*
 * src/main/java/ch/uzh/ifi/hase/soprafs24/service/OpenAiClient.java
 */
package ch.uzh.ifi.hase.soprafs24.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

@Component
public class OpenAiClient {
    private final Logger log = LoggerFactory.getLogger(OpenAiClient.class);
    private static final String ENDPOINT = "https://api.openai.com/v1/chat/completions";
    private final String apiKey;
    private final HttpClient client;
    private final ObjectMapper mapper = new ObjectMapper();

    public OpenAiClient() {
        this.apiKey = System.getenv("OPENAI_API_KEY");
        this.client = HttpClient.newBuilder()
                .connectTimeout(Duration.ofSeconds(5))
                .build();
    }

    public String createCompletion(String prompt) {
        try {
            String body = mapper.writeValueAsString(
                    mapper.createObjectNode()
                            .put("model", "gpt-4o")
                            .set("messages", mapper.createArrayNode()
                                    .add(mapper.createObjectNode()
                                            .put("role", "system")
                                            .put("content", "You are a helpful assistant."))
                                    .add(mapper.createObjectNode()
                                            .put("role", "user")
                                            .put("content", prompt))));

            HttpRequest req = HttpRequest.newBuilder()
                    .uri(URI.create(ENDPOINT))
                    .timeout(Duration.ofSeconds(10))
                    .header("Authorization", "Bearer " + apiKey)
                    .header("Content-Type", "application/json")
                    .POST(HttpRequest.BodyPublishers.ofString(body))
                    .build();

            log.info("Sending POST request to OpenAI API");
            HttpResponse<String> resp = client.send(req, HttpResponse.BodyHandlers.ofString());
            log.info("OpenAi api response status code: {}", resp.statusCode());
            log.debug("OpenAi api response: {}", resp.body());
            JsonNode root = mapper.readTree(resp.body());
            return root.path("choices").get(0).path("message").path("content").asText();

        } catch (IOException e) {
            throw new OpenAIClientException("Failed to call OpenAI API (I/O error)", e);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new OpenAIClientException("OpenAI API call interrupted", e);
        }
    }
}
package ch.uzh.ifi.hase.soprafs24.client;

import ch.uzh.ifi.hase.soprafs24.exceptions.OpenAIClientException;
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
    private static final Logger log = LoggerFactory.getLogger(OpenAiClient.class);

    private static final String ENDPOINT = "https://api.openai.com/v1/chat/completions";
    private static final String FIELD_ROLE = "role";
    private static final String FIELD_CONTENT = "content"; // ← estratta qui

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
            JsonNode systemMsg = mapper.createObjectNode()
                    .put(FIELD_ROLE, "system")
                    .put(FIELD_CONTENT, "You are a helpful assistant.");
            JsonNode userMsg = mapper.createObjectNode()
                    .put(FIELD_ROLE, "user")
                    .put(FIELD_CONTENT, prompt);

            String body = mapper.writeValueAsString(
                    mapper.createObjectNode()
                            .put("model", "gpt-4o")
                            .set("messages", mapper.createArrayNode()
                                    .add(systemMsg)
                                    .add(userMsg)));

            HttpRequest req = HttpRequest.newBuilder()
                    .uri(URI.create(ENDPOINT))
                    .timeout(Duration.ofSeconds(10))
                    .header("Authorization", "Bearer " + apiKey)
                    .header("Content-Type", "application/json")
                    .POST(HttpRequest.BodyPublishers.ofString(body))
                    .build();

            log.info("Sending POST request to OpenAI API");
            HttpResponse<String> resp = client.send(req, HttpResponse.BodyHandlers.ofString());
            log.info("OpenAI API response status code: {}", resp.statusCode());
            log.debug("OpenAI API response body: {}", resp.body());

            JsonNode root = mapper.readTree(resp.body());
            return root
                    .path("choices").get(0)
                    .path("message")
                    .path(FIELD_CONTENT) // ← usa la costante anche qui
                    .asText();

        } catch (IOException e) {
            throw new OpenAIClientException("Failed to call OpenAI API (I/O error)", e);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new OpenAIClientException("OpenAI API call interrupted", e);
        }
    }
}

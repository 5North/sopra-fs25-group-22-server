package ch.uzh.ifi.hase.soprafs24;

import static org.assertj.core.api.Assertions.assertThat;

import java.nio.charset.StandardCharsets;
import java.util.List;

import org.junit.jupiter.api.Test;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.web.server.LocalServerPort;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.http.*;

@SpringBootTest(classes = Application.class, webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
class ApplicationTest {

        @LocalServerPort
        int port;

        TestRestTemplate restTemplate = new TestRestTemplate();

        @Test
        void helloWorldEndpoint_returnsExpectedStringAndStatus() {
                String url = "http://localhost:" + port + "/";
                ResponseEntity<String> response = restTemplate.getForEntity(url, String.class);

                assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);

                MediaType ct = response.getHeaders().getContentType();
                assertThat(ct.getType()).isEqualTo("text");
                assertThat(ct.getSubtype()).isEqualTo("plain");
                assertThat(ct.getCharset()).isEqualTo(StandardCharsets.UTF_8);

                assertThat(response.getBody()).isEqualTo("The application is running.");
        }

}

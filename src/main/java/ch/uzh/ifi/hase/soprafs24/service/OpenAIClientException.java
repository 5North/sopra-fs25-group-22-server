package ch.uzh.ifi.hase.soprafs24.service;

public class OpenAIClientException extends RuntimeException {
    public OpenAIClientException(String message, Throwable cause) {
        super(message, cause);
    }
}

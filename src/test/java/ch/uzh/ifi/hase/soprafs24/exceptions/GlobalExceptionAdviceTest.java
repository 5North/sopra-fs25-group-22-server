package ch.uzh.ifi.hase.soprafs24.exceptions;

import static org.junit.jupiter.api.Assertions.*;

import java.lang.reflect.Constructor;
import java.nio.charset.Charset;

import org.junit.jupiter.api.Test;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.transaction.TransactionSystemException;
import org.springframework.web.client.HttpServerErrorException;
import org.springframework.web.context.request.ServletWebRequest;
import org.springframework.web.context.request.WebRequest;
import org.springframework.web.server.ResponseStatusException;

import javax.servlet.http.HttpServletRequest;

 class GlobalExceptionAdviceTest {

    private GlobalExceptionAdvice advice = new GlobalExceptionAdvice();

    @Test
     void testHandleConflictWithIllegalArgumentException() {
        IllegalArgumentException ex = new IllegalArgumentException("Invalid argument");
        HttpServletRequest servletRequest = new MockHttpServletRequest();
        WebRequest webRequest = new ServletWebRequest(servletRequest);

        ResponseEntity<Object> response = advice.handleConflict(ex, webRequest);

        assertEquals(HttpStatus.CONFLICT, response.getStatusCode());
        assertEquals("This should be application specific", response.getBody());
    }

    @Test
     void testHandleTransactionSystemException() {
        TransactionSystemException ex = new TransactionSystemException("Transaction error");
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.setRequestURI("http://localhost/test");

        ResponseStatusException rse = advice.handleTransactionSystemException(ex, request);

        assertEquals(HttpStatus.CONFLICT, rse.getStatus());
        assertEquals("Transaction error", rse.getReason());
        assertSame(ex, rse.getCause());
    }

    @Test
     void testHandleInternalServerErrorException() throws Exception {
        HttpHeaders headers = new HttpHeaders();
        Constructor<?> constructor = HttpServerErrorException.InternalServerError.class
                .getDeclaredConstructor(String.class, HttpHeaders.class, byte[].class, Charset.class);
        constructor.setAccessible(true);
        Object instance = constructor.newInstance("Internal error", headers, new byte[0], null);
        HttpServerErrorException.InternalServerError ex = (HttpServerErrorException.InternalServerError) instance;

        ResponseStatusException rse = advice.handleException(ex);

        assertEquals(HttpStatus.INTERNAL_SERVER_ERROR, rse.getStatus());
        assertEquals("500 Internal error", rse.getReason());
        assertSame(ex, rse.getCause());
    }
}

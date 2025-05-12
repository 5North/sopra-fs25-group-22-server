package ch.uzh.ifi.hase.soprafs24.websocket;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

import java.net.URI;
import java.util.HashMap;
import java.util.Map;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.http.server.ServerHttpRequest;
import org.springframework.http.server.ServerHttpResponse;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.web.socket.WebSocketHandler;
import org.springframework.test.util.ReflectionTestUtils;

import ch.uzh.ifi.hase.soprafs24.entity.User;
import ch.uzh.ifi.hase.soprafs24.service.UserService;

public class WebSocketAuthTest {

    private WebSocketAuth interceptor;
    private UserService mockUserService;
    private User testUser;

    @BeforeEach
    public void setUp() {
        interceptor = new WebSocketAuth();
        mockUserService = mock(UserService.class);

        ReflectionTestUtils.setField(interceptor, "userService", mockUserService);

        testUser = new User();
        testUser.setId(1L);
    }

    @Test
    public void beforeHandshake_validToken_authorizesAndStoresUserId() throws Exception {
        String token = "valid-token";
        URI uri = new URI("ws://localhost/lobby?token=" + token);
        ServerHttpRequest request = mock(ServerHttpRequest.class);
        when(request.getURI()).thenReturn(uri);
        ServerHttpResponse response = mock(ServerHttpResponse.class);
        WebSocketHandler handler = mock(WebSocketHandler.class);
        Map<String, Object> attrs = new HashMap<>();

        User fake = new User();
        fake.setId(42L);
        when(mockUserService.authorizeUser(token)).thenReturn(fake);

        boolean result = interceptor.beforeHandshake(request, response, handler, attrs);

        assertTrue(result);
        assertEquals(42L, attrs.get("userId"));
        verify(response, never()).setStatusCode(any());
    }

    @Test
    void beforeHandshake_InvalidToken_returnsFalse() throws Exception {
        URI uri = new URI("ws://localhost/lobby/?token=invalid-token");
        ServerHttpRequest request = mock(ServerHttpRequest.class);
        when(request.getURI()).thenReturn(uri);
        ServerHttpResponse response = mock(ServerHttpResponse.class);
        WebSocketHandler handler = mock(WebSocketHandler.class);
        Map<String, Object> attrs = new HashMap<>();

        when(mockUserService.authorizeUser("invalid-token")).thenThrow(new ResponseStatusException(HttpStatus.UNAUTHORIZED));

        boolean result = interceptor.beforeHandshake(request, response, handler, attrs);

        assertFalse(result);
        assertFalse(attrs.containsKey("userId"));
        verify(response, times(1)).setStatusCode(HttpStatus.UNAUTHORIZED);
    }

    @Test
    void beforeHandshake_missingToken_returnsFalse() throws Exception {
        URI uri = new URI("ws://localhost/lobby");
        ServerHttpRequest request = mock(ServerHttpRequest.class);
        when(request.getURI()).thenReturn(uri);
        ServerHttpResponse response = mock(ServerHttpResponse.class);
        WebSocketHandler handler = mock(WebSocketHandler.class);
        Map<String, Object> attrs = new HashMap<>();


        boolean result = interceptor.beforeHandshake(request, response, handler, attrs);

        assertFalse(result);
        assertFalse(attrs.containsKey("userId"));
        verify(response, times(1)).setStatusCode(HttpStatus.BAD_REQUEST);
    }

    @Test
    public void beforeHandshake_uriNull_throwsAndReturnsBadRequest() throws Exception {
        ServerHttpRequest request = mock(ServerHttpRequest.class);
        ServerHttpResponse response = mock(ServerHttpResponse.class);
        WebSocketHandler handler = mock(WebSocketHandler.class);
        Map<String, Object> attrs = new HashMap<>();

        URI dummy = new URI("http://localhost/lobby?foo=bar");
        when(request.getURI())
                .thenReturn(dummy)
                .thenThrow(new NullPointerException());
        when(mockUserService.authorizeUser("bar")).thenReturn(testUser);

        boolean result = interceptor.beforeHandshake(request, response, handler, attrs);

        assertFalse(result);
        verify(response).setStatusCode(HttpStatus.BAD_REQUEST);
        assertTrue(attrs.isEmpty());
    }

    @Test
    public void afterHandshake_noOp() {
        ServerHttpRequest request = mock(ServerHttpRequest.class);
        ServerHttpResponse response = mock(ServerHttpResponse.class);
        WebSocketHandler handler = mock(WebSocketHandler.class);

        assertDoesNotThrow(() -> interceptor.afterHandshake(request, response, handler, null));
    }
}

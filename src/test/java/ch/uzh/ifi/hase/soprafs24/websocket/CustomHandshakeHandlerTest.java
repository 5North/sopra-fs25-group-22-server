package ch.uzh.ifi.hase.soprafs24.websocket;

import org.junit.jupiter.api.Test;
import org.springframework.http.server.ServerHttpRequest;
import org.springframework.web.socket.WebSocketHandler;

import java.net.URI;
import java.security.Principal;
import java.util.HashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class CustomHandshakeHandlerTest {

    @Test
    void determineUser_withUserIdAttribute_returnsUserPrincipal() throws Exception {
        CustomHandshakeHandler handler = new CustomHandshakeHandler();
        ServerHttpRequest request = mock(ServerHttpRequest.class);
        when(request.getURI()).thenReturn(new URI("http://localhost/"));
        WebSocketHandler wsHandler = mock(WebSocketHandler.class);
        Map<String, Object> attrs = new HashMap<>();
        attrs.put("userId", "42");

        Principal principal = handler.determineUser(request, wsHandler, attrs);

        assertNotNull(principal);
        assertEquals("42", principal.getName());
        assertTrue(principal instanceof UserPrincipal);
    }

    @Test
    void determineUser_withoutUserId_returnsNull() throws Exception {
        CustomHandshakeHandler handler = new CustomHandshakeHandler();
        ServerHttpRequest request = mock(ServerHttpRequest.class);
        when(request.getURI()).thenReturn(new URI("http://localhost/"));
        WebSocketHandler wsHandler = mock(WebSocketHandler.class);
        Map<String, Object> attrs = new HashMap<>();

        Principal principal = handler.determineUser(request, wsHandler, attrs);

        assertNull(principal);
    }
}

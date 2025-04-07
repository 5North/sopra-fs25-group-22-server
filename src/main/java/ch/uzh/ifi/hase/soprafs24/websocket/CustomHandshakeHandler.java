package ch.uzh.ifi.hase.soprafs24.websocket;

import org.springframework.http.server.ServerHttpRequest;
import org.springframework.web.socket.WebSocketHandler;
import org.springframework.web.socket.server.support.DefaultHandshakeHandler;

import java.security.Principal;
import java.util.Map;


/**
 * Custom handshaker to assign userId
 * to the session the Custom Principal during the handshake.
 * This is necessary to be able to send messages to specific users.
 * Normally, when using a standard authentication, the username or another user
 * identifier is automatically set in the Principal during the handshake.
 * In our case with the diy token authentication we manually set the userId there.
 */
public class CustomHandshakeHandler extends DefaultHandshakeHandler {
    @Override
    protected Principal determineUser(ServerHttpRequest request,
                                      WebSocketHandler wsHandler,
                                      Map<String, Object> attributes) {

        // The userId attribute that was added in the auth interceptor before the handshake
        Object userId = attributes.get("userId");
        if (userId != null) {
            return new UserPrincipal(userId.toString());
        }
        // Fallback to original class behavior in assigning the Principal name if the useId is null
        return super.determineUser(request, wsHandler, attributes);
    }
}

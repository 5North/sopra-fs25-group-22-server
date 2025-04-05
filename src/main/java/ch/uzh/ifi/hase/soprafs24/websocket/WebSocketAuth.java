package ch.uzh.ifi.hase.soprafs24.websocket;

import ch.uzh.ifi.hase.soprafs24.entity.User;
import ch.uzh.ifi.hase.soprafs24.service.UserService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.server.ServerHttpRequest;
import org.springframework.http.server.ServerHttpResponse;
import org.springframework.web.socket.WebSocketHandler;
import org.springframework.web.socket.server.HandshakeInterceptor;

import java.util.Map;
import java.util.Objects;


public class WebSocketAuth implements HandshakeInterceptor {

    @Autowired
    private UserService userService;

    // authorize user before handshake
    @Override
    public boolean beforeHandshake(ServerHttpRequest request, ServerHttpResponse response,
                                   WebSocketHandler wsHandler, Map<String, Object> attributes) {

        // Extract and validate authentication token
        String token;
        try {
            token = Objects.requireNonNull(request.getHeaders().getFirst("Token"));
        }
        catch (NullPointerException e) {
            // return 400 if no token header was present
            response.setStatusCode(HttpStatus.BAD_REQUEST);
            return false;
        }

        User authUser = userService.authorizeUser(token);

        if (authUser != null) {
            // Save the user id as a session attribute, in order to retrieve it later
            attributes.put("userId", authUser.getId());
            return true;
        } else {
            //TODO delete
            //response.setStatusCode(HttpStatus.UNAUTHORIZED);
            return false;
        }
    }

    @Override
    public void afterHandshake(ServerHttpRequest request, ServerHttpResponse response,
                               WebSocketHandler wsHandler, Exception exception) {
        // No action needed after handshake
    }
}

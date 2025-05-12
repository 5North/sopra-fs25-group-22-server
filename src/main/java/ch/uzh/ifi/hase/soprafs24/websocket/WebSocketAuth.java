package ch.uzh.ifi.hase.soprafs24.websocket;

import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.server.ServerHttpRequest;
import org.springframework.http.server.ServerHttpResponse;
import org.springframework.util.MultiValueMap;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.web.socket.WebSocketHandler;
import org.springframework.web.socket.server.HandshakeInterceptor;
import org.springframework.web.util.UriComponents;
import org.springframework.web.util.UriComponentsBuilder;

import ch.uzh.ifi.hase.soprafs24.entity.User;
import ch.uzh.ifi.hase.soprafs24.service.UserService;


public class WebSocketAuth implements HandshakeInterceptor {

    private static final Logger log = LoggerFactory.getLogger(WebSocketAuth.class);

    @Autowired
    private UserService userService;

    // authorize user before handshake
    @Override
    public boolean beforeHandshake(ServerHttpRequest request, ServerHttpResponse response,
                                   WebSocketHandler wsHandler, Map<String, Object> attributes) {


        // Extract and validate authentication token
        String token;
        try {
            UriComponents uriComponents = UriComponentsBuilder.fromUri(request.getURI()).build();
            MultiValueMap<String, String> params = uriComponents.getQueryParams();
             token = params.getFirst("token");
        }
        catch (NullPointerException e) {
            // return 400 if no token header was present
            response.setStatusCode(HttpStatus.BAD_REQUEST);
            log.info("Handshake failed: no token provided");
            return false;
        }

        try {
            User authUser = userService.authorizeUser(token);
            // Save the user id as a session attribute, in order to retrieve it later
            log.info("Handshake authenticated successfully: user {}", authUser.getId());
            attributes.put("userId", authUser.getId());
            return true;
        }
        catch (ResponseStatusException e) {
            log.info("Handshake failed: unauthorized");
            return false;
        }
    }

    @Override
    public void afterHandshake(ServerHttpRequest request, ServerHttpResponse response,
                               WebSocketHandler wsHandler, Exception exception) {
        // No action needed after handshake
    }
}

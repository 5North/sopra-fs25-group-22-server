package ch.uzh.ifi.hase.soprafs24.websocket;

import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.server.ServerHttpRequest;
import org.springframework.http.server.ServerHttpResponse;
import org.springframework.util.MultiValueMap;
import org.springframework.web.socket.WebSocketHandler;
import org.springframework.web.socket.server.HandshakeInterceptor;
import org.springframework.web.util.UriComponents;
import org.springframework.web.util.UriComponentsBuilder;

import ch.uzh.ifi.hase.soprafs24.entity.User;
import ch.uzh.ifi.hase.soprafs24.service.UserService;


public class WebSocketAuth implements HandshakeInterceptor {

    private final Logger log = LoggerFactory.getLogger(WebSocketAuth.class);

    @Autowired
    private UserService userService;

    // authorize user before handshake
    @Override
    public boolean beforeHandshake(ServerHttpRequest request, ServerHttpResponse response,
                                   WebSocketHandler wsHandler, Map<String, Object> attributes) {

        System.out.println("====================BEFORE HANDSHAKE========================");
        System.out.println("Request: " + request.getURI());
        System.out.println("===================================================");
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

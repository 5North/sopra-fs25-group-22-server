package ch.uzh.ifi.hase.soprafs24.websocket;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.messaging.simp.config.MessageBrokerRegistry;
import org.springframework.web.socket.config.annotation.EnableWebSocketMessageBroker;
import org.springframework.web.socket.config.annotation.StompEndpointRegistry;
import org.springframework.web.socket.config.annotation.WebSocketMessageBrokerConfigurer;

@Configuration
@EnableWebSocketMessageBroker
public class WebSocketConfig implements WebSocketMessageBrokerConfigurer {

    @Bean
    public WebSocketAuth wsAuth() {
        return new WebSocketAuth();
    }

    @Override
    public void registerStompEndpoints(StompEndpointRegistry registry) {
        registry.addEndpoint("/lobby")
                .setAllowedOrigins("*")
                // adds interceptor for authentication
                .addInterceptors(wsAuth());
    }

    // sets up message routing
    @Override
    public void configureMessageBroker(MessageBrokerRegistry registry) {
        registry.enableSimpleBroker("/topic", "/queue");
                // set client pinging every xxxxx ms and expect response every xxxxx ms, else disconnect.
                //.setHeartbeatValue(new long[]{20000, 30000});
        registry.setApplicationDestinationPrefixes("/app");
    }
}

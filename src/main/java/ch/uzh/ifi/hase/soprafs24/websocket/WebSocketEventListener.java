package ch.uzh.ifi.hase.soprafs24.websocket;

import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.messaging.SessionDisconnectEvent;
import org.springframework.web.socket.messaging.SessionSubscribeEvent;
import org.springframework.web.socket.messaging.SessionUnsubscribeEvent;

@Component
public class WebSocketEventListener {

    @EventListener
    public void handleSubscribeEvent(SessionSubscribeEvent event){}

    @EventListener
    public void handleUnsubscribeEvent(SessionUnsubscribeEvent event){}

    @EventListener
    public void handleDisconnectEvent(SessionDisconnectEvent event){}
}

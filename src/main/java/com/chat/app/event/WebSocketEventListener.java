package com.chat.app.event;

import com.chat.app.model.UserSession;
import com.chat.app.service.MessagingService;
import com.chat.app.service.RoomService;
import com.chat.app.service.UserSessionService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.event.EventListener;
import org.springframework.messaging.simp.stomp.StompHeaderAccessor;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.messaging.SessionConnectedEvent;
import org.springframework.web.socket.messaging.SessionDisconnectEvent;
import org.springframework.web.socket.messaging.SessionSubscribeEvent;
import org.springframework.web.socket.messaging.SessionUnsubscribeEvent;

import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * Listens to WebSocket lifecycle events and keeps session/room state in sync.
 *
 * Events handled:
 *   CONNECTED     – a new STOMP session established
 *   DISCONNECTED  – session closed (broadcasts LEAVE to all joined rooms)
 *   SUBSCRIBE     – user subscribed to a room topic
 *   UNSUBSCRIBE   – user unsubscribed from a room topic
 */
@Component
public class WebSocketEventListener {

    private static final Logger log = LoggerFactory.getLogger(WebSocketEventListener.class);

    private final UserSessionService userSessionService;
    private final RoomService        roomService;
    private final MessagingService   messagingService;

    public WebSocketEventListener(UserSessionService userSessionService,
                                  RoomService roomService,
                                  MessagingService messagingService) {
        this.userSessionService = userSessionService;
        this.roomService        = roomService;
        this.messagingService   = messagingService;
    }

    // ── Connect ───────────────────────────────────────────────────────────

    @EventListener
    public void handleConnect(SessionConnectedEvent event) {
        StompHeaderAccessor accessor = StompHeaderAccessor.wrap(event.getMessage());
        String sessionId = accessor.getSessionId();
        log.debug("STOMP CONNECTED session={}", sessionId);
        // Username is registered later via /app/chat.join
    }

    // ── Disconnect ────────────────────────────────────────────────────────

    @EventListener
    public void handleDisconnect(SessionDisconnectEvent event) {
        StompHeaderAccessor accessor = StompHeaderAccessor.wrap(event.getMessage());
        String sessionId = accessor.getSessionId();

        // Remove from user registry
        Optional<UserSession> session = userSessionService.removeSession(sessionId);

        // Broadcast LEAVE to every room this session had joined
        List<String> vacatedRooms = roomService.removeSessionFromAllRooms(sessionId);

        session.ifPresent(us -> vacatedRooms.forEach(roomId -> {
            log.info("Broadcasting LEAVE for '{}' in #{}", us.getUsername(), roomId);
            messagingService.broadcastLeave(roomId, us.getUsername());
        }));

        if (session.isEmpty()) {
            log.debug("Disconnect for unregistered session {}", sessionId);
        }
    }

    // ── Subscribe ─────────────────────────────────────────────────────────

    @EventListener
    public void handleSubscribe(SessionSubscribeEvent event) {
        StompHeaderAccessor accessor = StompHeaderAccessor.wrap(event.getMessage());
        String sessionId   = accessor.getSessionId();
        String destination = accessor.getDestination();

        // We only care about room subscriptions: /topic/room/{roomId}
        if (destination == null || !destination.startsWith("/topic/room/")) return;

        String roomId = destination.substring("/topic/room/".length());

        // Mark this session as a member of the room
        roomService.joinRoom(roomId, sessionId);

        // Update UserSession if already registered
        userSessionService.getSession(sessionId).ifPresent(us -> us.joinRoom(roomId));

        log.debug("Session {} subscribed to #{}", sessionId, roomId);
    }

    // ── Unsubscribe ───────────────────────────────────────────────────────

    @EventListener
    public void handleUnsubscribe(SessionUnsubscribeEvent event) {
        StompHeaderAccessor accessor = StompHeaderAccessor.wrap(event.getMessage());
        String sessionId = accessor.getSessionId();

        // Spring doesn't give us the destination on unsubscribe directly;
        // we read it from the session attributes if stored
        Map<String, Object> attrs = accessor.getSessionAttributes();
        if (attrs == null) return;

        String roomId = (String) attrs.get("lastRoom");
        if (roomId != null) {
            roomService.leaveRoom(roomId, sessionId);
            userSessionService.getSession(sessionId).ifPresent(us -> us.leaveRoom(roomId));
            log.debug("Session {} unsubscribed from #{}", sessionId, roomId);
        }
    }
}

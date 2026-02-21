package com.chat.app.service;

import com.chat.app.model.ChatMessage;
import com.chat.app.model.ChatMessage.Type;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Service;

/**
 * Centralises all WebSocket broadcasting.
 *
 * Rooms use the topic pattern:  /topic/room/{roomId}
 * User-specific messages use:  /queue/user  (sent to a specific session)
 */
@Service
public class MessagingService {

    private static final Logger log = LoggerFactory.getLogger(MessagingService.class);

    private final SimpMessagingTemplate messenger;
    private final RoomService           roomService;

    public MessagingService(SimpMessagingTemplate messenger, RoomService roomService) {
        this.messenger   = messenger;
        this.roomService = roomService;
    }

    // ── Room broadcasts ───────────────────────────────────────────────────

    /**
     * Broadcasts a chat message to all subscribers of a room,
     * and persists it in the room's history.
     */
    public void broadcastToRoom(ChatMessage message) {
        if (message.getRoom() == null || message.getRoom().isBlank()) {
            log.warn("Dropping message with no room from sender '{}'", message.getSender());
            return;
        }
        // Persist first
        if (message.getType() == Type.CHAT) {
            roomService.saveMessage(message);
        }
        String destination = roomDestination(message.getRoom());
        messenger.convertAndSend(destination, message);
        log.debug("Broadcast [{}] to {} from '{}'", message.getType(), destination, message.getSender());
    }

    /**
     * Broadcasts a JOIN system message to a room.
     */
    public void broadcastJoin(String roomId, String username) {
        ChatMessage msg = new ChatMessage(
                username + " joined the room", username, Type.JOIN, roomId);
        broadcastToRoom(msg);
    }

    /**
     * Broadcasts a LEAVE system message to a room.
     */
    public void broadcastLeave(String roomId, String username) {
        ChatMessage msg = new ChatMessage(
                username + " left the room", username, Type.LEAVE, roomId);
        broadcastToRoom(msg);
    }

    /**
     * Broadcasts a typing indicator to a room.
     * Typing indicators are NOT persisted.
     */
    public void broadcastTyping(String roomId, String username, boolean isTyping) {
        ChatMessage msg = new ChatMessage(
                isTyping ? "typing" : "stopped", username, Type.TYPING, roomId);
        messenger.convertAndSend(roomDestination(roomId), msg);
    }

    // ── User-specific messages ────────────────────────────────────────────

    /**
     * Sends a message directly to a specific session (e.g. error feedback,
     * history replay on join).
     *
     * @param sessionId the target STOMP session ID
     * @param message   the message to deliver
     */
    public void sendToSession(String sessionId, ChatMessage message) {
        messenger.convertAndSendToUser(sessionId, "/queue/user", message,
                sessionHeaders(sessionId));
        log.debug("Sent private [{}] to session {}", message.getType(), sessionId);
    }

    /**
     * Sends an ERROR message to a specific session.
     */
    public void sendError(String sessionId, String errorText) {
        ChatMessage err = new ChatMessage(errorText, "System", Type.ERROR, null);
        sendToSession(sessionId, err);
    }

    // ── Helpers ───────────────────────────────────────────────────────────

    public static String roomDestination(String roomId) {
        return "/topic/room/" + roomId;
    }

    private org.springframework.messaging.MessageHeaders sessionHeaders(String sessionId) {
        org.springframework.messaging.simp.SimpMessageHeaderAccessor accessor =
                org.springframework.messaging.simp.SimpMessageHeaderAccessor.create(
                        org.springframework.messaging.simp.SimpMessageType.MESSAGE);
        accessor.setSessionId(sessionId);
        accessor.setLeaveMutable(true);
        return accessor.getMessageHeaders();
    }
}

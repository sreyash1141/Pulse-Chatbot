package com.chat.app.controller;

import com.chat.app.model.ChatMessage;
import com.chat.app.model.ChatMessage.Type;
import com.chat.app.model.UserSession;
import com.chat.app.service.MessagingService;
import com.chat.app.service.RoomService;
import com.chat.app.service.UserSessionService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.messaging.simp.SimpMessageHeaderAccessor;
import org.springframework.stereotype.Controller;

@Controller
public class ChatController {

    private static final Logger log = LoggerFactory.getLogger(ChatController.class);
    private static final int MAX_MESSAGE_LENGTH = 1000;
    private static final int HISTORY_REPLAY     = 50;

    private final MessagingService   messagingService;
    private final RoomService        roomService;
    private final UserSessionService userSessionService;

    public ChatController(MessagingService messagingService,
                          RoomService roomService,
                          UserSessionService userSessionService) {
        this.messagingService   = messagingService;
        this.roomService        = roomService;
        this.userSessionService = userSessionService;
    }

    @MessageMapping("/chat.join")
    public void joinRoom(@Payload ChatMessage message,
                         SimpMessageHeaderAccessor headerAccessor) {

        String sessionId = headerAccessor.getSessionId();
        String username  = sanitiseUsername(message.getSender());
        String roomId    = sanitiseRoom(message.getRoom());

        if (username == null) { messagingService.sendError(sessionId, "Username cannot be blank."); return; }
        if (roomId == null)   { messagingService.sendError(sessionId, "Room ID cannot be blank."); return; }

        userSessionService.getSession(sessionId).ifPresentOrElse(
                existing -> existing.joinRoom(roomId),
                () -> { UserSession s = userSessionService.registerSession(sessionId, username); s.joinRoom(roomId); }
        );

        roomService.joinRoom(roomId, sessionId);

        // Replay history privately to the joining user
        roomService.getHistory(roomId, HISTORY_REPLAY)
                   .forEach(msg -> messagingService.sendToSession(sessionId, msg));

        messagingService.broadcastJoin(roomId, username);
        log.info("'{}' joined #{} [session={}]", username, roomId, sessionId);
    }

    @MessageMapping("/chat.send")
    public void sendMessage(@Payload ChatMessage message,
                            SimpMessageHeaderAccessor headerAccessor) {

        String sessionId = headerAccessor.getSessionId();
        String username  = sanitiseUsername(message.getSender());
        String roomId    = sanitiseRoom(message.getRoom());
        String content   = message.getContent();

        if (username == null || roomId == null) { messagingService.sendError(sessionId, "Invalid sender or room."); return; }
        if (content == null || content.isBlank()) { messagingService.sendError(sessionId, "Message cannot be empty."); return; }
        if (content.length() > MAX_MESSAGE_LENGTH) { messagingService.sendError(sessionId, "Message too long (max " + MAX_MESSAGE_LENGTH + " chars)."); return; }
        if (!roomService.roomExists(roomId)) { messagingService.sendError(sessionId, "Room #" + roomId + " does not exist."); return; }

        ChatMessage clean = new ChatMessage(content.trim(), username, Type.CHAT, roomId);
        messagingService.broadcastToRoom(clean);
        log.debug("'{}' sent message to #{}", username, roomId);
    }

    @MessageMapping("/chat.leave")
    public void leaveRoom(@Payload ChatMessage message,
                          SimpMessageHeaderAccessor headerAccessor) {

        String sessionId = headerAccessor.getSessionId();
        String username  = sanitiseUsername(message.getSender());
        String roomId    = sanitiseRoom(message.getRoom());
        if (username == null || roomId == null) return;

        roomService.leaveRoom(roomId, sessionId);
        userSessionService.getSession(sessionId).ifPresent(s -> s.leaveRoom(roomId));
        messagingService.broadcastLeave(roomId, username);
        log.info("'{}' left #{} [session={}]", username, roomId, sessionId);
    }

    @MessageMapping("/chat.typing")
    public void typing(@Payload ChatMessage message) {
        String username = sanitiseUsername(message.getSender());
        String roomId   = sanitiseRoom(message.getRoom());
        if (username == null || roomId == null) return;
        boolean isTyping = "true".equalsIgnoreCase(message.getContent());
        messagingService.broadcastTyping(roomId, username, isTyping);
    }

    private String sanitiseUsername(String raw) {
        if (raw == null || raw.isBlank()) return null;
        return raw.trim().substring(0, Math.min(raw.trim().length(), 32));
    }

    private String sanitiseRoom(String raw) {
        if (raw == null || raw.isBlank()) return null;
        try { return RoomService.sanitise(raw); } catch (IllegalArgumentException e) { return null; }
    }
}

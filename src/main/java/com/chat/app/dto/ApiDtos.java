package com.chat.app.dto;

import com.chat.app.model.ChatMessage;
import java.time.Instant;
import java.util.List;

/**
 * Data Transfer Objects for REST API responses.
 * Keeps the API contract clean and separate from internal models.
 */
public class ApiDtos {

    // ── Room ──────────────────────────────────────────────────────────────

    /** Summary of a room returned in list endpoints */
    public static class RoomSummary {
        private String  id;
        private String  description;
        private int     memberCount;
        private int     messageCount;
        private Instant createdAt;

        public RoomSummary(String id, String description, int memberCount,
                           int messageCount, Instant createdAt) {
            this.id           = id;
            this.description  = description;
            this.memberCount  = memberCount;
            this.messageCount = messageCount;
            this.createdAt    = createdAt;
        }

        public String  getId()           { return id; }
        public String  getDescription()  { return description; }
        public int     getMemberCount()  { return memberCount; }
        public int     getMessageCount() { return messageCount; }
        public Instant getCreatedAt()    { return createdAt; }
    }

    /** Request body for creating a new room */
    public static class CreateRoomRequest {
        private String id;
        private String description;

        public String getId()                        { return id; }
        public void   setId(String id)               { this.id = id; }
        public String getDescription()               { return description; }
        public void   setDescription(String desc)    { this.description = desc; }
    }

    // ── Messages ──────────────────────────────────────────────────────────

    /** Paginated message history response */
    public static class MessageHistoryResponse {
        private String             roomId;
        private List<ChatMessage>  messages;
        private int                total;

        public MessageHistoryResponse(String roomId, List<ChatMessage> messages) {
            this.roomId   = roomId;
            this.messages = messages;
            this.total    = messages.size();
        }

        public String            getRoomId()   { return roomId; }
        public List<ChatMessage> getMessages() { return messages; }
        public int               getTotal()    { return total; }
    }

    // ── Users ─────────────────────────────────────────────────────────────

    /** User info returned in room member lists */
    public static class UserInfo {
        private String  sessionId;
        private String  username;
        private Instant connectedAt;

        public UserInfo(String sessionId, String username, Instant connectedAt) {
            this.sessionId   = sessionId;
            this.username    = username;
            this.connectedAt = connectedAt;
        }

        public String  getSessionId()   { return sessionId; }
        public String  getUsername()    { return username; }
        public Instant getConnectedAt() { return connectedAt; }
    }

    // ── General ───────────────────────────────────────────────────────────

    /** Generic API response wrapper */
    public static class ApiResponse<T> {
        private boolean success;
        private String  message;
        private T       data;

        public ApiResponse(boolean success, String message, T data) {
            this.success = success;
            this.message = message;
            this.data    = data;
        }

        public static <T> ApiResponse<T> ok(T data) {
            return new ApiResponse<>(true, "OK", data);
        }

        public static <T> ApiResponse<T> ok(String message, T data) {
            return new ApiResponse<>(true, message, data);
        }

        public static <T> ApiResponse<T> error(String message) {
            return new ApiResponse<>(false, message, null);
        }

        public boolean isSuccess() { return success; }
        public String  getMessage() { return message; }
        public T       getData()    { return data; }
    }
}

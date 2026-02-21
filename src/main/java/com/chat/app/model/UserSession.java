package com.chat.app.model;

import java.time.Instant;
import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

/**
 * Tracks an active WebSocket user session.
 * One session = one browser tab / connection.
 */
public class UserSession {

    private final String    sessionId;
    private final String    username;
    private final Instant   connectedAt;
    private final Set<String> joinedRooms = Collections.synchronizedSet(new HashSet<>());

    public UserSession(String sessionId, String username) {
        this.sessionId   = sessionId;
        this.username    = username;
        this.connectedAt = Instant.now();
    }

    public void joinRoom(String roomId)  { joinedRooms.add(roomId); }
    public void leaveRoom(String roomId) { joinedRooms.remove(roomId); }
    public boolean isInRoom(String roomId) { return joinedRooms.contains(roomId); }

    public String    getSessionId()   { return sessionId; }
    public String    getUsername()    { return username; }
    public Instant   getConnectedAt() { return connectedAt; }
    public Set<String> getJoinedRooms() { return Collections.unmodifiableSet(joinedRooms); }
}

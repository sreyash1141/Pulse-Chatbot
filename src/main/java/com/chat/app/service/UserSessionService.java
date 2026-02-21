package com.chat.app.service;

import com.chat.app.model.UserSession;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

/**
 * Tracks all active WebSocket user sessions.
 *
 * A "session" is one browser connection identified by its
 * STOMP session ID. The same user can have multiple sessions
 * (e.g. two open tabs).
 */
@Service
public class UserSessionService {

    private static final Logger log = LoggerFactory.getLogger(UserSessionService.class);

    /** sessionId → UserSession */
    private final Map<String, UserSession> sessions = new ConcurrentHashMap<>();

    // ── Session lifecycle ─────────────────────────────────────────────────

    /**
     * Registers a new session when a user connects and sets their username.
     */
    public UserSession registerSession(String sessionId, String username) {
        UserSession session = new UserSession(sessionId, username);
        sessions.put(sessionId, session);
        log.info("User '{}' connected [session={}]", username, sessionId);
        return session;
    }

    /**
     * Removes a session on disconnect.
     *
     * @return the removed session, or empty if not found
     */
    public Optional<UserSession> removeSession(String sessionId) {
        UserSession removed = sessions.remove(sessionId);
        if (removed != null) {
            log.info("User '{}' disconnected [session={}]", removed.getUsername(), sessionId);
        }
        return Optional.ofNullable(removed);
    }

    /**
     * Looks up a session by ID.
     */
    public Optional<UserSession> getSession(String sessionId) {
        return Optional.ofNullable(sessions.get(sessionId));
    }

    // ── Room-scoped queries ───────────────────────────────────────────────

    /**
     * Returns all sessions that have joined a specific room.
     */
    public List<UserSession> getSessionsInRoom(String roomId) {
        return sessions.values().stream()
                .filter(s -> s.isInRoom(roomId))
                .toList();
    }

    /**
     * Returns usernames of all users currently in a room (deduplicated).
     */
    public Set<String> getUsernamesInRoom(String roomId) {
        return getSessionsInRoom(roomId).stream()
                .map(UserSession::getUsername)
                .collect(Collectors.toCollection(LinkedHashSet::new));
    }

    /**
     * Count of distinct users (by username) in a room.
     */
    public int getUserCountInRoom(String roomId) {
        return (int) getSessionsInRoom(roomId).stream()
                .map(UserSession::getUsername)
                .distinct()
                .count();
    }

    // ── Global queries ────────────────────────────────────────────────────

    /**
     * All currently connected sessions.
     */
    public Collection<UserSession> getAllSessions() {
        return Collections.unmodifiableCollection(sessions.values());
    }

    /**
     * Total number of connected sessions.
     */
    public int getTotalSessionCount() {
        return sessions.size();
    }

    /**
     * Total distinct users connected (by username).
     */
    public long getDistinctUserCount() {
        return sessions.values().stream()
                .map(UserSession::getUsername)
                .distinct()
                .count();
    }

    /**
     * Returns true if at least one session with the given username is connected.
     */
    public boolean isUserOnline(String username) {
        return sessions.values().stream()
                .anyMatch(s -> s.getUsername().equalsIgnoreCase(username));
    }
}

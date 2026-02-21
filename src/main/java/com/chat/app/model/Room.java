package com.chat.app.model;

import java.time.Instant;
import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.Set;

/**
 * Represents a chat room.
 * Tracks its members and a capped history of recent messages.
 */
public class Room {

    private static final int MAX_HISTORY = 100;

    private final String  id;
    private final String  description;
    private final Instant createdAt;

    /** Active session IDs in this room */
    private final Set<String> memberSessionIds = Collections.synchronizedSet(new LinkedHashSet<>());

    /** Capped ring buffer of recent messages */
    private final java.util.ArrayDeque<ChatMessage> history =
            new java.util.ArrayDeque<>(MAX_HISTORY);

    public Room(String id, String description) {
        this.id          = id;
        this.description = description;
        this.createdAt   = Instant.now();
    }

    // ── Members ───────────────────────────────────────────────────────────

    public void addMember(String sessionId) {
        memberSessionIds.add(sessionId);
    }

    public void removeMember(String sessionId) {
        memberSessionIds.remove(sessionId);
    }

    public boolean hasMember(String sessionId) {
        return memberSessionIds.contains(sessionId);
    }

    public int getMemberCount() {
        return memberSessionIds.size();
    }

    public Set<String> getMemberSessionIds() {
        return Collections.unmodifiableSet(memberSessionIds);
    }

    // ── History ───────────────────────────────────────────────────────────

    public synchronized void addMessage(ChatMessage message) {
        if (history.size() >= MAX_HISTORY) {
            history.pollFirst();
        }
        history.addLast(message);
    }

    public synchronized java.util.List<ChatMessage> getHistory() {
        return java.util.List.copyOf(history);
    }

    public synchronized void clearHistory() {
        history.clear();
    }

    // ── Getters ───────────────────────────────────────────────────────────

    public String  getId()          { return id; }
    public String  getDescription() { return description; }
    public Instant getCreatedAt()   { return createdAt; }
}

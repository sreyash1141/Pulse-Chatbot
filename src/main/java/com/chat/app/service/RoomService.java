package com.chat.app.service;

import com.chat.app.model.ChatMessage;
import com.chat.app.model.Room;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Manages the lifecycle of chat rooms:
 *   - Creating and deleting rooms
 *   - Room membership (join/leave)
 *   - Per-room message history
 */
@Service
public class RoomService {

    private static final Logger log = LoggerFactory.getLogger(RoomService.class);

    /** roomId → Room */
    private final Map<String, Room> rooms = new ConcurrentHashMap<>();

    public RoomService() {
        // Seed default rooms on startup
        createRoom("general",  "General discussion for everyone");
        createRoom("dev",      "Engineering and code talk");
        createRoom("design",   "UI/UX, branding, and visuals");
        createRoom("random",   "Memes, fun, off-topic");
        createRoom("releases", "Deployment updates and changelogs");
        log.info("RoomService initialised with {} default rooms", rooms.size());
    }

    // ── Room CRUD ─────────────────────────────────────────────────────────

    /**
     * Creates a new room. No-op if a room with that ID already exists.
     *
     * @param id          unique room identifier (e.g. "general")
     * @param description human-readable description
     * @return the created (or existing) Room
     */
    public Room createRoom(String id, String description) {
        String sanitised = sanitise(id);
        return rooms.computeIfAbsent(sanitised, k -> {
            log.info("Creating room: #{}", sanitised);
            return new Room(sanitised, description);
        });
    }

    /**
     * Returns a Room by ID, or empty if not found.
     */
    public Optional<Room> getRoom(String id) {
        return Optional.ofNullable(rooms.get(sanitise(id)));
    }

    /**
     * Returns all rooms, sorted alphabetically by ID.
     */
    public List<Room> getAllRooms() {
        return rooms.values().stream()
                .sorted(Comparator.comparing(Room::getId))
                .toList();
    }

    /**
     * Deletes a room. Default rooms cannot be deleted.
     *
     * @throws IllegalArgumentException if the room is a default room or does not exist
     */
    public void deleteRoom(String id) {
        String sanitised = sanitise(id);
        if (isDefaultRoom(sanitised)) {
            throw new IllegalArgumentException("Cannot delete default room: #" + sanitised);
        }
        Room removed = rooms.remove(sanitised);
        if (removed == null) {
            throw new IllegalArgumentException("Room not found: #" + sanitised);
        }
        log.info("Deleted room: #{}", sanitised);
    }

    public boolean roomExists(String id) {
        return rooms.containsKey(sanitise(id));
    }

    // ── Membership ────────────────────────────────────────────────────────

    /**
     * Adds a session to a room. Creates the room if it does not exist yet.
     */
    public Room joinRoom(String roomId, String sessionId) {
        Room room = rooms.computeIfAbsent(sanitise(roomId),
                k -> new Room(k, "Custom room"));
        room.addMember(sessionId);
        log.debug("Session {} joined #{}", sessionId, roomId);
        return room;
    }

    /**
     * Removes a session from a room.
     */
    public void leaveRoom(String roomId, String sessionId) {
        getRoom(roomId).ifPresent(room -> {
            room.removeMember(sessionId);
            log.debug("Session {} left #{}", sessionId, roomId);
        });
    }

    /**
     * Removes a session from ALL rooms (called on disconnect).
     */
    public List<String> removeSessionFromAllRooms(String sessionId) {
        List<String> vacatedRooms = new ArrayList<>();
        rooms.values().forEach(room -> {
            if (room.hasMember(sessionId)) {
                room.removeMember(sessionId);
                vacatedRooms.add(room.getId());
            }
        });
        if (!vacatedRooms.isEmpty()) {
            log.debug("Session {} removed from rooms: {}", sessionId, vacatedRooms);
        }
        return vacatedRooms;
    }

    // ── Message History ───────────────────────────────────────────────────

    /**
     * Appends a message to a room's history buffer.
     */
    public void saveMessage(ChatMessage message) {
        getRoom(message.getRoom()).ifPresent(room -> room.addMessage(message));
    }

    /**
     * Returns the message history for a room, newest last.
     *
     * @param roomId target room
     * @param limit  max number of messages to return (0 = all)
     */
    public List<ChatMessage> getHistory(String roomId, int limit) {
        return getRoom(roomId)
                .map(room -> {
                    List<ChatMessage> history = room.getHistory();
                    if (limit > 0 && history.size() > limit) {
                        return history.subList(history.size() - limit, history.size());
                    }
                    return history;
                })
                .orElse(List.of());
    }

    /**
     * Clears the message history for a room.
     */
    public void clearHistory(String roomId) {
        getRoom(roomId).ifPresent(room -> {
            room.clearHistory();
            log.info("Cleared history for #{}", roomId);
        });
    }

    // ── Helpers ───────────────────────────────────────────────────────────

    private static final Set<String> DEFAULT_ROOMS =
            Set.of("general", "dev", "design", "random", "releases");

    public boolean isDefaultRoom(String id) {
        return DEFAULT_ROOMS.contains(id);
    }

    /** Lowercases and strips non-alphanumeric-hyphen chars */
    public static String sanitise(String id) {
        if (id == null || id.isBlank()) throw new IllegalArgumentException("Room ID cannot be blank");
        return id.toLowerCase().replaceAll("[^a-z0-9\\-]", "-");
    }
}

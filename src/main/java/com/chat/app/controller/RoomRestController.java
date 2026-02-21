package com.chat.app.controller;

import com.chat.app.dto.ApiDtos.*;
import com.chat.app.model.Room;
import com.chat.app.service.RoomService;
import com.chat.app.service.UserSessionService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * REST API for room management.
 *
 * GET    /api/rooms                  – list all rooms
 * POST   /api/rooms                  – create a new room
 * GET    /api/rooms/{id}             – get single room details
 * DELETE /api/rooms/{id}             – delete a custom room
 * GET    /api/rooms/{id}/messages    – get message history
 * DELETE /api/rooms/{id}/messages    – clear message history
 * GET    /api/rooms/{id}/users       – list users currently in the room
 */
@RestController
@RequestMapping("/api/rooms")
@CrossOrigin(origins = "*")
public class RoomRestController {

    private final RoomService        roomService;
    private final UserSessionService userSessionService;

    public RoomRestController(RoomService roomService, UserSessionService userSessionService) {
        this.roomService        = roomService;
        this.userSessionService = userSessionService;
    }

    // ── List rooms ────────────────────────────────────────────────────────

    @GetMapping
    public ResponseEntity<ApiResponse<List<RoomSummary>>> listRooms() {
        List<RoomSummary> summaries = roomService.getAllRooms().stream()
                .map(this::toSummary)
                .toList();
        return ResponseEntity.ok(ApiResponse.ok(summaries));
    }

    // ── Create room ───────────────────────────────────────────────────────

    @PostMapping
    public ResponseEntity<ApiResponse<RoomSummary>> createRoom(
            @RequestBody CreateRoomRequest request) {

        if (request.getId() == null || request.getId().isBlank()) {
            return ResponseEntity.badRequest()
                    .body(ApiResponse.error("Room ID is required."));
        }
        try {
            Room room = roomService.createRoom(
                    request.getId(),
                    request.getDescription() != null ? request.getDescription() : "");
            return ResponseEntity.status(HttpStatus.CREATED)
                    .body(ApiResponse.ok("Room created", toSummary(room)));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(ApiResponse.error(e.getMessage()));
        }
    }

    // ── Get single room ───────────────────────────────────────────────────

    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse<RoomSummary>> getRoom(@PathVariable String id) {
        return roomService.getRoom(id)
                .map(room -> ResponseEntity.ok(ApiResponse.ok(toSummary(room))))
                .orElseGet(() -> ResponseEntity.status(HttpStatus.NOT_FOUND)
                        .body(ApiResponse.error("Room #" + id + " not found.")));
    }

    // ── Delete room ───────────────────────────────────────────────────────

    @DeleteMapping("/{id}")
    public ResponseEntity<ApiResponse<Void>> deleteRoom(@PathVariable String id) {
        try {
            roomService.deleteRoom(id);
            return ResponseEntity.ok(ApiResponse.ok("Room deleted.", null));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(ApiResponse.error(e.getMessage()));
        }
    }

    // ── Message history ───────────────────────────────────────────────────

    @GetMapping("/{id}/messages")
    public ResponseEntity<ApiResponse<MessageHistoryResponse>> getMessages(
            @PathVariable String id,
            @RequestParam(defaultValue = "50") int limit) {

        if (!roomService.roomExists(id)) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(ApiResponse.error("Room #" + id + " not found."));
        }
        var messages = roomService.getHistory(id, limit);
        return ResponseEntity.ok(ApiResponse.ok(new MessageHistoryResponse(id, messages)));
    }

    @DeleteMapping("/{id}/messages")
    public ResponseEntity<ApiResponse<Void>> clearMessages(@PathVariable String id) {
        if (!roomService.roomExists(id)) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(ApiResponse.error("Room #" + id + " not found."));
        }
        roomService.clearHistory(id);
        return ResponseEntity.ok(ApiResponse.ok("History cleared.", null));
    }

    // ── Room users ────────────────────────────────────────────────────────

    @GetMapping("/{id}/users")
    public ResponseEntity<ApiResponse<List<UserInfo>>> getUsersInRoom(@PathVariable String id) {
        if (!roomService.roomExists(id)) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(ApiResponse.error("Room #" + id + " not found."));
        }
        List<UserInfo> users = userSessionService.getSessionsInRoom(id).stream()
                .map(s -> new UserInfo(s.getSessionId(), s.getUsername(), s.getConnectedAt()))
                .toList();
        return ResponseEntity.ok(ApiResponse.ok(users));
    }

    // ── Helper ────────────────────────────────────────────────────────────

    private RoomSummary toSummary(Room room) {
        return new RoomSummary(
                room.getId(),
                room.getDescription(),
                room.getMemberCount(),
                room.getHistory().size(),
                room.getCreatedAt()
        );
    }
}

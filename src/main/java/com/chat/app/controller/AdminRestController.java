package com.chat.app.controller;

import com.chat.app.dto.ApiDtos.*;
import com.chat.app.service.RoomService;
import com.chat.app.service.UserSessionService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.Instant;
import java.util.List;
import java.util.Map;
@RestController
@RequestMapping("/api")
@CrossOrigin(origins = "*")
public class AdminRestController {

    private final RoomService        roomService;
    private final UserSessionService userSessionService;

    public AdminRestController(RoomService roomService, UserSessionService userSessionService) {
        this.roomService        = roomService;
        this.userSessionService = userSessionService;
    }

    @GetMapping("/health")
    public ResponseEntity<ApiResponse<Map<String, Object>>> health() {
        return ResponseEntity.ok(ApiResponse.ok(Map.of(
                "status",    "UP",
                "timestamp", Instant.now().toString()
        )));
    }

    @GetMapping("/stats")
    public ResponseEntity<ApiResponse<Map<String, Object>>> stats() {
        return ResponseEntity.ok(ApiResponse.ok(Map.of(
                "totalRooms",       roomService.getAllRooms().size(),
                "totalSessions",    userSessionService.getTotalSessionCount(),
                "distinctUsers",    userSessionService.getDistinctUserCount(),
                "timestamp",        Instant.now().toString()
        )));
    }

    @GetMapping("/users")
    public ResponseEntity<ApiResponse<List<UserInfo>>> getAllUsers() {
        List<UserInfo> users = userSessionService.getAllSessions().stream()
                .map(s -> new UserInfo(s.getSessionId(), s.getUsername(), s.getConnectedAt()))
                .toList();
        return ResponseEntity.ok(ApiResponse.ok(users));
    }

    @GetMapping("/users/{username}/online")
    public ResponseEntity<ApiResponse<Map<String, Object>>> isOnline(@PathVariable String username) {
        boolean online = userSessionService.isUserOnline(username);
        return ResponseEntity.ok(ApiResponse.ok(Map.of(
                "username", username,
                "online",   online
        )));
    }
}

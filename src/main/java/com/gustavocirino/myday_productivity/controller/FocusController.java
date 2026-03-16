package com.gustavocirino.myday_productivity.controller;

import com.gustavocirino.myday_productivity.dto.FocusSessionDTO;
import com.gustavocirino.myday_productivity.dto.FocusStartRequestDTO;
import com.gustavocirino.myday_productivity.dto.FocusStatsDTO;
import com.gustavocirino.myday_productivity.model.User;
import com.gustavocirino.myday_productivity.repository.UserRepository;
import com.gustavocirino.myday_productivity.service.FocusService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/focus")
@CrossOrigin(origins = "*")
@RequiredArgsConstructor
public class FocusController {

    private final FocusService focusService;
    private final UserRepository userRepository;

    @PostMapping("/start")
    public ResponseEntity<FocusSessionDTO> startSession(
            @RequestBody FocusStartRequestDTO request,
            @RequestHeader(name = "X-Auth-Token") String authToken) {
        User user = resolveUser(authToken);
        return ResponseEntity.status(HttpStatus.CREATED).body(focusService.startSession(request, user));
    }

    @PatchMapping("/{id}/pause")
    public ResponseEntity<FocusSessionDTO> pauseSession(
            @PathVariable Long id,
            @RequestHeader(name = "X-Auth-Token") String authToken) {
        User user = resolveUser(authToken);
        return ResponseEntity.ok(focusService.pauseSession(id, user));
    }

    @PatchMapping("/{id}/resume")
    public ResponseEntity<FocusSessionDTO> resumeSession(
            @PathVariable Long id,
            @RequestHeader(name = "X-Auth-Token") String authToken) {
        User user = resolveUser(authToken);
        return ResponseEntity.ok(focusService.resumeSession(id, user));
    }

    @PatchMapping("/{id}/complete")
    public ResponseEntity<FocusSessionDTO> completeSession(
            @PathVariable Long id,
            @RequestHeader(name = "X-Auth-Token") String authToken) {
        User user = resolveUser(authToken);
        return ResponseEntity.ok(focusService.completeSession(id, user));
    }

    @PatchMapping("/{id}/next-cycle")
    public ResponseEntity<FocusSessionDTO> nextCycle(
            @PathVariable Long id,
            @RequestHeader(name = "X-Auth-Token") String authToken) {
        User user = resolveUser(authToken);
        return ResponseEntity.ok(focusService.nextCycle(id, user));
    }

    @PatchMapping("/{id}/cancel")
    public ResponseEntity<FocusSessionDTO> cancelSession(
            @PathVariable Long id,
            @RequestHeader(name = "X-Auth-Token") String authToken) {
        User user = resolveUser(authToken);
        return ResponseEntity.ok(focusService.cancelSession(id, user));
    }

    @GetMapping("/active")
    public ResponseEntity<FocusSessionDTO> getActiveSession(
            @RequestHeader(name = "X-Auth-Token") String authToken) {
        User user = resolveUser(authToken);
        FocusSessionDTO active = focusService.getActiveSession(user);
        if (active == null) {
            return ResponseEntity.noContent().build();
        }
        return ResponseEntity.ok(active);
    }

    @GetMapping("/stats")
    public ResponseEntity<FocusStatsDTO> getStats(
            @RequestHeader(name = "X-Auth-Token") String authToken) {
        User user = resolveUser(authToken);
        return ResponseEntity.ok(focusService.getStats(user));
    }

    private User resolveUser(String authToken) {
        if (authToken == null || authToken.isBlank()) {
            throw new RuntimeException("Token de autenticação obrigatório");
        }
        return userRepository.findByAuthToken(authToken)
                .orElseThrow(() -> new RuntimeException("Token de autenticação inválido"));
    }
}

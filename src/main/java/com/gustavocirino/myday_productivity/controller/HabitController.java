package com.gustavocirino.myday_productivity.controller;

import com.gustavocirino.myday_productivity.dto.BadgeDTO;
import com.gustavocirino.myday_productivity.dto.GamificationDTO;
import com.gustavocirino.myday_productivity.dto.HabitStreakDTO;
import com.gustavocirino.myday_productivity.model.User;
import com.gustavocirino.myday_productivity.repository.UserRepository;
import com.gustavocirino.myday_productivity.service.HabitStreakService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/habits")
@CrossOrigin(origins = "*")
@RequiredArgsConstructor
public class HabitController {

    private final HabitStreakService habitStreakService;
    private final UserRepository userRepository;

    @PostMapping("/checkin")
    public ResponseEntity<HabitStreakDTO> checkIn(
            @RequestBody Map<String, String> body,
            @RequestHeader(name = "X-Auth-Token") String authToken) {
        User user = resolveUser(authToken);
        String habitType = body.getOrDefault("habitType", "daily_completion");
        return ResponseEntity.ok(habitStreakService.checkIn(user, habitType));
    }

    @GetMapping
    public ResponseEntity<GamificationDTO> getGamificationData(
            @RequestHeader(name = "X-Auth-Token") String authToken) {
        User user = resolveUser(authToken);
        return ResponseEntity.ok(habitStreakService.getGamificationData(user));
    }

    @GetMapping("/badges")
    public ResponseEntity<List<BadgeDTO>> getBadges(
            @RequestHeader(name = "X-Auth-Token") String authToken) {
        User user = resolveUser(authToken);
        return ResponseEntity.ok(habitStreakService.getBadges(user));
    }

    private User resolveUser(String authToken) {
        Object principal = org.springframework.security.core.context.SecurityContextHolder.getContext().getAuthentication().getPrincipal();
        if (principal instanceof User) {
            return (User) principal;
        }
        throw new RuntimeException("Token de autenticação inválido");
    }
}

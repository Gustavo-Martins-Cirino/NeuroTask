package com.gustavocirino.myday_productivity.controller;

import com.gustavocirino.myday_productivity.dto.SmartRescheduleSuggestionDTO;
import com.gustavocirino.myday_productivity.model.User;
import com.gustavocirino.myday_productivity.repository.UserRepository;
import com.gustavocirino.myday_productivity.service.SmartRescheduleService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/reschedule")
@CrossOrigin(origins = "*")
@RequiredArgsConstructor
public class SmartRescheduleController {

    private final SmartRescheduleService smartRescheduleService;
    private final UserRepository userRepository;

    @GetMapping("/suggestions")
    public ResponseEntity<List<SmartRescheduleSuggestionDTO>> getSuggestions(
            @RequestHeader(name = "X-Auth-Token") String authToken) {
        User user = resolveUser(authToken);
        return ResponseEntity.ok(smartRescheduleService.getSuggestions(user));
    }

    @PostMapping("/accept")
    public ResponseEntity<Void> acceptSuggestion(
            @RequestBody Map<String, String> body,
            @RequestHeader(name = "X-Auth-Token") String authToken) {
        User user = resolveUser(authToken);
        Long taskId = Long.parseLong(body.get("taskId"));
        LocalDateTime newStart = LocalDateTime.parse(body.get("suggestedStart"));
        LocalDateTime newEnd = LocalDateTime.parse(body.get("suggestedEnd"));
        smartRescheduleService.acceptSuggestion(taskId, newStart, newEnd, user);
        return ResponseEntity.ok().build();
    }

    private User resolveUser(String authToken) {
        Object principal = org.springframework.security.core.context.SecurityContextHolder.getContext().getAuthentication().getPrincipal();
        if (principal instanceof User) {
            return (User) principal;
        }
        throw new RuntimeException("Token de autenticação inválido");
    }
}

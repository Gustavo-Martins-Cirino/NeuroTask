package com.gustavocirino.myday_productivity.controller;

import com.gustavocirino.myday_productivity.dto.WeeklyReviewDTO;
import com.gustavocirino.myday_productivity.model.User;
import com.gustavocirino.myday_productivity.repository.UserRepository;
import com.gustavocirino.myday_productivity.service.WeeklyReviewService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/weekly-review")
@CrossOrigin(origins = "*")
@RequiredArgsConstructor
public class WeeklyReviewController {

    private final WeeklyReviewService weeklyReviewService;
    private final UserRepository userRepository;

    @PostMapping("/generate")
    public ResponseEntity<WeeklyReviewDTO> generateReview(
            @RequestHeader(name = "X-Auth-Token") String authToken,
            @RequestParam(name = "force", defaultValue = "false") boolean force) {
        User user = resolveUser(authToken);
        return ResponseEntity.status(HttpStatus.CREATED).body(weeklyReviewService.generateReview(user, force));
    }

    @GetMapping
    public ResponseEntity<List<WeeklyReviewDTO>> getHistory(
            @RequestHeader(name = "X-Auth-Token") String authToken) {
        User user = resolveUser(authToken);
        return ResponseEntity.ok(weeklyReviewService.getReviewHistory(user));
    }

    @GetMapping("/{id}")
    public ResponseEntity<WeeklyReviewDTO> getReviewById(
            @PathVariable Long id,
            @RequestHeader(name = "X-Auth-Token") String authToken) {
        User user = resolveUser(authToken);
        return ResponseEntity.ok(weeklyReviewService.getReviewById(id, user));
    }

    private User resolveUser(String authToken) {
        if (authToken == null || authToken.isBlank()) {
            throw new RuntimeException("Token de autenticação obrigatório");
        }
        return userRepository.findByAuthToken(authToken)
                .orElseThrow(() -> new RuntimeException("Token de autenticação inválido"));
    }
}

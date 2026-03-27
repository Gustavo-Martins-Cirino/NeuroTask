package com.gustavocirino.myday_productivity.controller;

import com.gustavocirino.myday_productivity.dto.EnergyLogCreateDTO;
import com.gustavocirino.myday_productivity.dto.EnergyLogDTO;
import com.gustavocirino.myday_productivity.dto.EnergyPatternDTO;
import com.gustavocirino.myday_productivity.model.User;
import com.gustavocirino.myday_productivity.repository.UserRepository;
import com.gustavocirino.myday_productivity.service.EnergyService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/energy")
@CrossOrigin(origins = "*")
@RequiredArgsConstructor
public class EnergyController {

    private final EnergyService energyService;
    private final UserRepository userRepository;

    @PostMapping
    public ResponseEntity<EnergyLogDTO> logEnergy(
            @RequestBody EnergyLogCreateDTO dto,
            @RequestHeader(name = "X-Auth-Token") String authToken) {
        User user = resolveUser(authToken);
        return ResponseEntity.status(HttpStatus.CREATED).body(energyService.logEnergy(dto, user));
    }

    @GetMapping("/today")
    public ResponseEntity<List<EnergyLogDTO>> getTodayLogs(
            @RequestHeader(name = "X-Auth-Token") String authToken) {
        User user = resolveUser(authToken);
        return ResponseEntity.ok(energyService.getTodayLogs(user));
    }

    @GetMapping("/history")
    public ResponseEntity<List<EnergyLogDTO>> getHistory(
            @RequestHeader(name = "X-Auth-Token") String authToken,
            @RequestParam(defaultValue = "14") int days) {
        User user = resolveUser(authToken);
        return ResponseEntity.ok(energyService.getHistory(user, days));
    }

    @GetMapping("/patterns")
    public ResponseEntity<EnergyPatternDTO> getPatterns(
            @RequestHeader(name = "X-Auth-Token") String authToken) {
        User user = resolveUser(authToken);
        return ResponseEntity.ok(energyService.getPatterns(user));
    }

       private User resolveUser(String authToken) {
        return (User) org.springframework.security.core.context.SecurityContextHolder.getContext().getAuthentication().getPrincipal();
    }
}

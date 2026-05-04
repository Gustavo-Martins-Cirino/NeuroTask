package com.gustavocirino.myday_productivity.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.gustavocirino.myday_productivity.dto.TaskCreateDTO;
import com.gustavocirino.myday_productivity.model.User;
import com.gustavocirino.myday_productivity.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.RequestPostProcessor;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc(addFilters = false)
@Transactional
public class TaskControllerIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private UserRepository userRepository;

    private User testUser;

    @BeforeEach
    void setUp() {
        testUser = new User();
        testUser.setEmail("qa@neurotask.com");
        testUser.setPassword("123456");
        testUser = userRepository.save(testUser);
    }

    private RequestPostProcessor authenticatedUser() {
        return request -> {
            User managedUser = userRepository.findById(testUser.getId())
                    .orElseThrow(() -> new IllegalStateException("Usuário de teste não encontrado"));

            SecurityContext context = SecurityContextHolder.createEmptyContext();
            context.setAuthentication(new UsernamePasswordAuthenticationToken(managedUser, null));
            SecurityContextHolder.setContext(context);
            return request;
        };
    }

    @Test
    @DisplayName("Caminho Feliz: Deve criar uma tarefa com sucesso com dados válidos")
    void testCreateTask_HappyPath_ReturnsCreated() throws Exception {
        LocalDateTime start = LocalDateTime.parse("2026-04-23T09:00:00");
        LocalDateTime end = LocalDateTime.parse("2026-04-23T10:00:00");

        TaskCreateDTO dto = new TaskCreateDTO(
                "Finalizar Relatório de QA",
                "Escrever testes de integração para o neurotask",
                "HIGH",
                start,
                end,
                null
        );

        mockMvc.perform(post("/api/tasks")
                        .with(authenticatedUser())
                        .header("X-Auth-Token", "test-token")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(dto)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.title").value("Finalizar Relatório de QA"))
                .andExpect(jsonPath("$.status").value("SCHEDULED"));
    }

    @Test
    @DisplayName("Entrada Inválida: Deve retornar erro 400 ao tentar criar tarefa sem título")
    void testCreateTask_InvalidInput_ReturnsBadRequest() throws Exception {
        TaskCreateDTO dto = new TaskCreateDTO(
                "",
                "Descrição válida",
                "MEDIUM",
                null,
                null,
                null
        );

        mockMvc.perform(post("/api/tasks")
                        .with(authenticatedUser())
                        .header("X-Auth-Token", "test-token")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(dto)))
                .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("Caso Limite: Deve retornar erro 404 ao buscar uma tarefa com ID que não existe")
    void testGetTaskById_EdgeCase_ReturnsNotFound() throws Exception {
        long nonExistentTaskId = 99999L;

        mockMvc.perform(get("/api/tasks/" + nonExistentTaskId)
                        .with(authenticatedUser())
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isNotFound());
    }
}

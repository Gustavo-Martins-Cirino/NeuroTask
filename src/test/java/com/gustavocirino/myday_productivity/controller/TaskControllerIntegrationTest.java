package com.gustavocirino.myday_productivity.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.gustavocirino.myday_productivity.dto.TaskCreateDTO;
import com.gustavocirino.myday_productivity.model.User;
import com.gustavocirino.myday_productivity.repository.TaskRepository;
import com.gustavocirino.myday_productivity.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import java.util.Collections;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest(properties = { "OPENAI_API_KEY=test-key", "openai.api.key=test-key" })
@AutoConfigureMockMvc(addFilters = false)
@Transactional // Garante que o banco seja limpo após cada teste
public class TaskControllerIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private TaskRepository taskRepository;

    private User testUser;

    @BeforeEach
    void setUp() {
        taskRepository.deleteAll();
        userRepository.deleteAll();

        // Criar usuário padrão para os testes (já que o Controller depende de um User)
        testUser = new User();
        testUser.setEmail("qa@neurotask.com");
        testUser.setPassword("123456");
        testUser = userRepository.save(testUser);

        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken(testUser, null, Collections.emptyList()));
    }

    @Test
    @DisplayName("Caminho Feliz: Deve criar uma tarefa com sucesso com dados válidos")
    void testCreateTask_HappyPath_ReturnsCreated() throws Exception {
        TaskCreateDTO dto = new TaskCreateDTO(
                "Finalizar Relatório de QA",
                "Escrever testes de integração para o neurotask",
                "HIGH", // priority
            null, // startTime
            null, // endTime
                null // color
        );

        mockMvc.perform(post("/api/tasks")
                .header("X-Auth-Token", "test-token")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(dto)))
            .andExpect(status().isCreated())
                .andExpect(jsonPath("$.title").value("Finalizar Relatório de QA"))
                .andExpect(jsonPath("$.status").value("PENDING"));
    }

    @Test
    @DisplayName("Entrada Inválida: Deve retornar erro 400 ao tentar criar tarefa sem título")
    void testCreateTask_InvalidInput_ReturnsBadRequest() throws Exception {
        TaskCreateDTO dto = new TaskCreateDTO(
                "", // Título inválido (vazio)
                "Descrição válida",
                "MEDIUM",
                null,
                null,
                null
        );

        mockMvc.perform(post("/api/tasks")
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
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isNotFound());
    }
}

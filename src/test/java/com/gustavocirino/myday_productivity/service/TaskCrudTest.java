package com.gustavocirino.myday_productivity.service;

import com.gustavocirino.myday_productivity.dto.TaskCreateDTO;
import com.gustavocirino.myday_productivity.dto.TaskResponseDTO;
import com.gustavocirino.myday_productivity.exception.TaskNotFoundException;
import com.gustavocirino.myday_productivity.model.Task;
import com.gustavocirino.myday_productivity.model.User;
import com.gustavocirino.myday_productivity.model.enums.TaskPriority;
import com.gustavocirino.myday_productivity.model.enums.TaskStatus;
import com.gustavocirino.myday_productivity.repository.FocusSessionRepository;
import com.gustavocirino.myday_productivity.repository.ProcrastinationRecordRepository;
import com.gustavocirino.myday_productivity.repository.TaskCategoryRepository;
import com.gustavocirino.myday_productivity.repository.TaskRepository;
import com.gustavocirino.myday_productivity.repository.UserRepository;
import com.gustavocirino.myday_productivity.service.ai.ProcrastinationService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class TaskCrudTest {

    @Mock
    private TaskRepository taskRepository;

    @Mock
    private UserRepository userRepository;

    @Mock
    private ProcrastinationService procrastinationService;

    @Mock
    private FocusSessionRepository focusSessionRepository;

    @Mock
    private ProcrastinationRecordRepository procrastinationRecordRepository;

    @Mock
    private TaskCategoryRepository taskCategoryRepository;

    @InjectMocks
    private TaskService taskService;

    private User testUser;
    private Task testTask;

    @BeforeEach
    void setUp() {
        testUser = new User();
        testUser.setId(1L);
        testUser.setEmail("testuser@neurotask.com");
        testUser.setPassword("123456");

        testTask = new Task();
        testTask.setId(100L);
        testTask.setTitle("Test Task");
        testTask.setDescription("Test Description");
        testTask.setPriority(TaskPriority.HIGH);
        testTask.setStatus(TaskStatus.PENDING);
        testTask.setUser(testUser);
    }

    @Test
    void testCreateTask_Success() {
        LocalDateTime start = LocalDateTime.parse("2026-04-23T09:00:00");
        LocalDateTime end = LocalDateTime.parse("2026-04-23T10:00:00");

        TaskCreateDTO createDto = new TaskCreateDTO(
                "Test Task",
                "Test Description",
                "HIGH",
                start,
                end,
                "#34a853"
        );

        testTask.setStartTime(start);
        testTask.setEndTime(end);
        testTask.setStatus(TaskStatus.SCHEDULED);

        when(taskRepository.save(any(Task.class))).thenReturn(testTask);

        TaskResponseDTO result = taskService.createTask(createDto, testUser);

        assertNotNull(result);
        assertEquals(100L, result.id());
        assertEquals("Test Task", result.title());
        assertEquals(TaskStatus.SCHEDULED, result.status());
        verify(taskRepository, times(1)).save(any(Task.class));
    }

    @Test
    void testCreateTask_ValidationError() {
        TaskCreateDTO invalidDto = new TaskCreateDTO("", "Empty Title", "LOW", null, null, null);

        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class, () ->
                taskService.createTask(invalidDto, testUser)
        );

        assertTrue(
                exception.getMessage().contains("título")
                        || exception.getMessage().contains("title")
                        || exception.getMessage().toLowerCase().contains("obrigatório")
                        || exception.getMessage().toLowerCase().contains("vazio")
        );
        verify(taskRepository, never()).save(any(Task.class));
    }

    @Test
    void testGetTaskById_NotFound() {
        when(taskRepository.findById(999L)).thenReturn(Optional.empty());

        TaskNotFoundException exception = assertThrows(TaskNotFoundException.class, () ->
                taskService.getTaskById(999L)
        );

        assertTrue(exception.getMessage().contains("Tarefa não encontrada"));
        verify(taskRepository, times(1)).findById(999L);
    }
}

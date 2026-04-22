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

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

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
        TaskCreateDTO createDto = new TaskCreateDTO("Test Task", "Test Description", "HIGH", null, null, null);
        
        when(taskRepository.save(any(Task.class))).thenReturn(testTask);

        TaskResponseDTO result = taskService.createTask(createDto, testUser);

        assertNotNull(result);
        assertEquals(100L, result.id());
        assertEquals("Test Task", result.title());
        assertEquals(TaskStatus.PENDING, result.status());
        verify(taskRepository, times(1)).save(any(Task.class));
    }

    @Test
    void testCreateTask_ValidationError() {
        TaskCreateDTO invalidDto = new TaskCreateDTO("", "Empty Title", "LOW", null, null, null);
        
        Exception exception = assertThrows(IllegalArgumentException.class, () -> {
            taskService.createTask(invalidDto, testUser);
        });

        assertEquals("Título não pode ser vazio", exception.getMessage());
        verify(taskRepository, never()).save(any(Task.class));
    }

    @Test
    void testGetTaskById_NotFound() {
        when(taskRepository.findById(999L)).thenReturn(Optional.empty());

        TaskNotFoundException exception = assertThrows(TaskNotFoundException.class, () -> {
            taskService.getTaskById(999L);
        });

        assertEquals("Tarefa não encontrada com ID: 999", exception.getMessage());
        verify(taskRepository, times(1)).findById(999L);
    }
}

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
class TaskServiceTest {

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

    private User mockUser;

    @BeforeEach
    void setUp() {
        mockUser = new User();
        mockUser.setId(1L);
        mockUser.setEmail("user@test.com");
    }

    @Test
    void testCreateTask_HappyPath_ReturnsTaskResponse() {
        // Arrange
        TaskCreateDTO dto = new TaskCreateDTO("Nova Tarefa", "Descricao", "HIGH", null, null, "#ffffff");
        
        Task savedTask = new Task();
        savedTask.setId(10L);
        savedTask.setTitle(dto.title());
        savedTask.setDescription(dto.description());
        savedTask.setStatus(TaskStatus.PENDING);
        savedTask.setPriority(TaskPriority.HIGH);
        savedTask.setUser(mockUser);

        when(taskRepository.save(any(Task.class))).thenReturn(savedTask);

        // Act
        TaskResponseDTO result = taskService.createTask(dto, mockUser);

        // Assert
        assertNotNull(result);
        assertEquals(10L, result.id());
        assertEquals("Nova Tarefa", result.title());
        assertEquals(TaskStatus.PENDING, result.status());
        verify(taskRepository, times(1)).save(any(Task.class));
    }

    @Test
    void testCreateTask_InvalidInput_ThrowsExceptionWhenTitleIsNull() {
        // Arrange
        TaskCreateDTO dto = new TaskCreateDTO("", "Descricao", "MEDIUM", null, null, null);

        // Act & Assert
        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class, () -> {
            taskService.createTask(dto, mockUser);
        });

        assertEquals("Título não pode ser vazio", exception.getMessage());
        verify(taskRepository, never()).save(any(Task.class));
    }

    @Test
    void testGetTaskById_EdgeCase_ThrowsExceptionWhenTaskNotFound() {
        // Arrange
        Long nonExistentId = 999L;
        when(taskRepository.findById(nonExistentId)).thenReturn(Optional.empty());

        // Act & Assert
        TaskNotFoundException exception = assertThrows(TaskNotFoundException.class, () -> {
            taskService.getTaskById(nonExistentId);
        });

        // O Spring lidará com código 404 graças à anotação na exceção original
        assertNotNull(exception);
        verify(taskRepository, times(1)).findById(nonExistentId);
    }
}

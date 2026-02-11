package com.gustavocirino.myday_productivity.service.ai.analytics;

import com.gustavocirino.myday_productivity.dto.AnalyticsSummaryDTO;
import com.gustavocirino.myday_productivity.model.enums.TaskStatus;
import com.gustavocirino.myday_productivity.repository.TaskRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@DisplayName("AnalyticsService Tests")
class AnalyticsServiceTest {

    @Mock
    private TaskRepository taskRepository;

    @InjectMocks
    private AnalyticsService analyticsService;

    private LocalDateTime start;
    private LocalDateTime end;

    @BeforeEach
    void setUp() {
        start = LocalDateTime.of(2025, 12, 1, 0, 0);
        end = LocalDateTime.of(2025, 12, 7, 23, 59);
    }

    @Test
    @DisplayName("Should calculate summary with correct metrics")
    void testGetSummary_WithValidData() {
        // Arrange
        long totalPlanned = 10L;
        long totalCompleted = 7L;
        long totalCreated = 5L;

        when(taskRepository.countByStatusScheduledAndStartTimeBetween(start, end))
                .thenReturn(totalPlanned);
        when(taskRepository.countByStatusDoneAndStartTimeBetween(start, end))
                .thenReturn(totalCompleted);
        when(taskRepository.countByCreatedAtBetween(start, end))
                .thenReturn(totalCreated);

        // Act
        AnalyticsSummaryDTO result = analyticsService.getSummary(start, end);

        // Assert
        assertNotNull(result);
        assertEquals(start, result.periodStart());
        assertEquals(end, result.periodEnd());
        assertEquals(totalPlanned, result.totalPlanned());
        assertEquals(totalCompleted, result.totalCompleted());
        assertEquals(totalCreated, result.totalCreated());
        
        // Completion rate should be (7/10) * 100 = 70.0
        assertEquals(70.0, result.completionRate(), 0.01);
        
        // Period is 7 days (Dec 1-7), so avgPlannedPerDay = 10/7 = ~1.43
        assertEquals(10.0 / 7.0, result.avgPlannedPerDay(), 0.01);
    }

    @Test
    @DisplayName("Should handle zero planned tasks")
    void testGetSummary_WithZeroPlannedTasks() {
        // Arrange
        when(taskRepository.countByStatusScheduledAndStartTimeBetween(start, end))
                .thenReturn(0L);
        when(taskRepository.countByStatusDoneAndStartTimeBetween(start, end))
                .thenReturn(0L);
        when(taskRepository.countByCreatedAtBetween(start, end))
                .thenReturn(3L);

        // Act
        AnalyticsSummaryDTO result = analyticsService.getSummary(start, end);

        // Assert
        assertNotNull(result);
        assertEquals(0L, result.totalPlanned());
        assertEquals(0L, result.totalCompleted());
        assertEquals(3L, result.totalCreated());
        assertEquals(0.0, result.completionRate(), 0.01);
        assertEquals(0.0, result.avgPlannedPerDay(), 0.01);
    }

    @Test
    @DisplayName("Should handle single day period")
    void testGetSummary_WithSingleDayPeriod() {
        // Arrange
        LocalDateTime sameDay = LocalDateTime.of(2025, 12, 1, 0, 0);
        LocalDateTime sameDayEnd = LocalDateTime.of(2025, 12, 1, 23, 59);
        
        when(taskRepository.countByStatusScheduledAndStartTimeBetween(sameDay, sameDayEnd))
                .thenReturn(5L);
        when(taskRepository.countByStatusDoneAndStartTimeBetween(sameDay, sameDayEnd))
                .thenReturn(3L);
        when(taskRepository.countByCreatedAtBetween(sameDay, sameDayEnd))
                .thenReturn(2L);

        // Act
        AnalyticsSummaryDTO result = analyticsService.getSummary(sameDay, sameDayEnd);

        // Assert
        assertNotNull(result);
        assertEquals(5L, result.totalPlanned());
        assertEquals(3L, result.totalCompleted());
        assertEquals(2L, result.totalCreated());
        
        // Completion rate = (3/5) * 100 = 60.0
        assertEquals(60.0, result.completionRate(), 0.01);
        
        // Single day, so avgPlannedPerDay = 5/1 = 5.0
        assertEquals(5.0, result.avgPlannedPerDay(), 0.01);
    }

    @Test
    @DisplayName("Should handle 100% completion rate")
    void testGetSummary_WithFullCompletion() {
        // Arrange
        when(taskRepository.countByStatusScheduledAndStartTimeBetween(start, end))
                .thenReturn(10L);
        when(taskRepository.countByStatusDoneAndStartTimeBetween(start, end))
                .thenReturn(10L);
        when(taskRepository.countByCreatedAtBetween(start, end))
                .thenReturn(10L);

        // Act
        AnalyticsSummaryDTO result = analyticsService.getSummary(start, end);

        // Assert
        assertNotNull(result);
        assertEquals(10L, result.totalPlanned());
        assertEquals(10L, result.totalCompleted());
        assertEquals(100.0, result.completionRate(), 0.01);
    }
}

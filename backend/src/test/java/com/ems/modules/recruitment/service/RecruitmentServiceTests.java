package com.ems.modules.recruitment.service;

import com.ems.exception.BadRequestException;
import com.ems.modules.auth.model.User;
import com.ems.modules.auth.repository.UserRepository;
import com.ems.modules.recruitment.dto.ApplicationDto;
import com.ems.modules.recruitment.dto.ApplicationSubmitRequest;
import com.ems.modules.recruitment.model.Application;
import com.ems.modules.recruitment.model.ApplicationStatus;
import com.ems.modules.recruitment.model.Job;
import com.ems.modules.recruitment.model.JobStatus;
import com.ems.modules.recruitment.repository.ApplicationRepository;
import com.ems.modules.recruitment.repository.InterviewRepository;
import com.ems.modules.recruitment.repository.JobRepository;
import com.ems.modules.employee.repository.EmployeeRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.context.ApplicationEventPublisher;

import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
public class RecruitmentServiceTests {

    @Mock
    private ApplicationRepository applicationRepository;
    @Mock
    private JobRepository jobRepository;
    @Mock
    private UserRepository userRepository;
    @Mock
    private EmployeeRepository employeeRepository;
    @Mock
    private InterviewRepository interviewRepository;
    @Mock
    private ApplicationEventPublisher eventPublisher;

    @InjectMocks
    private RecruitmentServiceImpl recruitmentService;

    private User candidate;
    private Job openJob;
    private Job closedJob;
    private ApplicationSubmitRequest submitRequest;

    @BeforeEach
    void setUp() {
        candidate = User.builder()
                .id(UUID.randomUUID())
                .username("student1")
                .email("student1@university.edu")
                .enabled(true)
                .build();

        openJob = Job.builder()
                .id(1L)
                .title("Software Engineer Intern")
                .status(JobStatus.OPEN)
                .build();

        closedJob = Job.builder()
                .id(2L)
                .title("Data Analyst Intern")
                .status(JobStatus.CLOSED)
                .build();

        submitRequest = new ApplicationSubmitRequest();
        submitRequest.setJobId(1L);
        submitRequest.setResumeUrl("/api/v1/recruitment/resumes/my-cv.pdf");
        submitRequest.setCoverLetter("I am highly motivated.");
    }

    @Test
    void applyForJob_Success() {
        // Arrange
        when(userRepository.findByUsername("student1")).thenReturn(Optional.of(candidate));
        when(jobRepository.findById(1L)).thenReturn(Optional.of(openJob));
        when(applicationRepository.existsByJobIdAndCandidateId(1L, candidate.getId())).thenReturn(false);
        
        // Act
        ApplicationDto result = recruitmentService.applyForJob("student1", submitRequest);

        // Assert
        assertNotNull(result);
        assertEquals(ApplicationStatus.SUBMITTED.name(), result.getStatus());
        verify(applicationRepository, times(1)).save(any(Application.class));
    }

    @Test
    void applyForJob_ClosedJob_ThrowsBadRequestException() {
        // Arrange
        submitRequest.setJobId(2L);
        when(userRepository.findByUsername("student1")).thenReturn(Optional.of(candidate));
        when(jobRepository.findById(2L)).thenReturn(Optional.of(closedJob));

        // Act & Assert
        assertThrows(BadRequestException.class, () -> {
            recruitmentService.applyForJob("student1", submitRequest);
        });
        verify(applicationRepository, never()).save(any(Application.class));
    }
}

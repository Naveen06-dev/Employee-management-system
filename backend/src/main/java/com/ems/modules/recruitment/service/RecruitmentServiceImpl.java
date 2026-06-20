package com.ems.modules.recruitment.service;

import com.ems.exception.BadRequestException;
import com.ems.exception.DuplicateResourceException;
import com.ems.exception.ResourceNotFoundException;
import com.ems.modules.auth.model.User;
import com.ems.modules.auth.repository.UserRepository;
import com.ems.modules.employee.model.Employee;
import com.ems.modules.employee.repository.EmployeeRepository;
import com.ems.modules.recruitment.dto.*;
import com.ems.modules.recruitment.mapper.ApplicationMapper;
import com.ems.modules.recruitment.mapper.InterviewMapper;
import com.ems.modules.recruitment.model.*;
import com.ems.modules.recruitment.repository.ApplicationRepository;
import com.ems.modules.recruitment.repository.InterviewRepository;
import com.ems.modules.recruitment.repository.JobRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.security.access.AccessDeniedException;
import com.ems.common.event.InterviewScheduledEvent;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Slf4j
@Service
public class RecruitmentServiceImpl implements RecruitmentService {

    private final ApplicationRepository applicationRepository;
    private final JobRepository jobRepository;
    private final UserRepository userRepository;
    private final EmployeeRepository employeeRepository;
    private final InterviewRepository interviewRepository;
    private final ApplicationEventPublisher eventPublisher;

    public RecruitmentServiceImpl(
            ApplicationRepository applicationRepository,
            JobRepository jobRepository,
            UserRepository userRepository,
            EmployeeRepository employeeRepository,
            InterviewRepository interviewRepository,
            ApplicationEventPublisher eventPublisher) {
        this.applicationRepository = applicationRepository;
        this.jobRepository = jobRepository;
        this.userRepository = userRepository;
        this.employeeRepository = employeeRepository;
        this.interviewRepository = interviewRepository;
        this.eventPublisher = eventPublisher;
    }

    @Override
    @Transactional
    @CacheEvict(value = "dashboardStats", allEntries = true)
    public ApplicationDto applyForJob(String username, ApplicationSubmitRequest request) {
        log.info("Candidate user {} is applying for job posting ID: {}", username, request.getJobId());

        User candidate = userRepository.findByUsername(username)
                .orElseThrow(() -> new ResourceNotFoundException("Candidate user not found: " + username));

        Job job = jobRepository.findById(request.getJobId())
                .orElseThrow(() -> new ResourceNotFoundException("Job posting not found with ID: " + request.getJobId()));

        if (job.getStatus() != JobStatus.OPEN) {
            throw new BadRequestException("This job posting is currently closed or in draft status.");
        }

        if (applicationRepository.existsByJobIdAndCandidateId(job.getId(), candidate.getId())) {
            throw new DuplicateResourceException("You have already submitted an application for this job posting.");
        }

        Application application = Application.builder()
                .job(job)
                .candidate(candidate)
                .resumeUrl(request.getResumeUrl())
                .coverLetter(request.getCoverLetter())
                .status(ApplicationStatus.SUBMITTED)
                .build();

        applicationRepository.save(application);
        log.info("Application submitted successfully. ID: {}", application.getId());

        // eventPublisher.publishEvent(new ApplicationSubmittedEvent(application));

        return ApplicationMapper.toDto(application);
    }

    @Override
    @Transactional(readOnly = true)
    public List<ApplicationDto> getCandidateApplications(String username) {
        log.info("Fetching job applications for candidate: {}", username);
        User candidate = userRepository.findByUsername(username)
                .orElseThrow(() -> new ResourceNotFoundException("Candidate user not found: " + username));

        return applicationRepository.findByCandidateId(candidate.getId()).stream()
                .map(ApplicationMapper::toDto)
                .collect(Collectors.toList());
    }

    @Override
    @Transactional(readOnly = true)
    public List<ApplicationDto> getAllApplications() {
        log.info("Fetching all job applications in the system");
        return applicationRepository.findAll().stream()
                .map(ApplicationMapper::toDto)
                .collect(Collectors.toList());
    }

    @Override
    @Transactional
    @CacheEvict(value = "dashboardStats", allEntries = true)
    public ApplicationDto changeApplicationStatus(Long applicationId, String statusStr) {
        log.info("Transitioning application ID: {} status to {}", applicationId, statusStr);
        Application application = applicationRepository.findById(applicationId)
                .orElseThrow(() -> new ResourceNotFoundException("Application not found with ID: " + applicationId));

        ApplicationStatus status;
        try {
            status = ApplicationStatus.valueOf(statusStr.toUpperCase());
        } catch (IllegalArgumentException e) {
            throw new BadRequestException("Invalid application status. Must be SUBMITTED, SCREENING, INTERVIEWING, OFFERED, or REJECTED.");
        }

        application.setStatus(status);
        applicationRepository.save(application);
        log.info("Application ID: {} status updated to {}", applicationId, status);

        // eventPublisher.publishEvent(new ApplicationStatusChangedEvent(application));

        return ApplicationMapper.toDto(application);
    }

    @Override
    @Transactional
    @CacheEvict(value = "dashboardStats", allEntries = true)
    public InterviewDto scheduleInterview(InterviewScheduleRequest request) {
        log.info("Scheduling interview for application ID: {}", request.getApplicationId());

        Application application = applicationRepository.findById(request.getApplicationId())
                .orElseThrow(() -> new ResourceNotFoundException("Application not found with ID: " + request.getApplicationId()));

        if (application.getStatus() == ApplicationStatus.REJECTED) {
            throw new BadRequestException("Cannot schedule an interview for a rejected application.");
        }

        Employee interviewer = employeeRepository.findById(request.getInterviewerId())
                .orElseThrow(() -> new ResourceNotFoundException("Interviewer employee profile not found with ID: " + request.getInterviewerId()));

        Interview interview = Interview.builder()
                .application(application)
                .interviewer(interviewer)
                .scheduledAt(request.getScheduledAt())
                .durationMinutes(request.getDurationMinutes())
                .meetLink(request.getMeetLink())
                .status(InterviewStatus.SCHEDULED)
                .build();

        interviewRepository.save(interview);

        // Auto transition application status to INTERVIEWING
        application.setStatus(ApplicationStatus.INTERVIEWING);
        applicationRepository.save(application);

        log.info("Interview scheduled successfully for application ID: {}, Interview ID: {}", application.getId(), interview.getId());

        // Publish event for notifications
        eventPublisher.publishEvent(new InterviewScheduledEvent(interview));

        return InterviewMapper.toDto(interview);
    }

    @Override
    @Transactional(readOnly = true)
    public List<InterviewDto> getInterviewsForInterviewer(String username) {
        log.info("Fetching scheduled interviews for interviewer username: {}", username);
        Employee employee = employeeRepository.findByUserUsername(username)
                .orElseThrow(() -> new ResourceNotFoundException("Employee profile not found for user: " + username));

        return interviewRepository.findByInterviewerId(employee.getId()).stream()
                .map(InterviewMapper::toDto)
                .collect(Collectors.toList());
    }

    @Override
    @Transactional
    @CacheEvict(value = "dashboardStats", allEntries = true)
    public InterviewDto submitInterviewFeedback(Long interviewId, InterviewFeedbackRequest request, String reviewerUsername) {
        log.info("Submitting interview feedback for interview ID: {} by reviewer: {}", interviewId, reviewerUsername);

        Interview interview = interviewRepository.findById(interviewId)
                .orElseThrow(() -> new ResourceNotFoundException("Interview record not found with ID: " + interviewId));

        if (interview.getStatus() != InterviewStatus.SCHEDULED) {
            throw new BadRequestException("Feedback can only be submitted for scheduled interviews.");
        }

        Employee reviewer = employeeRepository.findByUserUsername(reviewerUsername)
                .orElseThrow(() -> new ResourceNotFoundException("Employee profile not found for reviewer user: " + reviewerUsername));

        // Validate that this reviewer is the assigned interviewer
        if (!interview.getInterviewer().getId().equals(reviewer.getId())) {
            throw new AccessDeniedException("You are not authorized to submit feedback for this interview session. You are not the assigned interviewer.");
        }

        interview.setFeedback(request.getFeedback());
        interview.setRating(request.getRating());
        interview.setStatus(InterviewStatus.COMPLETED);

        interviewRepository.save(interview);
        log.info("Interview ID: {} feedback submitted successfully", interviewId);

        // eventPublisher.publishEvent(new InterviewCompletedEvent(interview));

        return InterviewMapper.toDto(interview);
    }
}

package com.ems.modules.recruitment.controller;

import com.ems.common.dto.ApiResponse;
import com.ems.modules.recruitment.dto.*;
import com.ems.modules.recruitment.service.RecruitmentService;
import jakarta.validation.Valid;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.security.Principal;
import java.util.List;

@Slf4j
@RestController
@RequestMapping("/api/v1/recruitment")
public class RecruitmentController {

    private final RecruitmentService recruitmentService;

    public RecruitmentController(RecruitmentService recruitmentService) {
        this.recruitmentService = recruitmentService;
    }

    @PostMapping("/apply")
    @PreAuthorize("hasRole('CANDIDATE')")
    public ResponseEntity<ApiResponse<ApplicationDto>> applyForJob(
            @Valid @RequestBody ApplicationSubmitRequest request,
            Principal principal) {
        log.info("API request to apply for job from user: {}", principal.getName());
        ApplicationDto dto = recruitmentService.applyForJob(principal.getName(), request);
        return ResponseEntity.ok(ApiResponse.success("Job application submitted successfully", dto));
    }

    @GetMapping("/applications/my")
    @PreAuthorize("hasRole('CANDIDATE')")
    public ResponseEntity<ApiResponse<List<ApplicationDto>>> getMyApplications(Principal principal) {
        log.info("API request to fetch applications for user: {}", principal.getName());
        List<ApplicationDto> dtos = recruitmentService.getCandidateApplications(principal.getName());
        return ResponseEntity.ok(ApiResponse.success("Applications retrieved successfully", dtos));
    }

    @GetMapping("/applications")
    @PreAuthorize("hasRole('ADMIN') or hasRole('HR')")
    public ResponseEntity<ApiResponse<List<ApplicationDto>>> getAllApplications() {
        log.info("API request to list all job applications");
        List<ApplicationDto> dtos = recruitmentService.getAllApplications();
        return ResponseEntity.ok(ApiResponse.success("All applications retrieved successfully", dtos));
    }

    @PatchMapping("/applications/{id}/status")
    @PreAuthorize("hasRole('ADMIN') or hasRole('HR')")
    public ResponseEntity<ApiResponse<ApplicationDto>> changeApplicationStatus(
            @PathVariable Long id,
            @RequestParam String status) {
        log.info("API request to transition application ID: {} status to {}", id, status);
        ApplicationDto dto = recruitmentService.changeApplicationStatus(id, status);
        return ResponseEntity.ok(ApiResponse.success("Application status updated successfully", dto));
    }

    @PostMapping("/interviews")
    @PreAuthorize("hasRole('ADMIN') or hasRole('HR')")
    public ResponseEntity<ApiResponse<InterviewDto>> scheduleInterview(
            @Valid @RequestBody InterviewScheduleRequest request) {
        log.info("API request to schedule interview for application ID: {}", request.getApplicationId());
        InterviewDto dto = recruitmentService.scheduleInterview(request);
        return ResponseEntity.ok(ApiResponse.success("Interview scheduled successfully", dto));
    }

    @GetMapping("/interviews/my-schedule")
    @PreAuthorize("hasRole('ADMIN') or hasRole('HR') or hasRole('EMPLOYEE')")
    public ResponseEntity<ApiResponse<List<InterviewDto>>> getMyInterviews(Principal principal) {
        log.info("API request to get scheduled interviews for user: {}", principal.getName());
        List<InterviewDto> dtos = recruitmentService.getInterviewsForInterviewer(principal.getName());
        return ResponseEntity.ok(ApiResponse.success("Scheduled interviews retrieved successfully", dtos));
    }

    @PutMapping("/interviews/{id}/feedback")
    @PreAuthorize("hasRole('ADMIN') or hasRole('HR') or hasRole('EMPLOYEE')")
    public ResponseEntity<ApiResponse<InterviewDto>> submitInterviewFeedback(
            @PathVariable Long id,
            @Valid @RequestBody InterviewFeedbackRequest request,
            Principal principal) {
        log.info("API request to submit interview feedback for ID: {} by: {}", id, principal.getName());
        InterviewDto dto = recruitmentService.submitInterviewFeedback(id, request, principal.getName());
        return ResponseEntity.ok(ApiResponse.success("Interview feedback submitted successfully", dto));
    }
}

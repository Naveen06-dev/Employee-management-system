package com.ems.modules.recruitment.controller;

import com.ems.common.dto.ApiResponse;
import com.ems.modules.recruitment.dto.JobCreateRequest;
import com.ems.modules.recruitment.dto.JobDto;
import com.ems.modules.recruitment.service.JobService;
import jakarta.validation.Valid;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.security.Principal;

@Slf4j
@RestController
@RequestMapping("/api/v1/jobs")
public class JobController {

    private final JobService jobService;

    public JobController(JobService jobService) {
        this.jobService = jobService;
    }

    @GetMapping
    public ResponseEntity<ApiResponse<Page<JobDto>>> searchJobs(
            @RequestParam(required = false, defaultValue = "") String search,
            @RequestParam(required = false) Long departmentId,
            @RequestParam(required = false) String status,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(defaultValue = "createdAt") String sortBy,
            @RequestParam(defaultValue = "desc") String sortDir) {
        
        log.info("API request to search job postings");
        Page<JobDto> jobs = jobService.searchJobs(search, departmentId, status, page, size, sortBy, sortDir);
        return ResponseEntity.ok(ApiResponse.success("Jobs retrieved successfully", jobs));
    }

    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse<JobDto>> getJobById(@PathVariable Long id) {
        log.info("API request to fetch job posting details ID: {}", id);
        JobDto job = jobService.getJobById(id);
        return ResponseEntity.ok(ApiResponse.success("Job details retrieved successfully", job));
    }

    @PostMapping
    @PreAuthorize("hasRole('ADMIN') or hasRole('HR')")
    public ResponseEntity<ApiResponse<JobDto>> createJob(
            @Valid @RequestBody JobCreateRequest request,
            Principal principal) {
        log.info("API request to create job by user: {}", principal.getName());
        JobDto created = jobService.createJob(request, principal.getName());
        return ResponseEntity.ok(ApiResponse.success("Job posting created successfully", created));
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN') or hasRole('HR')")
    public ResponseEntity<ApiResponse<JobDto>> updateJob(
            @PathVariable Long id,
            @Valid @RequestBody JobCreateRequest request) {
        log.info("API request to update job posting ID: {}", id);
        JobDto updated = jobService.updateJob(id, request);
        return ResponseEntity.ok(ApiResponse.success("Job posting updated successfully", updated));
    }

    @PatchMapping("/{id}/status")
    @PreAuthorize("hasRole('ADMIN') or hasRole('HR')")
    public ResponseEntity<ApiResponse<JobDto>> changeJobStatus(
            @PathVariable Long id,
            @RequestParam String status) {
        log.info("API request to change status of job ID: {} to {}", id, status);
        JobDto updated = jobService.changeJobStatus(id, status);
        return ResponseEntity.ok(ApiResponse.success("Job status updated successfully", updated));
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ApiResponse<Void>> deleteJob(@PathVariable Long id) {
        log.info("API request to delete job posting ID: {}", id);
        jobService.deleteJob(id);
        return ResponseEntity.ok(ApiResponse.success("Job posting deleted successfully"));
    }
}

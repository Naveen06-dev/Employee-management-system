package com.ems.modules.recruitment.service;

import com.ems.modules.recruitment.dto.JobCreateRequest;
import com.ems.modules.recruitment.dto.JobDto;
import org.springframework.data.domain.Page;

public interface JobService {
    Page<JobDto> searchJobs(String search, Long departmentId, String status, int page, int size, String sortBy, String sortDir);
    JobDto getJobById(Long id);
    JobDto createJob(JobCreateRequest request, String username);
    JobDto updateJob(Long id, JobCreateRequest request);
    JobDto changeJobStatus(Long id, String status);
    void deleteJob(Long id);
}

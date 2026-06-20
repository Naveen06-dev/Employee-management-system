package com.ems.modules.recruitment.mapper;

import com.ems.modules.recruitment.dto.JobDto;
import com.ems.modules.recruitment.dto.JobCreateRequest;
import com.ems.modules.recruitment.model.Job;
import com.ems.modules.recruitment.model.JobStatus;

public class JobMapper {

    public static JobDto toDto(Job job) {
        if (job == null) {
            return null;
        }

        return JobDto.builder()
                .id(job.getId())
                .title(job.getTitle())
                .description(job.getDescription())
                .requirements(job.getRequirements())
                .location(job.getLocation())
                .salaryRange(job.getSalaryRange())
                .departmentId(job.getDepartment().getId())
                .departmentName(job.getDepartment().getName())
                .createdByUsername(job.getCreatedBy().getUsername())
                .status(job.getStatus().name())
                .createdAt(job.getCreatedAt())
                .build();
    }

    public static Job toEntity(JobCreateRequest request) {
        if (request == null) {
            return null;
        }

        return Job.builder()
                .title(request.getTitle())
                .description(request.getDescription())
                .requirements(request.getRequirements())
                .location(request.getLocation())
                .salaryRange(request.getSalaryRange())
                .status(JobStatus.OPEN)
                .build();
    }
}

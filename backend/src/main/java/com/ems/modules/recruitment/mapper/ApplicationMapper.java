package com.ems.modules.recruitment.mapper;

import com.ems.modules.recruitment.dto.ApplicationDto;
import com.ems.modules.recruitment.model.Application;

public class ApplicationMapper {

    public static ApplicationDto toDto(Application application) {
        if (application == null) {
            return null;
        }

        return ApplicationDto.builder()
                .id(application.getId())
                .jobId(application.getJob().getId())
                .jobTitle(application.getJob().getTitle())
                .candidateId(application.getCandidate().getId())
                .candidateUsername(application.getCandidate().getUsername())
                .candidateEmail(application.getCandidate().getEmail())
                .resumeUrl(application.getResumeUrl())
                .coverLetter(application.getCoverLetter())
                .status(application.getStatus().name())
                .createdAt(application.getCreatedAt())
                .build();
    }
}

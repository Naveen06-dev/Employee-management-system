package com.ems.modules.recruitment.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ApplicationDto {
    private Long id;
    private Long jobId;
    private String jobTitle;
    private UUID candidateId;
    private String candidateUsername;
    private String candidateEmail;
    private String resumeUrl;
    private String coverLetter;
    private String status;
    private LocalDateTime createdAt;
}

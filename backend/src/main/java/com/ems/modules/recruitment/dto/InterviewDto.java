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
public class InterviewDto {
    private Long id;
    private Long applicationId;
    private String jobTitle;
    private String candidateName;
    private UUID interviewerId;
    private String interviewerName;
    private LocalDateTime scheduledAt;
    private Integer durationMinutes;
    private String meetLink;
    private String status;
    private String feedback;
    private Integer rating;
}

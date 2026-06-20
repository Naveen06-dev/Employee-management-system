package com.ems.modules.recruitment.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class JobDto {
    private Long id;
    private String title;
    private String description;
    private String requirements;
    private String location;
    private String salaryRange;
    private Long departmentId;
    private String departmentName;
    private String createdByUsername;
    private String status;
    private LocalDateTime createdAt;
}

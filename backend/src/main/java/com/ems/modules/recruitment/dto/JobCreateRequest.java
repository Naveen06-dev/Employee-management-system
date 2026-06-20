package com.ems.modules.recruitment.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class JobCreateRequest {

    @NotBlank(message = "Job title is required")
    private String title;

    @NotBlank(message = "Job description is required")
    private String description;

    @NotBlank(message = "Requirements description is required")
    private String requirements;

    @NotBlank(message = "Location is required")
    private String location;

    @NotBlank(message = "Salary range is required")
    private String salaryRange;

    @NotNull(message = "Department ID is required")
    private Long departmentId;
}

package com.ems.modules.employee.dto;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.math.BigDecimal;

@Data
public class EmployeeUpdateRequest {

    @NotBlank(message = "First name is required")
    private String firstName;

    @NotBlank(message = "Last name is required")
    private String lastName;

    @NotBlank(message = "Job title is required")
    private String jobTitle;

    @NotNull(message = "Department ID is required")
    private Long departmentId;

    @NotNull(message = "Salary is required")
    @Min(value = 0, message = "Salary must be non-negative")
    private BigDecimal salary;

    @NotBlank(message = "Status is required")
    private String status; // ACTIVE, TERMINATED, LEAVE
}

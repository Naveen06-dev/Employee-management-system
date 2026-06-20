package com.ems.modules.employee.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.time.LocalDate;

@Data
public class LeaveCreateRequest {

    @NotNull(message = "Start date is required")
    private LocalDate startDate;

    @NotNull(message = "End date is required")
    private LocalDate endDate;

    @NotBlank(message = "Leave type is required")
    private String leaveType; // SICK, CASUAL, MATERNITY, PATERNITY, UNPAID

    @NotBlank(message = "Reason is required")
    private String reason;
}

package com.ems.modules.employee.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class EmployeeDto {
    private UUID id;
    private String employeeId;
    private String firstName;
    private String lastName;
    private String email;
    private String username;
    private String jobTitle;
    private Long departmentId;
    private String departmentName;
    private BigDecimal salary;
    private LocalDate hireDate;
    private String status;
}

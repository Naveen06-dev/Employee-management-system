package com.ems.modules.employee.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AttendanceDto {
    private Long id;
    private UUID employeeId;
    private String employeeName;
    private LocalDate workDate;
    private LocalTime checkIn;
    private LocalTime checkOut;
    private String status;
}

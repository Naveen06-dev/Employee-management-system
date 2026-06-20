package com.ems.modules.employee.service;

import com.ems.modules.employee.dto.AttendanceDto;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

public interface AttendanceService {
    AttendanceDto checkIn(String username);
    AttendanceDto checkOut(String username);
    List<AttendanceDto> getAttendanceReport(UUID employeeId, LocalDate startDate, LocalDate endDate);
    List<AttendanceDto> getDailyAttendance(LocalDate date);
}

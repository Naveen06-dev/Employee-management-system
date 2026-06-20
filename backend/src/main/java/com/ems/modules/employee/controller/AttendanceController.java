package com.ems.modules.employee.controller;

import com.ems.common.dto.ApiResponse;
import com.ems.modules.employee.dto.AttendanceDto;
import com.ems.modules.employee.service.AttendanceService;
import com.ems.modules.employee.service.EmployeeService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.security.Principal;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

@Slf4j
@RestController
@RequestMapping("/api/v1/attendance")
public class AttendanceController {

    private final AttendanceService attendanceService;
    private final EmployeeService employeeService;

    public AttendanceController(AttendanceService attendanceService, EmployeeService employeeService) {
        this.attendanceService = attendanceService;
        this.employeeService = employeeService;
    }

    @PostMapping("/check-in")
    public ResponseEntity<ApiResponse<AttendanceDto>> checkIn(Principal principal) {
        log.info("API request to check in for user: {}", principal.getName());
        AttendanceDto dto = attendanceService.checkIn(principal.getName());
        return ResponseEntity.ok(ApiResponse.success("Clock-in recorded successfully", dto));
    }

    @PostMapping("/check-out")
    public ResponseEntity<ApiResponse<AttendanceDto>> checkOut(Principal principal) {
        log.info("API request to check out for user: {}", principal.getName());
        AttendanceDto dto = attendanceService.checkOut(principal.getName());
        return ResponseEntity.ok(ApiResponse.success("Clock-out recorded successfully", dto));
    }

    @GetMapping("/report")
    @PreAuthorize("hasRole('ADMIN') or hasRole('HR') or hasRole('EMPLOYEE')")
    public ResponseEntity<ApiResponse<List<AttendanceDto>>> getAttendanceReport(
            @RequestParam UUID employeeId,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate,
            Principal principal) {
        
        log.info("API request for attendance report of employee ID: {} from {} to {}", employeeId, startDate, endDate);
        
        // Security check: Employee can only view their own report
        boolean isAdminOrHr = hasRole(principal, "ROLE_ADMIN") || hasRole(principal, "ROLE_HR");
        if (!isAdminOrHr) {
            String profileUsername = employeeService.getEmployeeById(employeeId).getUsername();
            if (!principal.getName().equals(profileUsername)) {
                throw new AccessDeniedException("You are not authorized to view attendance reports for other employees.");
            }
        }

        List<AttendanceDto> report = attendanceService.getAttendanceReport(employeeId, startDate, endDate);
        return ResponseEntity.ok(ApiResponse.success("Attendance report retrieved successfully", report));
    }

    @GetMapping("/daily")
    @PreAuthorize("hasRole('ADMIN') or hasRole('HR')")
    public ResponseEntity<ApiResponse<List<AttendanceDto>>> getDailyAttendance(
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date) {
        
        LocalDate targetDate = date != null ? date : LocalDate.now();
        log.info("API request for daily attendance of date: {}", targetDate);
        List<AttendanceDto> attendanceList = attendanceService.getDailyAttendance(targetDate);
        return ResponseEntity.ok(ApiResponse.success("Daily attendance records retrieved successfully", attendanceList));
    }

    private boolean hasRole(Principal principal, String role) {
        if (principal instanceof org.springframework.security.authentication.UsernamePasswordAuthenticationToken token) {
            return token.getAuthorities().stream()
                    .anyMatch(auth -> auth.getAuthority().equals(role));
        }
        return false;
    }
}

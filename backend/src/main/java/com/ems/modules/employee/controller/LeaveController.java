package com.ems.modules.employee.controller;

import com.ems.common.dto.ApiResponse;
import com.ems.modules.employee.dto.LeaveCreateRequest;
import com.ems.modules.employee.dto.LeaveDto;
import com.ems.modules.employee.service.LeaveService;
import jakarta.validation.Valid;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.security.Principal;
import java.util.List;

@Slf4j
@RestController
@RequestMapping("/api/v1/leaves")
public class LeaveController {

    private final LeaveService leaveService;

    public LeaveController(LeaveService leaveService) {
        this.leaveService = leaveService;
    }

    @PostMapping
    @PreAuthorize("hasRole('EMPLOYEE')")
    public ResponseEntity<ApiResponse<LeaveDto>> requestLeave(
            @Valid @RequestBody LeaveCreateRequest request,
            Principal principal) {
        log.info("API request to submit leave for user: {}", principal.getName());
        LeaveDto dto = leaveService.requestLeave(principal.getName(), request);
        return ResponseEntity.ok(ApiResponse.success("Leave request submitted successfully", dto));
    }

    @GetMapping("/my")
    @PreAuthorize("hasRole('EMPLOYEE')")
    public ResponseEntity<ApiResponse<List<LeaveDto>>> getMyLeaveHistory(Principal principal) {
        log.info("API request for leave history of user: {}", principal.getName());
        List<LeaveDto> history = leaveService.getEmployeeLeaveHistory(principal.getName());
        return ResponseEntity.ok(ApiResponse.success("Leave history retrieved successfully", history));
    }

    @GetMapping
    @PreAuthorize("hasRole('ADMIN') or hasRole('HR')")
    public ResponseEntity<ApiResponse<List<LeaveDto>>> getAllLeaveRequests(@RequestParam(required = false) String status) {
        log.info("API request to list leave requests with status: {}", status);
        List<LeaveDto> leaves = leaveService.getAllLeaveRequests(status);
        return ResponseEntity.ok(ApiResponse.success("Leave requests retrieved successfully", leaves));
    }

    @PatchMapping("/{id}/approve")
    @PreAuthorize("hasRole('ADMIN') or hasRole('HR')")
    public ResponseEntity<ApiResponse<LeaveDto>> reviewLeave(
            @PathVariable Long id,
            @RequestParam String status,
            Principal principal) {
        log.info("API request to review leave ID: {} with status: {}, by reviewer: {}", id, status, principal.getName());
        LeaveDto reviewed = leaveService.reviewLeave(id, status, principal.getName());
        return ResponseEntity.ok(ApiResponse.success("Leave request review submitted successfully", reviewed));
    }
}

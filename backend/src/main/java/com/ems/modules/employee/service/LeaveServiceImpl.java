package com.ems.modules.employee.service;

import com.ems.exception.BadRequestException;
import com.ems.exception.ResourceNotFoundException;
import com.ems.modules.employee.dto.LeaveCreateRequest;
import com.ems.modules.employee.dto.LeaveDto;
import com.ems.modules.employee.mapper.LeaveMapper;
import com.ems.modules.employee.model.*;
import com.ems.modules.employee.repository.EmployeeRepository;
import com.ems.modules.employee.repository.LeaveRepository;
import com.ems.common.event.LeaveRequestedEvent;
import com.ems.common.event.LeaveReviewedEvent;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.util.List;
import java.util.stream.Collectors;

@Slf4j
@Service
public class LeaveServiceImpl implements LeaveService {

    private final LeaveRepository leaveRepository;
    private final EmployeeRepository employeeRepository;
    private final ApplicationEventPublisher eventPublisher;

    public LeaveServiceImpl(
            LeaveRepository leaveRepository,
            EmployeeRepository employeeRepository,
            ApplicationEventPublisher eventPublisher) {
        this.leaveRepository = leaveRepository;
        this.employeeRepository = employeeRepository;
        this.eventPublisher = eventPublisher;
    }

    @Override
    @Transactional
    public LeaveDto requestLeave(String username, LeaveCreateRequest request) {
        log.info("Requesting leave for user: {}", username);
        Employee employee = employeeRepository.findByUserUsername(username)
                .orElseThrow(() -> new ResourceNotFoundException("Employee profile not found for user: " + username));

        if (request.getStartDate().isAfter(request.getEndDate())) {
            throw new BadRequestException("Start date cannot be after end date.");
        }

        LeaveType type;
        try {
            type = LeaveType.valueOf(request.getLeaveType().toUpperCase());
        } catch (IllegalArgumentException e) {
            throw new BadRequestException("Invalid leave type. Must be SICK, CASUAL, MATERNITY, PATERNITY, or UNPAID.");
        }

        Leave leave = Leave.builder()
                .employee(employee)
                .startDate(request.getStartDate())
                .endDate(request.getEndDate())
                .leaveType(type)
                .reason(request.getReason())
                .status(LeaveStatus.PENDING)
                .build();

        leaveRepository.save(leave);
        log.info("Leave request created successfully for employee: {}", employee.getEmployeeId());

        // Publish event for notifications
        eventPublisher.publishEvent(new LeaveRequestedEvent(leave));

        return LeaveMapper.toDto(leave);
    }

    @Override
    @Transactional(readOnly = true)
    public List<LeaveDto> getEmployeeLeaveHistory(String username) {
        log.info("Fetching leave history for user: {}", username);
        Employee employee = employeeRepository.findByUserUsername(username)
                .orElseThrow(() -> new ResourceNotFoundException("Employee profile not found for user: " + username));

        return leaveRepository.findByEmployeeId(employee.getId()).stream()
                .map(LeaveMapper::toDto)
                .collect(Collectors.toList());
    }

    @Override
    @Transactional(readOnly = true)
    public List<LeaveDto> getAllLeaveRequests(String status) {
        log.info("Fetching all leave requests with status filter: {}", status);
        List<Leave> leaves;
        if (StringUtils.hasText(status)) {
            try {
                LeaveStatus leaveStatus = LeaveStatus.valueOf(status.toUpperCase());
                leaves = leaveRepository.findByStatus(leaveStatus);
            } catch (IllegalArgumentException e) {
                log.warn("Invalid leave status filter passed: {}, returning all", status);
                leaves = leaveRepository.findAll();
            }
        } else {
            leaves = leaveRepository.findAll();
        }

        return leaves.stream()
                .map(LeaveMapper::toDto)
                .collect(Collectors.toList());
    }

    @Override
    @Transactional
    public LeaveDto reviewLeave(Long leaveId, String statusStr, String reviewerUsername) {
        log.info("Reviewing leave ID: {}, status: {}, by reviewer: {}", leaveId, statusStr, reviewerUsername);
        Leave leave = leaveRepository.findById(leaveId)
                .orElseThrow(() -> new ResourceNotFoundException("Leave request not found with ID: " + leaveId));

        if (leave.getStatus() != LeaveStatus.PENDING) {
            throw new BadRequestException("Leave request has already been reviewed. Current status: " + leave.getStatus());
        }

        LeaveStatus reviewStatus;
        try {
            reviewStatus = LeaveStatus.valueOf(statusStr.toUpperCase());
            if (reviewStatus == LeaveStatus.PENDING) {
                throw new IllegalArgumentException();
            }
        } catch (IllegalArgumentException e) {
            throw new BadRequestException("Invalid review status. Status must be APPROVED or REJECTED.");
        }

        Employee reviewer = employeeRepository.findByUserUsername(reviewerUsername)
                .orElseThrow(() -> new ResourceNotFoundException("Reviewer employee profile not found: " + reviewerUsername));

        leave.setStatus(reviewStatus);
        leave.setApprovedBy(reviewer);
        leaveRepository.save(leave);

        log.info("Leave request ID: {} reviewed successfully. Status: {}", leaveId, reviewStatus);

        // Publish event for notifications
        eventPublisher.publishEvent(new LeaveReviewedEvent(leave));

        return LeaveMapper.toDto(leave);
    }
}

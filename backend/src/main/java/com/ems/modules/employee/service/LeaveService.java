package com.ems.modules.employee.service;

import com.ems.modules.employee.dto.LeaveCreateRequest;
import com.ems.modules.employee.dto.LeaveDto;

import java.util.List;

public interface LeaveService {
    LeaveDto requestLeave(String username, LeaveCreateRequest request);
    List<LeaveDto> getEmployeeLeaveHistory(String username);
    List<LeaveDto> getAllLeaveRequests(String status);
    LeaveDto reviewLeave(Long leaveId, String status, String reviewerUsername);
}

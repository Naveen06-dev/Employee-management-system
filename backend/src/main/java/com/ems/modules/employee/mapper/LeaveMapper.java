package com.ems.modules.employee.mapper;

import com.ems.modules.employee.dto.LeaveDto;
import com.ems.modules.employee.model.Leave;

public class LeaveMapper {

    public static LeaveDto toDto(Leave leave) {
        if (leave == null) {
            return null;
        }

        LeaveDto.LeaveDtoBuilder builder = LeaveDto.builder()
                .id(leave.getId())
                .employeeId(leave.getEmployee().getId())
                .employeeName(leave.getEmployee().getFirstName() + " " + leave.getEmployee().getLastName())
                .startDate(leave.getStartDate())
                .endDate(leave.getEndDate())
                .leaveType(leave.getLeaveType().name())
                .status(leave.getStatus().name())
                .reason(leave.getReason());

        if (leave.getApprovedBy() != null) {
            builder.approvedByName(leave.getApprovedBy().getFirstName() + " " + leave.getApprovedBy().getLastName());
        }

        return builder.build();
    }
}

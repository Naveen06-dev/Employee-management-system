package com.ems.modules.employee.mapper;

import com.ems.modules.employee.dto.AttendanceDto;
import com.ems.modules.employee.model.Attendance;

public class AttendanceMapper {

    public static AttendanceDto toDto(Attendance attendance) {
        if (attendance == null) {
            return null;
        }

        return AttendanceDto.builder()
                .id(attendance.getId())
                .employeeId(attendance.getEmployee().getId())
                .employeeName(attendance.getEmployee().getFirstName() + " " + attendance.getEmployee().getLastName())
                .workDate(attendance.getWorkDate())
                .checkIn(attendance.getCheckIn())
                .checkOut(attendance.getCheckOut())
                .status(attendance.getStatus().name())
                .build();
    }
}

package com.ems.modules.employee.service;

import com.ems.exception.BadRequestException;
import com.ems.exception.ResourceNotFoundException;
import com.ems.modules.employee.dto.AttendanceDto;
import com.ems.modules.employee.mapper.AttendanceMapper;
import com.ems.modules.employee.model.Attendance;
import com.ems.modules.employee.model.AttendanceStatus;
import com.ems.modules.employee.model.Employee;
import com.ems.modules.employee.repository.AttendanceRepository;
import com.ems.modules.employee.repository.EmployeeRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Slf4j
@Service
public class AttendanceServiceImpl implements AttendanceService {

    private final AttendanceRepository attendanceRepository;
    private final EmployeeRepository employeeRepository;

    private static final LocalTime START_TIME_LIMIT = LocalTime.of(9, 15); // Lateness limit 09:15 AM

    public AttendanceServiceImpl(AttendanceRepository attendanceRepository, EmployeeRepository employeeRepository) {
        this.attendanceRepository = attendanceRepository;
        this.employeeRepository = employeeRepository;
    }

    @Override
    @Transactional
    public AttendanceDto checkIn(String username) {
        log.info("Processing check-in request for username: {}", username);
        Employee employee = employeeRepository.findByUserUsername(username)
                .orElseThrow(() -> new ResourceNotFoundException("Employee profile not found for user: " + username));

        LocalDate today = LocalDate.now();
        if (attendanceRepository.findByEmployeeAndWorkDate(employee, today).isPresent()) {
            throw new BadRequestException("You have already checked in today.");
        }

        LocalTime now = LocalTime.now();
        AttendanceStatus status = now.isAfter(START_TIME_LIMIT) ? AttendanceStatus.LATE : AttendanceStatus.PRESENT;

        Attendance attendance = Attendance.builder()
                .employee(employee)
                .workDate(today)
                .checkIn(now)
                .status(status)
                .build();

        attendanceRepository.save(attendance);
        log.info("Check-in successful for employee: {}, status: {}", employee.getEmployeeId(), status);
        return AttendanceMapper.toDto(attendance);
    }

    @Override
    @Transactional
    public AttendanceDto checkOut(String username) {
        log.info("Processing check-out request for username: {}", username);
        Employee employee = employeeRepository.findByUserUsername(username)
                .orElseThrow(() -> new ResourceNotFoundException("Employee profile not found for user: " + username));

        LocalDate today = LocalDate.now();
        Attendance attendance = attendanceRepository.findByEmployeeAndWorkDate(employee, today)
                .orElseThrow(() -> new BadRequestException("You cannot check out without checking in first."));

        if (attendance.getCheckOut() != null) {
            throw new BadRequestException("You have already checked out today.");
        }

        attendance.setCheckOut(LocalTime.now());
        attendanceRepository.save(attendance);
        log.info("Check-out successful for employee: {}", employee.getEmployeeId());
        return AttendanceMapper.toDto(attendance);
    }

    @Override
    @Transactional(readOnly = true)
    public List<AttendanceDto> getAttendanceReport(UUID employeeId, LocalDate startDate, LocalDate endDate) {
        log.info("Fetching attendance report for employee ID: {} between {} and {}", employeeId, startDate, endDate);
        return attendanceRepository.findByEmployeeIdAndWorkDateBetween(employeeId, startDate, endDate).stream()
                .map(AttendanceMapper::toDto)
                .collect(Collectors.toList());
    }

    @Override
    @Transactional(readOnly = true)
    public List<AttendanceDto> getDailyAttendance(LocalDate date) {
        log.info("Fetching daily attendance for date: {}", date);
        return attendanceRepository.findByWorkDate(date).stream()
                .map(AttendanceMapper::toDto)
                .collect(Collectors.toList());
    }
}

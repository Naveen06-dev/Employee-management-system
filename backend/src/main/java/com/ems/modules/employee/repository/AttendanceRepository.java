package com.ems.modules.employee.repository;

import com.ems.modules.employee.model.Attendance;
import com.ems.modules.employee.model.AttendanceStatus;
import com.ems.modules.employee.model.Employee;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface AttendanceRepository extends JpaRepository<Attendance, Long> {
    Optional<Attendance> findByEmployeeAndWorkDate(Employee employee, LocalDate workDate);
    List<Attendance> findByEmployeeIdAndWorkDateBetween(UUID employeeId, LocalDate startDate, LocalDate endDate);
    List<Attendance> findByWorkDate(LocalDate workDate);
    long countByWorkDateAndStatus(LocalDate workDate, AttendanceStatus status);
}

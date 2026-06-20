package com.ems.modules.employee.service;

import com.ems.modules.employee.dto.DashboardStatsDto;
import com.ems.modules.employee.model.AttendanceStatus;
import com.ems.modules.employee.model.EmployeeStatus;
import com.ems.modules.employee.model.LeaveStatus;
import com.ems.modules.employee.repository.AttendanceRepository;
import com.ems.modules.employee.repository.DepartmentRepository;
import com.ems.modules.employee.repository.EmployeeRepository;
import com.ems.modules.employee.repository.LeaveRepository;
import com.ems.modules.recruitment.model.ApplicationStatus;
import com.ems.modules.recruitment.model.JobStatus;
import com.ems.modules.recruitment.repository.ApplicationRepository;
import com.ems.modules.recruitment.repository.JobRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.HashMap;
import java.util.Map;

@Slf4j
@Service
public class DashboardServiceImpl implements DashboardService {

    private final EmployeeRepository employeeRepository;
    private final DepartmentRepository departmentRepository;
    private final JobRepository jobRepository;
    private final ApplicationRepository applicationRepository;
    private final LeaveRepository leaveRepository;
    private final AttendanceRepository attendanceRepository;

    public DashboardServiceImpl(
            EmployeeRepository employeeRepository,
            DepartmentRepository departmentRepository,
            JobRepository jobRepository,
            ApplicationRepository applicationRepository,
            LeaveRepository leaveRepository,
            AttendanceRepository attendanceRepository) {
        this.employeeRepository = employeeRepository;
        this.departmentRepository = departmentRepository;
        this.jobRepository = jobRepository;
        this.applicationRepository = applicationRepository;
        this.leaveRepository = leaveRepository;
        this.attendanceRepository = attendanceRepository;
    }

    @Override
    @Transactional(readOnly = true)
    @Cacheable(value = "dashboardStats", key = "'summary'")
    public DashboardStatsDto getDashboardStatistics() {
        log.info("Calculating dashboard metrics (cache miss)");

        LocalDate today = LocalDate.now();
        long totalEmp = employeeRepository.count();
        long activeEmp = employeeRepository.countByStatus(EmployeeStatus.ACTIVE);
        long leaveEmp = employeeRepository.countByStatus(EmployeeStatus.LEAVE);
        long totalDept = departmentRepository.count();

        long openJobs = jobRepository.countByStatus(JobStatus.OPEN);
        long totalApps = applicationRepository.count();

        // Application Status Distribution
        Map<String, Long> appDistribution = new HashMap<>();
        for (ApplicationStatus status : ApplicationStatus.values()) {
            appDistribution.put(status.name(), applicationRepository.countByStatus(status));
        }

        long pendingLeaves = leaveRepository.countByStatus(LeaveStatus.PENDING);

        long presentToday = attendanceRepository.countByWorkDateAndStatus(today, AttendanceStatus.PRESENT);
        long lateToday = attendanceRepository.countByWorkDateAndStatus(today, AttendanceStatus.LATE);
        long absentToday = attendanceRepository.countByWorkDateAndStatus(today, AttendanceStatus.ABSENT);

        long checkedInToday = presentToday + lateToday;
        double attendanceRate = totalEmp == 0 ? 0.0 : ((double) checkedInToday / totalEmp) * 100.0;

        return DashboardStatsDto.builder()
                .totalEmployees(totalEmp)
                .activeEmployees(activeEmp)
                .leaveEmployees(leaveEmp)
                .totalDepartments(totalDept)
                .openJobs(openJobs)
                .totalApplications(totalApps)
                .applicationsStatusDistribution(appDistribution)
                .pendingLeaves(pendingLeaves)
                .presentToday(presentToday)
                .lateToday(lateToday)
                .absentToday(absentToday)
                .attendanceRateToday(Math.round(attendanceRate * 100.0) / 100.0) // round to 2 decimal places
                .build();
    }
}

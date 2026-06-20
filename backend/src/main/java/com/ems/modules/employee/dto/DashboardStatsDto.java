package com.ems.modules.employee.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.Map;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class DashboardStatsDto {
    private long totalEmployees;
    private long activeEmployees;
    private long leaveEmployees;
    private long totalDepartments;
    private long openJobs;
    private long totalApplications;
    private Map<String, Long> applicationsStatusDistribution;
    private long pendingLeaves;
    private long presentToday;
    private long lateToday;
    private long absentToday;
    private double attendanceRateToday;
}

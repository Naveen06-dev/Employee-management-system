package com.ems.modules.employee.repository;

import com.ems.modules.employee.model.Leave;
import com.ems.modules.employee.model.LeaveStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface LeaveRepository extends JpaRepository<Leave, Long> {
    List<Leave> findByEmployeeId(UUID employeeId);
    List<Leave> findByStatus(LeaveStatus status);
    long countByStatus(LeaveStatus status);
}

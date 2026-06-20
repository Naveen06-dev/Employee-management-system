package com.ems.modules.employee.repository;

import com.ems.modules.auth.model.User;
import com.ems.modules.employee.model.Employee;
import com.ems.modules.employee.model.EmployeeStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

@Repository
public interface EmployeeRepository extends JpaRepository<Employee, UUID>, JpaSpecificationExecutor<Employee> {
    Optional<Employee> findByEmployeeId(String employeeId);
    Optional<Employee> findByUser(User user);
    Optional<Employee> findByUserUsername(String username);
    boolean existsByEmployeeId(String employeeId);
    
    long countByStatus(EmployeeStatus status);
    long countByDepartmentId(Long departmentId);
}

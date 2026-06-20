package com.ems.modules.employee.service;

import com.ems.modules.employee.dto.EmployeeCreateRequest;
import com.ems.modules.employee.dto.EmployeeDto;
import com.ems.modules.employee.dto.EmployeeUpdateRequest;
import org.springframework.data.domain.Page;

import java.util.UUID;

public interface EmployeeService {
    Page<EmployeeDto> searchEmployees(String search, Long departmentId, String status, int page, int size, String sortBy, String sortDir);
    EmployeeDto getEmployeeById(UUID id);
    EmployeeDto getEmployeeByUsername(String username);
    EmployeeDto createEmployee(EmployeeCreateRequest request);
    EmployeeDto updateEmployee(UUID id, EmployeeUpdateRequest request);
    void deleteEmployee(UUID id);
}

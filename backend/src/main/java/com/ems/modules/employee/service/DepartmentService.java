package com.ems.modules.employee.service;

import com.ems.modules.employee.dto.DepartmentDto;

import java.util.List;
import java.util.UUID;

public interface DepartmentService {
    List<DepartmentDto> getAllDepartments();
    DepartmentDto getDepartmentById(Long id);
    DepartmentDto createDepartment(DepartmentDto dto);
    DepartmentDto updateDepartment(Long id, DepartmentDto dto);
    void deleteDepartment(Long id);
    DepartmentDto assignManager(Long departmentId, UUID employeeId);
}

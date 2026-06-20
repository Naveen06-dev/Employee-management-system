package com.ems.modules.employee.service;

import com.ems.exception.BadRequestException;
import com.ems.exception.DuplicateResourceException;
import com.ems.exception.ResourceNotFoundException;
import com.ems.modules.employee.dto.DepartmentDto;
import com.ems.modules.employee.mapper.DepartmentMapper;
import com.ems.modules.employee.model.Department;
import com.ems.modules.employee.model.Employee;
import com.ems.modules.employee.repository.DepartmentRepository;
import com.ems.modules.employee.repository.EmployeeRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Slf4j
@Service
public class DepartmentServiceImpl implements DepartmentService {

    private final DepartmentRepository departmentRepository;
    private final EmployeeRepository employeeRepository;

    public DepartmentServiceImpl(DepartmentRepository departmentRepository, EmployeeRepository employeeRepository) {
        this.departmentRepository = departmentRepository;
        this.employeeRepository = employeeRepository;
    }

    @Override
    @Transactional(readOnly = true)
    @Cacheable(value = "departments", key = "'all'")
    public List<DepartmentDto> getAllDepartments() {
        log.info("Fetching all departments from database (cache miss)");
        return departmentRepository.findAll().stream()
                .map(DepartmentMapper::toDto)
                .collect(Collectors.toList());
    }

    @Override
    @Transactional(readOnly = true)
    public DepartmentDto getDepartmentById(Long id) {
        log.info("Fetching department by ID: {}", id);
        Department department = departmentRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Department not found with ID: " + id));
        return DepartmentMapper.toDto(department);
    }

    @Override
    @Transactional
    @CacheEvict(value = "departments", allEntries = true)
    public DepartmentDto createDepartment(DepartmentDto dto) {
        log.info("Creating new department: {}", dto.getName());
        if (departmentRepository.existsByCode(dto.getCode())) {
            throw new DuplicateResourceException("Department with code " + dto.getCode() + " already exists");
        }

        Department department = DepartmentMapper.toEntity(dto);
        departmentRepository.save(department);
        return DepartmentMapper.toDto(department);
    }

    @Override
    @Transactional
    @CacheEvict(value = "departments", allEntries = true)
    public DepartmentDto updateDepartment(Long id, DepartmentDto dto) {
        log.info("Updating department ID: {}", id);
        Department department = departmentRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Department not found with ID: " + id));

        if (!department.getCode().equals(dto.getCode()) && departmentRepository.existsByCode(dto.getCode())) {
            throw new DuplicateResourceException("Department with code " + dto.getCode() + " already exists");
        }

        department.setName(dto.getName());
        department.setCode(dto.getCode());
        departmentRepository.save(department);

        return DepartmentMapper.toDto(department);
    }

    @Override
    @Transactional
    @CacheEvict(value = "departments", allEntries = true)
    public void deleteDepartment(Long id) {
        log.info("Deleting department ID: {}", id);
        Department department = departmentRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Department not found with ID: " + id));
        
        // Check if there are employees assigned to this department
        long employeeCount = employeeRepository.countByDepartmentId(id);
        if (employeeCount > 0) {
            throw new BadRequestException("Cannot delete department because it contains " + employeeCount + " employee(s). Reassign them first.");
        }

        departmentRepository.delete(department);
    }

    @Override
    @Transactional
    @CacheEvict(value = "departments", allEntries = true)
    public DepartmentDto assignManager(Long departmentId, UUID employeeId) {
        log.info("Assigning employee ID: {} as manager for department ID: {}", employeeId, departmentId);
        Department department = departmentRepository.findById(departmentId)
                .orElseThrow(() -> new ResourceNotFoundException("Department not found with ID: " + departmentId));

        Employee employee = employeeRepository.findById(employeeId)
                .orElseThrow(() -> new ResourceNotFoundException("Employee not found with ID: " + employeeId));

        department.setManager(employee);
        departmentRepository.save(department);
        
        return DepartmentMapper.toDto(department);
    }
}

package com.ems.modules.employee.service;

import com.ems.modules.audit.annotation.Auditable;
import com.ems.exception.BadRequestException;
import com.ems.exception.ResourceNotFoundException;
import com.ems.modules.auth.model.User;
import com.ems.modules.auth.repository.UserRepository;
import com.ems.modules.employee.dto.EmployeeCreateRequest;
import com.ems.modules.employee.dto.EmployeeDto;
import com.ems.modules.employee.dto.EmployeeUpdateRequest;
import com.ems.modules.employee.mapper.EmployeeMapper;
import com.ems.modules.employee.model.Department;
import com.ems.modules.employee.model.Employee;
import com.ems.modules.employee.model.EmployeeStatus;
import com.ems.modules.employee.repository.DepartmentRepository;
import com.ems.modules.employee.repository.EmployeeRepository;
import jakarta.persistence.criteria.Predicate;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.*;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.security.SecureRandom;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Slf4j
@Service
public class EmployeeServiceImpl implements EmployeeService {

    private final EmployeeRepository employeeRepository;
    private final DepartmentRepository departmentRepository;
    private final UserRepository userRepository;
    private final SecureRandom random = new SecureRandom();

    public EmployeeServiceImpl(
            EmployeeRepository employeeRepository,
            DepartmentRepository departmentRepository,
            UserRepository userRepository) {
        this.employeeRepository = employeeRepository;
        this.departmentRepository = departmentRepository;
        this.userRepository = userRepository;
    }

    @Override
    @Transactional(readOnly = true)
    public Page<EmployeeDto> searchEmployees(
            String search, Long departmentId, String status,
            int page, int size, String sortBy, String sortDir) {
        
        log.info("Searching employees with query: search='{}', departmentId={}, status={}", search, departmentId, status);
        
        Sort sort = sortDir.equalsIgnoreCase(Sort.Direction.ASC.name()) ? 
                Sort.by(sortBy).ascending() : Sort.by(sortBy).descending();
        Pageable pageable = PageRequest.of(page, size, sort);

        // Build Dynamic Query Specifications
        Specification<Employee> spec = (root, query, criteriaBuilder) -> {
            List<Predicate> predicates = new ArrayList<>();

            // 1. Text Search across name, jobTitle, employeeId
            if (StringUtils.hasText(search)) {
                String searchPattern = "%" + search.toLowerCase() + "%";
                Predicate searchPredicate = criteriaBuilder.or(
                        criteriaBuilder.like(criteriaBuilder.lower(root.get("firstName")), searchPattern),
                        criteriaBuilder.like(criteriaBuilder.lower(root.get("lastName")), searchPattern),
                        criteriaBuilder.like(criteriaBuilder.lower(root.get("jobTitle")), searchPattern),
                        criteriaBuilder.like(criteriaBuilder.lower(root.get("employeeId")), searchPattern)
                );
                predicates.add(searchPredicate);
            }

            // 2. Filter by Department
            if (departmentId != null) {
                predicates.add(criteriaBuilder.equal(root.get("department").get("id"), departmentId));
            }

            // 3. Filter by Status
            if (StringUtils.hasText(status)) {
                try {
                    EmployeeStatus employeeStatus = EmployeeStatus.valueOf(status.toUpperCase());
                    predicates.add(criteriaBuilder.equal(root.get("status"), employeeStatus));
                } catch (IllegalArgumentException e) {
                    log.warn("Invalid employee status filter passed: {}", status);
                }
            }

            return criteriaBuilder.and(predicates.toArray(new Predicate[0]));
        };

        Page<Employee> employeePage = employeeRepository.findAll(spec, pageable);
        List<EmployeeDto> employeeDtos = employeePage.getContent().stream()
                .map(EmployeeMapper::toDto)
                .toList();

        return new PageImpl<>(employeeDtos, pageable, employeePage.getTotalElements());
    }

    @Override
    @Transactional(readOnly = true)
    public EmployeeDto getEmployeeById(UUID id) {
        log.info("Fetching employee by ID: {}", id);
        Employee employee = employeeRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Employee not found with ID: " + id));
        return EmployeeMapper.toDto(employee);
    }

    @Override
    @Transactional(readOnly = true)
    public EmployeeDto getEmployeeByUsername(String username) {
        log.info("Fetching employee by username: {}", username);
        Employee employee = employeeRepository.findByUserUsername(username)
                .orElseThrow(() -> new ResourceNotFoundException("Employee not found for username: " + username));
        return EmployeeMapper.toDto(employee);
    }

    @Override
    @Transactional
    @Auditable(action = "CREATE_EMPLOYEE", entity = "Employee")
    public EmployeeDto createEmployee(EmployeeCreateRequest request) {
        log.info("Creating employee profile: {} {}", request.getFirstName(), request.getLastName());

        Department department = departmentRepository.findById(request.getDepartmentId())
                .orElseThrow(() -> new ResourceNotFoundException("Department not found with ID: " + request.getDepartmentId()));

        Employee employee = EmployeeMapper.toEntity(request);
        employee.setDepartment(department);
        employee.setEmployeeId(generateUniqueEmployeeId());

        // Associate user account if provided
        if (request.getUserId() != null) {
            User user = userRepository.findById(request.getUserId())
                    .orElseThrow(() -> new ResourceNotFoundException("User not found with ID: " + request.getUserId()));
            
            // Check if user already linked to an employee
            if (employeeRepository.findByUser(user).isPresent()) {
                throw new BadRequestException("User ID " + request.getUserId() + " is already linked to another employee");
            }
            employee.setUser(user);
        }

        employeeRepository.save(employee);
        log.info("Employee created successfully with ID: {}", employee.getEmployeeId());
        return EmployeeMapper.toDto(employee);
    }

    @Override
    @Transactional
    @Auditable(action = "UPDATE_EMPLOYEE", entity = "Employee")
    public EmployeeDto updateEmployee(UUID id, EmployeeUpdateRequest request) {
        log.info("Updating employee ID: {}", id);
        Employee employee = employeeRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Employee not found with ID: " + id));

        Department department = departmentRepository.findById(request.getDepartmentId())
                .orElseThrow(() -> new ResourceNotFoundException("Department not found with ID: " + request.getDepartmentId()));

        EmployeeStatus status;
        try {
            status = EmployeeStatus.valueOf(request.getStatus().toUpperCase());
        } catch (IllegalArgumentException e) {
            throw new BadRequestException("Invalid status: Status must be ACTIVE, TERMINATED, or LEAVE.");
        }

        employee.setFirstName(request.getFirstName());
        employee.setLastName(request.getLastName());
        employee.setJobTitle(request.getJobTitle());
        employee.setDepartment(department);
        employee.setSalary(request.getSalary());
        employee.setStatus(status);

        employeeRepository.save(employee);
        log.info("Employee ID: {} updated successfully", id);
        return EmployeeMapper.toDto(employee);
    }

    @Override
    @Transactional
    @Auditable(action = "DELETE_EMPLOYEE", entity = "Employee")
    public void deleteEmployee(UUID id) {
        log.info("Deleting employee profile ID: {}", id);
        Employee employee = employeeRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Employee not found with ID: " + id));
        
        // Unlink department manager links to prevent foreign key errors
        departmentRepository.findAll().forEach(dept -> {
            if (dept.getManager() != null && dept.getManager().getId().equals(id)) {
                dept.setManager(null);
                departmentRepository.save(dept);
            }
        });

        employeeRepository.delete(employee);
        log.info("Employee ID: {} deleted successfully", id);
    }

    private String generateUniqueEmployeeId() {
        String employeeId;
        boolean exists = true;
        do {
            int code = 1000 + random.nextInt(9000); // 4-digit code: 1000 to 9999
            employeeId = "EMP-" + code;
            exists = employeeRepository.existsByEmployeeId(employeeId);
        } while (exists);
        return employeeId;
    }
}

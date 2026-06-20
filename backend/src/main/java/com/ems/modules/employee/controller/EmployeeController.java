package com.ems.modules.employee.controller;

import com.ems.common.dto.ApiResponse;
import com.ems.modules.employee.dto.EmployeeCreateRequest;
import com.ems.modules.employee.dto.EmployeeDto;
import com.ems.modules.employee.dto.EmployeeUpdateRequest;
import com.ems.modules.employee.service.EmployeeService;
import jakarta.validation.Valid;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.security.Principal;
import java.util.UUID;

@Slf4j
@RestController
@RequestMapping("/api/v1/employees")
public class EmployeeController {

    private final EmployeeService employeeService;

    public EmployeeController(EmployeeService employeeService) {
        this.employeeService = employeeService;
    }

    @GetMapping
    @PreAuthorize("hasRole('ADMIN') or hasRole('HR') or hasRole('EMPLOYEE')")
    public ResponseEntity<ApiResponse<Page<EmployeeDto>>> searchEmployees(
            @RequestParam(required = false, defaultValue = "") String search,
            @RequestParam(required = false) Long departmentId,
            @RequestParam(required = false) String status,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(defaultValue = "createdAt") String sortBy,
            @RequestParam(defaultValue = "desc") String sortDir) {
        
        log.info("API request to search employees");
        Page<EmployeeDto> employees = employeeService.searchEmployees(search, departmentId, status, page, size, sortBy, sortDir);
        return ResponseEntity.ok(ApiResponse.success("Employees retrieved successfully", employees));
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN') or hasRole('HR') or hasRole('EMPLOYEE')")
    public ResponseEntity<ApiResponse<EmployeeDto>> getEmployeeById(@PathVariable UUID id, Principal principal) {
        log.info("API request to fetch employee ID: {}", id);
        EmployeeDto employee = employeeService.getEmployeeById(id);
        
        // Security constraint: Employees can only view their own profile details
        boolean isAdminOrHr = principal.getName().equals(employee.getUsername()) || 
                hasRole(principal, "ROLE_ADMIN") || 
                hasRole(principal, "ROLE_HR");
        
        if (!isAdminOrHr) {
            throw new AccessDeniedException("You are not authorized to view this employee profile.");
        }

        return ResponseEntity.ok(ApiResponse.success("Employee profile retrieved successfully", employee));
    }

    @GetMapping("/profile")
    public ResponseEntity<ApiResponse<EmployeeDto>> getCurrentEmployeeProfile(Principal principal) {
        log.info("API request to fetch current logged-in employee profile for user: {}", principal.getName());
        EmployeeDto employee = employeeService.getEmployeeByUsername(principal.getName());
        return ResponseEntity.ok(ApiResponse.success("Employee profile retrieved successfully", employee));
    }

    @PostMapping
    @PreAuthorize("hasRole('ADMIN') or hasRole('HR')")
    public ResponseEntity<ApiResponse<EmployeeDto>> createEmployee(@Valid @RequestBody EmployeeCreateRequest request) {
        log.info("API request to create employee: {} {}", request.getFirstName(), request.getLastName());
        EmployeeDto created = employeeService.createEmployee(request);
        return ResponseEntity.ok(ApiResponse.success("Employee created successfully", created));
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN') or hasRole('HR')")
    public ResponseEntity<ApiResponse<EmployeeDto>> updateEmployee(
            @PathVariable UUID id,
            @Valid @RequestBody EmployeeUpdateRequest request) {
        log.info("API request to update employee ID: {}", id);
        EmployeeDto updated = employeeService.updateEmployee(id, request);
        return ResponseEntity.ok(ApiResponse.success("Employee updated successfully", updated));
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ApiResponse<Void>> deleteEmployee(@PathVariable UUID id) {
        log.info("API request to delete employee ID: {}", id);
        employeeService.deleteEmployee(id);
        return ResponseEntity.ok(ApiResponse.success("Employee profile deleted successfully"));
    }

    private boolean hasRole(Principal principal, String role) {
        if (principal instanceof org.springframework.security.authentication.UsernamePasswordAuthenticationToken token) {
            return token.getAuthorities().stream()
                    .anyMatch(auth -> auth.getAuthority().equals(role));
        }
        return false;
    }
}

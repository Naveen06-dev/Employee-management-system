package com.ems.modules.employee.service;

import com.ems.exception.ResourceNotFoundException;
import com.ems.modules.employee.dto.EmployeeCreateRequest;
import com.ems.modules.employee.dto.EmployeeDto;
import com.ems.modules.employee.model.Department;
import com.ems.modules.employee.model.Employee;
import com.ems.modules.employee.repository.DepartmentRepository;
import com.ems.modules.employee.repository.EmployeeRepository;
import com.ems.modules.auth.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
public class EmployeeServiceTests {

    @Mock
    private EmployeeRepository employeeRepository;
    @Mock
    private DepartmentRepository departmentRepository;
    @Mock
    private UserRepository userRepository;

    private EmployeeServiceImpl employeeService;

    @BeforeEach
    void setUp() {
        employeeService = new EmployeeServiceImpl(employeeRepository, departmentRepository, userRepository);
    }

    @Test
    void getEmployeeById_ShouldThrowException_WhenIdDoesNotExist() {
        // Arrange
        UUID randomId = UUID.randomUUID();
        when(employeeRepository.findById(randomId)).thenReturn(Optional.empty());

        // Act & Assert
        assertThrows(ResourceNotFoundException.class, () -> employeeService.getEmployeeById(randomId));
    }

    @Test
    void createEmployee_ShouldSaveAndReturnDto_WhenRequestIsValid() {
        // Arrange
        EmployeeCreateRequest request = new EmployeeCreateRequest();
        request.setFirstName("John");
        request.setLastName("Doe");
        request.setJobTitle("Software Engineer");
        request.setDepartmentId(1L);
        request.setSalary(BigDecimal.valueOf(80000));
        request.setHireDate(LocalDate.now());

        Department department = Department.builder().id(1L).name("Engineering").code("ENG").build();

        when(departmentRepository.findById(1L)).thenReturn(Optional.of(department));
        when(employeeRepository.existsByEmployeeId(anyString())).thenReturn(false);
        
        when(employeeRepository.save(any(Employee.class))).thenAnswer(invocation -> {
            Employee emp = invocation.getArgument(0);
            emp.setId(UUID.randomUUID());
            return emp;
        });

        // Act
        EmployeeDto result = employeeService.createEmployee(request);

        // Assert
        assertNotNull(result);
        assertEquals("John", result.getFirstName());
        assertEquals("Doe", result.getLastName());
        assertEquals("Engineering", result.getDepartmentName());
        assertTrue(result.getEmployeeId().startsWith("EMP-"));
        
        verify(employeeRepository, times(1)).save(any(Employee.class));
    }
}

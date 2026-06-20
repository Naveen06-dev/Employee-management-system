package com.ems.modules.employee.mapper;

import com.ems.modules.employee.dto.EmployeeDto;
import com.ems.modules.employee.dto.EmployeeCreateRequest;
import com.ems.modules.employee.model.Employee;
import com.ems.modules.employee.model.EmployeeStatus;

public class EmployeeMapper {

    public static EmployeeDto toDto(Employee employee) {
        if (employee == null) {
            return null;
        }

        EmployeeDto.EmployeeDtoBuilder builder = EmployeeDto.builder()
                .id(employee.getId())
                .employeeId(employee.getEmployeeId())
                .firstName(employee.getFirstName())
                .lastName(employee.getLastName())
                .jobTitle(employee.getJobTitle())
                .salary(employee.getSalary())
                .hireDate(employee.getHireDate())
                .status(employee.getStatus().name());

        if (employee.getUser() != null) {
            builder.username(employee.getUser().getUsername());
            builder.email(employee.getUser().getEmail());
        }

        if (employee.getDepartment() != null) {
            builder.departmentId(employee.getDepartment().getId());
            builder.departmentName(employee.getDepartment().getName());
        }

        return builder.build();
    }

    public static Employee toEntity(EmployeeCreateRequest request) {
        if (request == null) {
            return null;
        }

        return Employee.builder()
                .firstName(request.getFirstName())
                .lastName(request.getLastName())
                .jobTitle(request.getJobTitle())
                .salary(request.getSalary())
                .hireDate(request.getHireDate())
                .status(EmployeeStatus.ACTIVE)
                .build();
    }
}

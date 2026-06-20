package com.ems.modules.employee.mapper;

import com.ems.modules.employee.dto.DepartmentDto;
import com.ems.modules.employee.model.Department;

public class DepartmentMapper {

    public static DepartmentDto toDto(Department department) {
        if (department == null) {
            return null;
        }

        DepartmentDto.DepartmentDtoBuilder builder = DepartmentDto.builder()
                .id(department.getId())
                .name(department.getName())
                .code(department.getCode());

        if (department.getManager() != null) {
            builder.managerId(department.getManager().getId());
            builder.managerName(department.getManager().getFirstName() + " " + department.getManager().getLastName());
        }

        return builder.build();
    }

    public static Department toEntity(DepartmentDto dto) {
        if (dto == null) {
            return null;
        }

        return Department.builder()
                .id(dto.getId())
                .name(dto.getName())
                .code(dto.getCode())
                .build();
    }
}

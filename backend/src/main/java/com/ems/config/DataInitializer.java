package com.ems.config;

import com.ems.modules.auth.model.Role;
import com.ems.modules.auth.model.RoleName;
import com.ems.modules.auth.model.User;
import com.ems.modules.auth.repository.RoleRepository;
import com.ems.modules.auth.repository.UserRepository;
import com.ems.modules.employee.model.Department;
import com.ems.modules.employee.model.Employee;
import com.ems.modules.employee.model.EmployeeStatus;
import com.ems.modules.employee.repository.DepartmentRepository;
import com.ems.modules.employee.repository.EmployeeRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.CommandLineRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Set;

@Slf4j
@Component
public class DataInitializer implements CommandLineRunner {

    private final RoleRepository roleRepository;
    private final UserRepository userRepository;
    private final EmployeeRepository employeeRepository;
    private final DepartmentRepository departmentRepository;
    private final PasswordEncoder passwordEncoder;

    public DataInitializer(RoleRepository roleRepository,
                           UserRepository userRepository,
                           EmployeeRepository employeeRepository,
                           DepartmentRepository departmentRepository,
                           PasswordEncoder passwordEncoder) {
        this.roleRepository = roleRepository;
        this.userRepository = userRepository;
        this.employeeRepository = employeeRepository;
        this.departmentRepository = departmentRepository;
        this.passwordEncoder = passwordEncoder;
    }

    @Override
    public void run(String... args) throws Exception {
        log.info("Checking and initializing role data...");
        for (RoleName roleName : RoleName.values()) {
            if (roleRepository.findByName(roleName).isEmpty()) {
                Role role = Role.builder()
                        .name(roleName)
                        .build();
                roleRepository.save(role);
                log.info("Seeded role: {}", roleName);
            }
        }
        log.info("Role data initialization completed.");

        log.info("Checking and initializing department data...");
        seedDepartment("Developer - UI/UX", "DEV_UIUX");
        seedDepartment("Developer - Frontend", "DEV_FRONTEND");
        seedDepartment("Developer - Backend", "DEV_BACKEND");
        seedDepartment("Developer - Database", "DEV_DB");
        seedDepartment("Data Analytics", "DATA_ANALYTICS");
        seedDepartment("Data Engineer", "DATA_ENGINEER");
        seedDepartment("Tester", "TESTER");
        seedDepartment("Intern", "INTERN");
        log.info("Department data initialization completed.");

        // Seed default users for easier development
        seedUser("admin", "admin@ems.com", "password123", RoleName.ROLE_ADMIN, "Admin", "User", "Administrator");
        seedUser("hr", "hr@ems.com", "password123", RoleName.ROLE_HR, "HR", "Manager", "HR Specialist");
        seedUser("employee", "employee@ems.com", "password123", RoleName.ROLE_EMPLOYEE, "Jane", "Doe", "Software Engineer");
    }

    private void seedDepartment(String name, String code) {
        if (!departmentRepository.existsByCode(code)) {
            Department department = Department.builder()
                    .name(name)
                    .code(code)
                    .build();
            departmentRepository.save(department);
            log.info("Seeded department: {} ({})", name, code);
        }
    }

    private void seedUser(String username, String email, String password, RoleName roleName, String firstName, String lastName, String jobTitle) {
        if (userRepository.findByUsername(username).isEmpty() && userRepository.findByEmail(email).isEmpty()) {
            Role role = roleRepository.findByName(roleName)
                    .orElseThrow(() -> new IllegalStateException("Role " + roleName + " not found during seeding."));

            User user = User.builder()
                    .username(username)
                    .email(email)
                    .passwordHash(passwordEncoder.encode(password))
                    .roles(Set.of(role))
                    .enabled(true)
                    .build();

            userRepository.save(user);
            log.info("Seeded default user: {}", username);

            Department dept = null;
            if (roleName == RoleName.ROLE_EMPLOYEE) {
                dept = departmentRepository.findByCode("DEV_BACKEND").orElse(null);
            }

            // Also seed default employee profile
            Employee employee = Employee.builder()
                    .user(user)
                    .employeeId(generateUniqueEmployeeId())
                    .firstName(firstName)
                    .lastName(lastName)
                    .jobTitle(jobTitle)
                    .department(dept)
                    .salary(new BigDecimal("75000.00"))
                    .hireDate(LocalDate.now())
                    .status(EmployeeStatus.ACTIVE)
                    .build();

            employeeRepository.save(employee);
            log.info("Seeded default employee profile for user: {}", username);
        }
    }

    private String generateUniqueEmployeeId() {
        String employeeId;
        boolean exists = true;
        java.security.SecureRandom random = new java.security.SecureRandom();
        do {
            int code = 1000 + random.nextInt(9000);
            employeeId = "EMP-" + code;
            exists = employeeRepository.existsByEmployeeId(employeeId);
        } while (exists);
        return employeeId;
    }
}

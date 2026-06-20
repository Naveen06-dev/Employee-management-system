package com.ems.config;

import com.ems.modules.auth.model.Role;
import com.ems.modules.auth.model.RoleName;
import com.ems.modules.auth.model.User;
import com.ems.modules.auth.repository.RoleRepository;
import com.ems.modules.auth.repository.UserRepository;
import com.ems.modules.employee.model.Employee;
import com.ems.modules.employee.model.EmployeeStatus;
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
    private final PasswordEncoder passwordEncoder;

    public DataInitializer(RoleRepository roleRepository,
                           UserRepository userRepository,
                           EmployeeRepository employeeRepository,
                           PasswordEncoder passwordEncoder) {
        this.roleRepository = roleRepository;
        this.userRepository = userRepository;
        this.employeeRepository = employeeRepository;
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

        // Seed default users for easier development
        seedUser("admin", "admin@ems.com", "password123", RoleName.ROLE_ADMIN, "Admin", "User", "Administrator");
        seedUser("hr", "hr@ems.com", "password123", RoleName.ROLE_HR, "HR", "Manager", "HR Specialist");
        seedUser("employee", "employee@ems.com", "password123", RoleName.ROLE_EMPLOYEE, "Jane", "Doe", "Software Engineer");
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

            // Also seed default employee profile
            Employee employee = Employee.builder()
                    .user(user)
                    .employeeId(generateUniqueEmployeeId())
                    .firstName(firstName)
                    .lastName(lastName)
                    .jobTitle(jobTitle)
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

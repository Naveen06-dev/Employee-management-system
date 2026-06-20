package com.ems.modules.recruitment.service;

import com.ems.exception.BadRequestException;
import com.ems.exception.ResourceNotFoundException;
import com.ems.modules.auth.model.User;
import com.ems.modules.auth.repository.UserRepository;
import com.ems.modules.employee.model.Department;
import com.ems.modules.employee.repository.DepartmentRepository;
import com.ems.modules.recruitment.dto.JobCreateRequest;
import com.ems.modules.recruitment.dto.JobDto;
import com.ems.modules.recruitment.mapper.JobMapper;
import com.ems.modules.recruitment.model.Job;
import com.ems.modules.recruitment.model.JobStatus;
import com.ems.modules.recruitment.repository.JobRepository;
import jakarta.persistence.criteria.Predicate;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.data.domain.*;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.util.ArrayList;
import java.util.List;

@Slf4j
@Service
public class JobServiceImpl implements JobService {

    private final JobRepository jobRepository;
    private final DepartmentRepository departmentRepository;
    private final UserRepository userRepository;

    public JobServiceImpl(
            JobRepository jobRepository,
            DepartmentRepository departmentRepository,
            UserRepository userRepository) {
        this.jobRepository = jobRepository;
        this.departmentRepository = departmentRepository;
        this.userRepository = userRepository;
    }

    @Override
    @Transactional(readOnly = true)
    public Page<JobDto> searchJobs(
            String search, Long departmentId, String status,
            int page, int size, String sortBy, String sortDir) {
        
        log.info("Searching job postings: search='{}', departmentId={}, status={}", search, departmentId, status);
        
        Sort sort = sortDir.equalsIgnoreCase(Sort.Direction.ASC.name()) ? 
                Sort.by(sortBy).ascending() : Sort.by(sortBy).descending();
        Pageable pageable = PageRequest.of(page, size, sort);

        Specification<Job> spec = (root, query, criteriaBuilder) -> {
            List<Predicate> predicates = new ArrayList<>();

            // 1. Text Search across title, description, requirements, location
            if (StringUtils.hasText(search)) {
                String searchPattern = "%" + search.toLowerCase() + "%";
                Predicate searchPredicate = criteriaBuilder.or(
                        criteriaBuilder.like(criteriaBuilder.lower(root.get("title")), searchPattern),
                        criteriaBuilder.like(criteriaBuilder.lower(root.get("description")), searchPattern),
                        criteriaBuilder.like(criteriaBuilder.lower(root.get("requirements")), searchPattern),
                        criteriaBuilder.like(criteriaBuilder.lower(root.get("location")), searchPattern)
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
                    JobStatus jobStatus = JobStatus.valueOf(status.toUpperCase());
                    predicates.add(criteriaBuilder.equal(root.get("status"), jobStatus));
                } catch (IllegalArgumentException e) {
                    log.warn("Invalid job status filter passed: {}", status);
                }
            } else {
                // Default to showing only OPEN jobs for generic searches
                predicates.add(criteriaBuilder.equal(root.get("status"), JobStatus.OPEN));
            }

            return criteriaBuilder.and(predicates.toArray(new Predicate[0]));
        };

        Page<Job> jobPage = jobRepository.findAll(spec, pageable);
        List<JobDto> jobDtos = jobPage.getContent().stream()
                .map(JobMapper::toDto)
                .toList();

        return new PageImpl<>(jobDtos, pageable, jobPage.getTotalElements());
    }

    @Override
    @Transactional(readOnly = true)
    @Cacheable(value = "jobs", key = "#id")
    public JobDto getJobById(Long id) {
        log.info("Fetching job details for ID: {} (cache miss)", id);
        Job job = jobRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Job posting not found with ID: " + id));
        return JobMapper.toDto(job);
    }

    @Override
    @Transactional
    @CacheEvict(value = "dashboardStats", allEntries = true)
    public JobDto createJob(JobCreateRequest request, String username) {
        log.info("Creating new job posting by user: {}", username);

        Department department = departmentRepository.findById(request.getDepartmentId())
                .orElseThrow(() -> new ResourceNotFoundException("Department not found with ID: " + request.getDepartmentId()));

        User user = userRepository.findByUsername(username)
                .orElseThrow(() -> new ResourceNotFoundException("User not found with username: " + username));

        Job job = JobMapper.toEntity(request);
        job.setDepartment(department);
        job.setCreatedBy(user);

        jobRepository.save(job);
        log.info("Job posting created successfully. ID: {}", job.getId());
        return JobMapper.toDto(job);
    }

    @Override
    @Transactional
    @CacheEvict(value = {"jobs", "dashboardStats"}, key = "#id")
    public JobDto updateJob(Long id, JobCreateRequest request) {
        log.info("Updating job posting ID: {}", id);
        Job job = jobRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Job posting not found with ID: " + id));

        Department department = departmentRepository.findById(request.getDepartmentId())
                .orElseThrow(() -> new ResourceNotFoundException("Department not found with ID: " + request.getDepartmentId()));

        job.setTitle(request.getTitle());
        job.setDescription(request.getDescription());
        job.setRequirements(request.getRequirements());
        job.setLocation(request.getLocation());
        job.setSalaryRange(request.getSalaryRange());
        job.setDepartment(department);

        jobRepository.save(job);
        log.info("Job posting ID: {} updated successfully", id);
        return JobMapper.toDto(job);
    }

    @Override
    @Transactional
    @CacheEvict(value = {"jobs", "dashboardStats"}, key = "#id")
    public JobDto changeJobStatus(Long id, String statusStr) {
        log.info("Changing status of job ID: {} to {}", id, statusStr);
        Job job = jobRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Job posting not found with ID: " + id));

        JobStatus status;
        try {
            status = JobStatus.valueOf(statusStr.toUpperCase());
        } catch (IllegalArgumentException e) {
            throw new BadRequestException("Invalid status: Must be DRAFT, OPEN, or CLOSED.");
        }

        job.setStatus(status);
        jobRepository.save(job);
        log.info("Job ID: {} status changed to {}", id, status);
        return JobMapper.toDto(job);
    }

    @Override
    @Transactional
    @CacheEvict(value = {"jobs", "dashboardStats"}, key = "#id")
    public void deleteJob(Long id) {
        log.info("Deleting job posting ID: {}", id);
        Job job = jobRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Job posting not found with ID: " + id));
        jobRepository.delete(job);
        log.info("Job ID: {} deleted successfully", id);
    }
}

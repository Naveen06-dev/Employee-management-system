package com.ems.modules.recruitment.repository;

import com.ems.modules.recruitment.model.Job;
import com.ems.modules.recruitment.model.JobStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface JobRepository extends JpaRepository<Job, Long>, JpaSpecificationExecutor<Job> {
    List<Job> findByStatus(JobStatus status);
    long countByStatus(JobStatus status);
}

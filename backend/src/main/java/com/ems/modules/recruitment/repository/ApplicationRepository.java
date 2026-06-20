package com.ems.modules.recruitment.repository;

import com.ems.modules.recruitment.model.Application;
import com.ems.modules.recruitment.model.ApplicationStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface ApplicationRepository extends JpaRepository<Application, Long> {
    List<Application> findByCandidateId(UUID candidateId);
    boolean existsByJobIdAndCandidateId(Long jobId, UUID candidateId);
    long countByStatus(ApplicationStatus status);
}

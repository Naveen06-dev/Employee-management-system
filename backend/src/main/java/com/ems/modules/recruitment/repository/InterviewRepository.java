package com.ems.modules.recruitment.repository;

import com.ems.modules.recruitment.model.Interview;
import com.ems.modules.recruitment.model.InterviewStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface InterviewRepository extends JpaRepository<Interview, Long> {
    List<Interview> findByInterviewerId(UUID interviewerId);
    long countByStatus(InterviewStatus status);
}

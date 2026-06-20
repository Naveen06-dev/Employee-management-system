package com.ems.modules.recruitment.service;

import com.ems.modules.recruitment.dto.*;

import java.util.List;

public interface RecruitmentService {
    ApplicationDto applyForJob(String username, ApplicationSubmitRequest request);
    List<ApplicationDto> getCandidateApplications(String username);
    List<ApplicationDto> getAllApplications();
    ApplicationDto changeApplicationStatus(Long applicationId, String status);
    
    InterviewDto scheduleInterview(InterviewScheduleRequest request);
    List<InterviewDto> getInterviewsForInterviewer(String username);
    InterviewDto submitInterviewFeedback(Long interviewId, InterviewFeedbackRequest request, String reviewerUsername);
}

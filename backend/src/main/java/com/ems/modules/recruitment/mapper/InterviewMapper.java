package com.ems.modules.recruitment.mapper;

import com.ems.modules.recruitment.dto.InterviewDto;
import com.ems.modules.recruitment.model.Interview;

public class InterviewMapper {

    public static InterviewDto toDto(Interview interview) {
        if (interview == null) {
            return null;
        }

        return InterviewDto.builder()
                .id(interview.getId())
                .applicationId(interview.getApplication().getId())
                .jobTitle(interview.getApplication().getJob().getTitle())
                .candidateName(interview.getApplication().getCandidate().getUsername())
                .interviewerId(interview.getInterviewer().getId())
                .interviewerName(interview.getInterviewer().getFirstName() + " " + interview.getInterviewer().getLastName())
                .scheduledAt(interview.getScheduledAt())
                .durationMinutes(interview.getDurationMinutes())
                .meetLink(interview.getMeetLink())
                .status(interview.getStatus().name())
                .feedback(interview.getFeedback())
                .rating(interview.getRating())
                .build();
    }
}

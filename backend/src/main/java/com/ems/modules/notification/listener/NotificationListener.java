package com.ems.modules.notification.listener;

import com.ems.common.event.InterviewScheduledEvent;
import com.ems.common.event.LeaveRequestedEvent;
import com.ems.common.event.LeaveReviewedEvent;
import com.ems.common.event.UserRegisteredEvent;
import com.ems.modules.notification.service.EmailService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.event.EventListener;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;

@Slf4j
@Component
public class NotificationListener {

    private final EmailService emailService;

    public NotificationListener(EmailService emailService) {
        this.emailService = emailService;
    }

    @Async
    @EventListener
    public void handleUserRegistration(UserRegisteredEvent event) {
        log.info("Async notification triggered for UserRegisteredEvent");
        String to = event.getUser().getEmail();
        String subject = "Welcome to EMS Platform!";
        String body = String.format(
                "Hello %s,\n\nYour account has been registered successfully. Welcome aboard!\n\nBest Regards,\nEMS Team",
                event.getUser().getUsername()
        );
        emailService.sendEmail(to, subject, body);
    }

    @Async
    @EventListener
    public void handleLeaveRequested(LeaveRequestedEvent event) {
        log.info("Async notification triggered for LeaveRequestedEvent");
        String to = event.getLeave().getEmployee().getUser() != null ? 
                event.getLeave().getEmployee().getUser().getEmail() : "admin@ems.com";
        String subject = "Leave Request Submitted";
        String body = String.format(
                "Hello %s,\n\nYour request for leave from %s to %s (Type: %s) has been successfully submitted and is pending review.\n\nBest Regards,\nHR Team",
                event.getLeave().getEmployee().getFirstName(),
                event.getLeave().getStartDate(),
                event.getLeave().getEndDate(),
                event.getLeave().getLeaveType()
        );
        emailService.sendEmail(to, subject, body);
    }

    @Async
    @EventListener
    public void handleLeaveReviewed(LeaveReviewedEvent event) {
        log.info("Async notification triggered for LeaveReviewedEvent");
        String to = event.getLeave().getEmployee().getUser() != null ? 
                event.getLeave().getEmployee().getUser().getEmail() : "admin@ems.com";
        String subject = "Leave Request Status Update";
        String body = String.format(
                "Hello %s,\n\nYour leave request from %s to %s has been reviewed and is %s by %s %s.\n\nBest Regards,\nHR Team",
                event.getLeave().getEmployee().getFirstName(),
                event.getLeave().getStartDate(),
                event.getLeave().getEndDate(),
                event.getLeave().getStatus(),
                event.getLeave().getApprovedBy().getFirstName(),
                event.getLeave().getApprovedBy().getLastName()
        );
        emailService.sendEmail(to, subject, body);
    }

    @Async
    @EventListener
    public void handleInterviewScheduled(InterviewScheduledEvent event) {
        log.info("Async notification triggered for InterviewScheduledEvent");
        
        // Notify Candidate
        String candidateEmail = event.getInterview().getApplication().getCandidate().getEmail();
        String candidateSubject = "Interview Scheduled: " + event.getInterview().getApplication().getJob().getTitle();
        String candidateBody = String.format(
                "Hello %s,\n\nYour interview for the post of '%s' has been scheduled.\nDate/Time: %s\nInterviewer: %s %s\nMeet Link: %s\n\nGood luck!\nRecruitment Team",
                event.getInterview().getApplication().getCandidate().getUsername(),
                event.getInterview().getApplication().getJob().getTitle(),
                event.getInterview().getScheduledAt(),
                event.getInterview().getInterviewer().getFirstName(),
                event.getInterview().getInterviewer().getLastName(),
                event.getInterview().getMeetLink()
        );
        emailService.sendEmail(candidateEmail, candidateSubject, candidateBody);

        // Notify Interviewer (Employee)
        if (event.getInterview().getInterviewer().getUser() != null) {
            String interviewerEmail = event.getInterview().getInterviewer().getUser().getEmail();
            String interviewerSubject = "Assigned Interview: Candidate " + event.getInterview().getApplication().getCandidate().getUsername();
            String interviewerBody = String.format(
                    "Hello %s,\n\nYou have been assigned to conduct an interview for the post of '%s'.\nCandidate: %s\nDate/Time: %s\nMeet Link: %s\n\nPlease check your portal to record feedback afterward.\n\nBest Regards,\nHR Team",
                    event.getInterview().getInterviewer().getFirstName(),
                    event.getInterview().getApplication().getJob().getTitle(),
                    event.getInterview().getApplication().getCandidate().getUsername(),
                    event.getInterview().getScheduledAt(),
                    event.getInterview().getMeetLink()
            );
            emailService.sendEmail(interviewerEmail, interviewerSubject, interviewerBody);
        }
    }
}

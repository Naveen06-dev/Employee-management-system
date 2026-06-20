package com.ems.common.event;

import com.ems.modules.recruitment.model.Interview;
import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public class InterviewScheduledEvent {
    private final Interview interview;
}

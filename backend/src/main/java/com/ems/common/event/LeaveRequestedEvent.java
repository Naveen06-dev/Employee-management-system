package com.ems.common.event;

import com.ems.modules.employee.model.Leave;
import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public class LeaveRequestedEvent {
    private final Leave leave;
}

package com.devbridge.backend.domain.schedule.service;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class MeetingReminderSchedulerTest {

    @Mock
    private MeetingService meetingService;

    @Test
    void sendDueReminders_대상회의목록을조회해_각회의에대해sendReminder를호출한다() {
        when(meetingService.findMeetingIdsDueForReminder(any(), any()))
                .thenReturn(List.of("meeting-1", "meeting-2"));

        MeetingReminderScheduler scheduler = new MeetingReminderScheduler(meetingService);

        scheduler.sendDueReminders();

        verify(meetingService).sendReminder("meeting-1");
        verify(meetingService).sendReminder("meeting-2");
    }

    @Test
    void sendDueReminders_특정회의처리중예외가발생해도_나머지회의는계속처리한다() {
        when(meetingService.findMeetingIdsDueForReminder(any(), any()))
                .thenReturn(List.of("meeting-1", "meeting-2"));
        doThrow(new RuntimeException("전송 실패")).when(meetingService).sendReminder("meeting-1");

        MeetingReminderScheduler scheduler = new MeetingReminderScheduler(meetingService);

        scheduler.sendDueReminders();

        verify(meetingService).sendReminder("meeting-1");
        verify(meetingService).sendReminder("meeting-2");
    }
}

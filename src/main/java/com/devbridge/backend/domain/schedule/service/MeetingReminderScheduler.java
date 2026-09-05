package com.devbridge.backend.domain.schedule.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.util.List;

@Slf4j
@Component
@RequiredArgsConstructor
public class MeetingReminderScheduler {

    private static final int REMINDER_WINDOW_MINUTES = 10;

    private final MeetingService meetingService;

    // 단일 인스턴스 배포 전제(README 운영 노트)에서만 별도 분산 락 없이 안전하다.
    @Scheduled(fixedRate = 60_000)
    public void sendDueReminders() {
        LocalDateTime now = LocalDateTime.now();
        List<String> meetingIds = meetingService.findMeetingIdsDueForReminder(now, now.plusMinutes(REMINDER_WINDOW_MINUTES));

        for (String meetingId : meetingIds) {
            try {
                meetingService.sendReminder(meetingId);
            } catch (Exception e) {
                log.error("회의 리마인더 발송 실패: meetingId={}", meetingId, e);
            }
        }
    }
}

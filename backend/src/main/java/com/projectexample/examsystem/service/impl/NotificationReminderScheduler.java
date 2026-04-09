package com.projectexample.examsystem.service.impl;

import com.projectexample.examsystem.service.NotificationService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class NotificationReminderScheduler {

    private final NotificationService notificationService;

    @Scheduled(initialDelay = 60000, fixedDelay = 60000)
    public void dispatchUpcomingExamReminders() {
        try {
            int deliveredCount = notificationService.dispatchUpcomingExamReminders();
            if (deliveredCount > 0) {
                log.info("Dispatched {} upcoming exam reminder notifications", deliveredCount);
            }
        } catch (Exception exception) {
            log.error("Failed to dispatch upcoming exam reminders", exception);
        }
    }
}

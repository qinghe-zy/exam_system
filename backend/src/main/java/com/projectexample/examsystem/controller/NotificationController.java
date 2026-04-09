package com.projectexample.examsystem.controller;

import com.projectexample.examsystem.common.ApiResponse;
import com.projectexample.examsystem.common.PageResponse;
import com.projectexample.examsystem.dto.NotificationDeliveryLogQueryRequest;
import com.projectexample.examsystem.dto.NotificationTemplateSaveRequest;
import com.projectexample.examsystem.service.NotificationService;
import com.projectexample.examsystem.vo.NotificationDeliveryLogVO;
import com.projectexample.examsystem.vo.NotificationTemplateVO;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/notifications")
@RequiredArgsConstructor
public class NotificationController {

    private final NotificationService notificationService;

    @GetMapping("/templates")
    @PreAuthorize("hasAnyRole('ADMIN','ORG_ADMIN','TEACHER')")
    public ApiResponse<List<NotificationTemplateVO>> templates() {
        return ApiResponse.success(notificationService.listTemplates());
    }

    @PostMapping("/templates")
    @PreAuthorize("hasAnyRole('ADMIN','ORG_ADMIN','TEACHER')")
    public ApiResponse<NotificationTemplateVO> createTemplate(@Valid @RequestBody NotificationTemplateSaveRequest request) {
        return ApiResponse.success("notification template created", notificationService.createTemplate(request));
    }

    @PutMapping("/templates/{id}")
    @PreAuthorize("hasAnyRole('ADMIN','ORG_ADMIN','TEACHER')")
    public ApiResponse<NotificationTemplateVO> updateTemplate(@PathVariable Long id,
                                                              @Valid @RequestBody NotificationTemplateSaveRequest request) {
        return ApiResponse.success("notification template updated", notificationService.updateTemplate(id, request));
    }

    @DeleteMapping("/templates/{id}")
    @PreAuthorize("hasAnyRole('ADMIN','ORG_ADMIN','TEACHER')")
    public ApiResponse<Void> deleteTemplate(@PathVariable Long id) {
        notificationService.deleteTemplate(id);
        return ApiResponse.success("notification template deleted", null);
    }

    @GetMapping("/delivery-logs")
    @PreAuthorize("hasAnyRole('ADMIN','ORG_ADMIN','TEACHER')")
    public ApiResponse<PageResponse<NotificationDeliveryLogVO>> deliveryLogs(@ModelAttribute NotificationDeliveryLogQueryRequest request) {
        return ApiResponse.success(notificationService.pageDeliveryLogs(request));
    }

    @PostMapping("/exam-reminders/dispatch")
    @PreAuthorize("hasAnyRole('ADMIN','ORG_ADMIN','TEACHER')")
    public ApiResponse<Integer> dispatchUpcomingExamReminders() {
        return ApiResponse.success("upcoming exam reminders dispatched", notificationService.dispatchUpcomingExamReminders());
    }
}

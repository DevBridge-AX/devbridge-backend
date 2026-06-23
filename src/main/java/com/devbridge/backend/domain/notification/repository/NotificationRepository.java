package com.devbridge.backend.domain.notification.repository;

import com.devbridge.backend.domain.notification.entity.Notification;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface NotificationRepository extends JpaRepository<Notification, String> {

    List<Notification> findByUser_IdOrderByCreatedAtDesc(String userId);

    Page<Notification> findByUser_EmployeeIdAndDeletedAtIsNull(String employeeId, Pageable pageable);

    Page<Notification> findByUser_EmployeeIdAndIsReadAndDeletedAtIsNull(
            String employeeId, Boolean isRead, Pageable pageable);
}

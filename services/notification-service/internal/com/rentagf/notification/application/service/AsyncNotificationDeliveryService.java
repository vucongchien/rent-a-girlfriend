package com.rentagf.notification.application.service;

import com.rentagf.notification.application.port.outbound.NotificationSender;
import com.rentagf.notification.application.port.outbound.SendResult;
import com.rentagf.notification.application.registry.NotificationSenderRegistry;
import com.rentagf.notification.domain.aggregate.Notification;
import com.rentagf.notification.domain.repository.NotificationRepository;
import com.rentagf.notification.domain.vo.DeliveryAttempt;
import com.rentagf.notification.domain.vo.enums.DeliveryChannel;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

/**
 * Helper Service chịu trách nhiệm gửi tin vật lý bất đồng bộ (@Async) chạy trên Virtual Threads.
 * Việc tách riêng ra class này đảm bảo Spring Proxy hoạt động chính xác (không bị AOP proxy bypass).
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class AsyncNotificationDeliveryService {

    private final NotificationRepository notificationRepository;
    private final NotificationSenderRegistry senderRegistry;

    /**
     * Gửi tin bất đồng bộ chạy trên Virtual Threads.
     * Sử dụng transaction độc lập để ghi nhận từng attempt gửi tin.
     *
     * @param notificationId ID của Notification.
     * @param channel        Kênh gửi tin vật lý.
     */
    @Async
    @Transactional
    public void sendAsync(UUID notificationId, DeliveryChannel channel) {
        log.info("Async sending notification: {} via channel: {} inside thread: {}", 
                notificationId, channel, Thread.currentThread());

        Notification notification = notificationRepository.findById(notificationId)
                .orElseThrow(() -> new IllegalArgumentException("Notification not found: " + notificationId));

        DeliveryAttempt attempt = null;
        try {
            // 1. Tạo attempt mới (Chuyển sang trạng thái PROCESSING)
            attempt = notification.createAttempt(channel);
            notificationRepository.save(notification);

            // 2. Tìm Strategy Sender phù hợp
            NotificationSender sender = senderRegistry.getSender(channel)
                    .orElseThrow(() -> new IllegalArgumentException("No sender strategy found for channel: " + channel));

            // 3. Thực thi gửi vật lý thông qua Adapter
            SendResult result = sender.send(notification);

            // 4. Xử lý kết quả dựa trên Invariants & Failure Handling
            if (result.isSuccess()) {
                notification.markAttemptSuccess(attempt.getId(), result.getMessageId());
                log.info("Successfully sent notification: {} via message: {}", notificationId, result.getMessageId());
            } else {
                notification.markAttemptFailed(attempt.getId(), result.getErrorCode(), result.getErrorMessage(), result.isRecoverable());
                log.warn("Failed to send notification: {} via channel: {}. Error: {}", notificationId, channel, result.getErrorMessage());
            }

        } catch (Exception e) {
            log.error("Unexpected error during async send for notification: {}", notificationId, e);
            if (attempt != null) {
                notification.markAttemptFailed(attempt.getId(), "SYSTEM_ERROR", e.getMessage(), true);
            }
        } finally {
            notificationRepository.save(notification);
        }
    }
}

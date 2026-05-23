package com.rentagf.notification.infrastructure.persistence.jpa;

import com.rentagf.notification.domain.aggregate.Notification;
import com.rentagf.notification.domain.repository.NotificationRepository;
import com.rentagf.notification.infrastructure.persistence.jpa.entity.NotificationJpaEntity;
import com.rentagf.notification.infrastructure.persistence.jpa.mapper.NotificationMapper;
import com.rentagf.notification.infrastructure.persistence.jpa.repository.NotificationJpaRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Limit;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Collectors;

@Repository
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class NotificationRepositoryImpl implements NotificationRepository {

    private final NotificationJpaRepository jpaRepository;
    private final NotificationMapper mapper;

    @Override
    @Transactional
    public Notification save(Notification notification) {
        NotificationJpaEntity jpaEntity = mapper.toJpaEntity(notification);
        NotificationJpaEntity saved = jpaRepository.save(jpaEntity);
        return mapper.toDomain(saved);
    }

    @Override
    public Optional<Notification> findById(UUID id) {
        return jpaRepository.findById(id).map(mapper::toDomain);
    }

    @Override
    public Optional<Notification> findByEventIdAndUserId(String eventId, UUID userId) {
        return jpaRepository.findByEventIdAndUserId(eventId, userId).map(mapper::toDomain);
    }

    @Override
    public List<Notification> findByUserId(UUID userId, Instant cursor, UUID cursorId, int limit, boolean unreadOnly) {
        List<NotificationJpaEntity> entities;
        Limit springLimit = Limit.of(limit);
        if (cursor == null || cursorId == null) {
            entities = jpaRepository.findFirstPage(userId, unreadOnly, springLimit);
        } else {
            entities = jpaRepository.findWithCursor(userId, cursor, cursorId, unreadOnly, springLimit);
        }
        return entities.stream().map(mapper::toDomain).collect(Collectors.toList());
    }

    @Override
    public long countUnreadByUserId(UUID userId) {
        return jpaRepository.countByUserIdAndReadAtIsNull(userId);
    }

    @Override
    @Transactional
    public void markAsRead(UUID notificationId, Instant readAt) {
        jpaRepository.updateReadAt(notificationId, readAt != null ? readAt : Instant.now(), Instant.now());
    }

    @Override
    @Transactional
    public void markAllAsRead(UUID userId, Instant readAt) {
        jpaRepository.updateAllReadAt(userId, readAt != null ? readAt : Instant.now(), Instant.now());
    }
}

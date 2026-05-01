package com.ascmoda.notification.domain.repository;

import com.ascmoda.notification.domain.model.NotificationMessage;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.lang.Nullable;

import java.util.Optional;
import java.util.UUID;

public interface NotificationMessageRepository
        extends JpaRepository<NotificationMessage, UUID>, JpaSpecificationExecutor<NotificationMessage> {

    Optional<NotificationMessage> findByEventId(UUID eventId);

    boolean existsByEventId(UUID eventId);

    @Override
    Page<NotificationMessage> findAll(@Nullable Specification<NotificationMessage> specification, Pageable pageable);
}

package com.vimainsurance.vimaadmin.notification;

import java.util.Optional;
import java.util.UUID;

import com.vimainsurance.vimaadmin.notification.dto.CreateNotificationCommand;

public interface NotificationService {

    /**
     * @return notification id when a new row was created; empty when deduplicated (already exists).
     */
    Optional<UUID> createIfAbsent(CreateNotificationCommand command);
}

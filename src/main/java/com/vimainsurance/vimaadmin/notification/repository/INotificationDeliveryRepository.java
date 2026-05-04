package com.vimainsurance.vimaadmin.notification.repository;

import java.time.LocalDateTime;
import java.util.Collection;
import java.util.List;
import java.util.UUID;

import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import com.vimainsurance.vimaadmin.notification.entity.NotificationDelivery;
import com.vimainsurance.vimaadmin.notification.enums.NotificationDeliveryStatus;

@Repository
public interface INotificationDeliveryRepository extends JpaRepository<NotificationDelivery, UUID> {

    @Query("""
            SELECT d FROM NotificationDelivery d
            JOIN FETCH d.notification n
            JOIN FETCH n.recipient
            LEFT JOIN FETCH n.company
            WHERE d.status IN :statuses
              AND (d.nextRetryAt IS NULL OR d.nextRetryAt <= :now)
              AND d.attemptCount < :maxAttempts
            ORDER BY d.createdAt ASC
            """)
    List<NotificationDelivery> findDueForDispatch(
            @Param("statuses") Collection<NotificationDeliveryStatus> statuses,
            @Param("now") LocalDateTime now,
            @Param("maxAttempts") int maxAttempts,
            Pageable pageable);
}

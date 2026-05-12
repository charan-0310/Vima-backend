package com.vimainsurance.vimaadmin.notification.repository;

import java.util.Collection;
import java.time.LocalDateTime;
import java.util.Optional;
import java.util.UUID;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import com.vimainsurance.vimaadmin.notification.entity.AdminNotification;
import com.vimainsurance.vimaadmin.notification.enums.NotificationCategory;

@Repository
public interface IAdminNotificationRepository extends JpaRepository<AdminNotification, UUID> {

    boolean existsByDedupKey(String dedupKey);

    Optional<AdminNotification> findByDedupKey(String dedupKey);

    @EntityGraph(attributePaths = { "deliveries", "recipient", "company" })
    @Query("SELECT n FROM AdminNotification n WHERE n.id = :id")
    Optional<AdminNotification> findByIdForDispatch(@Param("id") UUID id);

    long countByRecipient_IdAndReadAtIsNull(UUID recipientId);

    /**
     * HR (and optional VIMA company filter): include rows with {@code company_id} null so legacy/manual rows still appear.
     * Rows remain scoped to {@code recipientId} only.
     */
    @Query("""
            SELECT n FROM AdminNotification n
            WHERE n.recipient.id = :recipientId
              AND (:unreadOnly = false OR n.readAt IS NULL)
              AND (:category IS NULL OR n.category = :category)
              AND (:companyId IS NULL OR n.company IS NULL OR n.company.organizationId = :companyId)
            """)
    Page<AdminNotification> findInbox(
            @Param("recipientId") UUID recipientId,
            @Param("unreadOnly") boolean unreadOnly,
            @Param("category") NotificationCategory category,
            @Param("companyId") UUID companyId,
            Pageable pageable);

    @Query("""
            SELECT n FROM AdminNotification n
            WHERE n.recipient.role IN :roles
              AND (:unreadOnly = false OR n.readAt IS NULL)
              AND (:category IS NULL OR n.category = :category)
              AND (:companyId IS NULL OR n.company IS NULL OR n.company.organizationId = :companyId)
            """)
    Page<AdminNotification> findInboxByRecipientRoles(
            @Param("roles") Collection<String> roles,
            @Param("unreadOnly") boolean unreadOnly,
            @Param("category") NotificationCategory category,
            @Param("companyId") UUID companyId,
            Pageable pageable);

    @Query("""
            SELECT n FROM AdminNotification n
            WHERE n.recipient.id IN :recipientIds
              AND (:unreadOnly = false OR n.readAt IS NULL)
              AND (:category IS NULL OR n.category = :category)
              AND (:companyId IS NULL OR n.company IS NULL OR n.company.organizationId = :companyId)
            """)
    Page<AdminNotification> findInboxByRecipientIds(
            @Param("recipientIds") Collection<UUID> recipientIds,
            @Param("unreadOnly") boolean unreadOnly,
            @Param("category") NotificationCategory category,
            @Param("companyId") UUID companyId,
            Pageable pageable);

    @Query("""
            SELECT n FROM AdminNotification n
            WHERE LOWER(n.receiverEmail) = LOWER(:receiverEmail)
              AND (:unreadOnly = false OR n.readAt IS NULL)
              AND (:category IS NULL OR n.category = :category)
            """)
    Page<AdminNotification> findInboxByReceiverEmail(
            @Param("receiverEmail") String receiverEmail,
            @Param("unreadOnly") boolean unreadOnly,
            @Param("category") NotificationCategory category,
            Pageable pageable);

    @Query("""
            SELECT COUNT(n) FROM AdminNotification n
            WHERE LOWER(n.receiverEmail) = LOWER(:receiverEmail)
              AND n.readAt IS NULL
            """)
    long countUnreadByReceiverEmail(@Param("receiverEmail") String receiverEmail);

    @Modifying
    @Query("""
            UPDATE AdminNotification n SET n.readAt = :readAt, n.updatedAt = :readAt
            WHERE n.readAt IS NULL
              AND LOWER(n.receiverEmail) = LOWER(:receiverEmail)
            """)
    int markAllReadByReceiverEmail(
            @Param("receiverEmail") String receiverEmail,
            @Param("readAt") LocalDateTime readAt);

    @Query("""
            SELECT COUNT(n) FROM AdminNotification n
            WHERE n.recipient.role IN :roles
              AND n.readAt IS NULL
            """)
    long countUnreadByRecipientRoles(@Param("roles") Collection<String> roles);

    @Query("""
            SELECT COUNT(n) FROM AdminNotification n
            WHERE n.recipient.id IN :recipientIds
              AND n.readAt IS NULL
            """)
    long countUnreadByRecipientIds(@Param("recipientIds") Collection<UUID> recipientIds);

    @Modifying
    @Query("UPDATE AdminNotification n SET n.readAt = :readAt, n.updatedAt = :readAt WHERE n.id = :id AND n.recipient.id = :recipientId AND n.readAt IS NULL")
    int markReadIfOwned(@Param("id") UUID id, @Param("recipientId") UUID recipientId, @Param("readAt") LocalDateTime readAt);

    @Modifying
    @Query("UPDATE AdminNotification n SET n.isStarred = :starred, n.updatedAt = :updatedAt WHERE n.id = :id AND n.recipient.id = :recipientId")
    int markStarredIfOwned(
            @Param("id") UUID id,
            @Param("recipientId") UUID recipientId,
            @Param("starred") boolean starred,
            @Param("updatedAt") LocalDateTime updatedAt);

    @Modifying
    @Query("""
            UPDATE AdminNotification n SET n.readAt = :readAt, n.updatedAt = :readAt
            WHERE n.recipient.id = :recipientId AND n.readAt IS NULL
              AND (:companyId IS NULL OR n.company IS NULL OR n.company.organizationId = :companyId)
            """)
    int markAllReadForRecipient(
            @Param("recipientId") UUID recipientId,
            @Param("companyId") UUID companyId,
            @Param("readAt") LocalDateTime readAt);

    /**
     * Shared Vima audience inbox: same company filter semantics as {@link #findInboxByRecipientIds}.
     * Caller must pass a non-empty {@code recipientIds}.
     */
    @Modifying
    @Query("""
            UPDATE AdminNotification n SET n.readAt = :readAt, n.updatedAt = :readAt
            WHERE n.recipient.id IN :recipientIds AND n.readAt IS NULL
              AND (:companyId IS NULL OR n.company IS NULL OR n.company.organizationId = :companyId)
            """)
    int markAllReadForRecipientIds(
            @Param("recipientIds") Collection<UUID> recipientIds,
            @Param("companyId") UUID companyId,
            @Param("readAt") LocalDateTime readAt);

    /**
     * Marks all unread fan-out rows in the shared pool that share the same logical dedup prefix as
     * {@code logicalKey} / {@code dedupPrefix} (see {@link com.vimainsurance.vimaadmin.notification.AdminNotificationInboxService#logicalDedupKey}).
     */
    @Modifying
    @Query("""
            UPDATE AdminNotification n SET n.readAt = :readAt, n.updatedAt = :readAt
            WHERE n.readAt IS NULL AND n.recipient.id IN :recipientIds
              AND (n.dedupKey = :logicalKey OR n.dedupKey LIKE CONCAT(:dedupPrefix, '%'))
            """)
    int markReadLogicalGroupForRecipients(
            @Param("recipientIds") Collection<UUID> recipientIds,
            @Param("logicalKey") String logicalKey,
            @Param("dedupPrefix") String dedupPrefix,
            @Param("readAt") LocalDateTime readAt);
}

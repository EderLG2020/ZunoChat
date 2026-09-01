package com.example.backend.module.usermanagement.persistence;

import com.example.backend.module.usermanagement.domain.AdminAuditLogModel;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface AdminAuditLogRepository extends JpaRepository<AdminAuditLogModel, Long> {

    /** targetId/actorId opcionales — cualquiera de los dos puede venir null para no filtrar por ese campo. */
    @Query("""
        SELECT a FROM AdminAuditLogModel a
        WHERE (:targetId IS NULL OR a.targetId = :targetId)
          AND (:actorId IS NULL OR a.actorId = :actorId)
        ORDER BY a.createdAt DESC
        """)
    Page<AdminAuditLogModel> search(@Param("targetId") Long targetId, @Param("actorId") Long actorId, Pageable pageable);
}

package com.example.backend.module.usermanagement.persistence;

import com.example.backend.module.usermanagement.domain.BlockedUserModel;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface BlockedUserRepository extends JpaRepository<BlockedUserModel, Long> {

    boolean existsByBlockerIdAndBlockedId(Long blockerId, Long blockedId);

    void deleteByBlockerIdAndBlockedId(Long blockerId, Long blockedId);

    List<BlockedUserModel> findAllByBlockerIdOrderByCreatedAtDesc(Long blockerId);

    /** true si cualquiera de los dos bloqueó al otro — usado antes de dejar enviar un mensaje. */
    @Query("""
        SELECT COUNT(b) > 0 FROM BlockedUserModel b
        WHERE (b.blockerId = :userA AND b.blockedId = :userB)
           OR (b.blockerId = :userB AND b.blockedId = :userA)
        """)
    boolean existsBetween(@Param("userA") Long userA, @Param("userB") Long userB);
}

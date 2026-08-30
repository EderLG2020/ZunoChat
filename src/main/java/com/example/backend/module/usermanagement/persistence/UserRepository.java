package com.example.backend.module.usermanagement.persistence;

import com.example.backend.common.enums.Role;
import com.example.backend.common.enums.UserStatus;
import com.example.backend.module.usermanagement.domain.UserModel;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Repository
public interface UserRepository extends JpaRepository<UserModel, Long> {

    long countByRole(Role role);
    List<UserModel> findByRole(Role role, Pageable pageable);
    Optional<UserModel> findByUsername(String username);
    Optional<UserModel> findByEmail(String email);
    Optional<UserModel> findByUsernameOrEmail(String username, String email);
    boolean existsByUsername(String username);
    boolean existsByEmail(String email);
    boolean existsByDni(String dni);

    /**
     * Búsqueda por prefijo sobre username_lower (indexada) — el llamador debe
     * pasar `term` ya en minúsculas. A diferencia de un LIKE '%term%' sobre
     * LOWER(username), esto sí puede usar idx_users_username_lower en vez de
     * escanear toda la tabla.
     */
    @Query("""
        SELECT u FROM UserModel u
        WHERE u.status = :status
          AND u.id <> :excludeId
          AND u.usernameLower LIKE CONCAT(:term, '%')
        ORDER BY u.username ASC
    """)
    List<UserModel> searchByUsername(
            @Param("term")      String     term,
            @Param("excludeId") Long       excludeId,
            @Param("status")    UserStatus status,
            Pageable            pageable
    );

    /**
     * Borra cuentas que quedaron en PENDING_VERIFICATION (nunca verificaron el
     * OTP) y ya pasaron el umbral de expiración. Sin esto, esas filas quedan
     * huérfanas para siempre y además bloquean re-registrar el mismo
     * email/username/dni (USER_EMAIL_EXISTS, etc.) sin ninguna salida.
     */
    @Modifying
    @Query("""
        DELETE FROM UserModel u
        WHERE u.status = com.example.backend.common.enums.UserStatus.PENDING_VERIFICATION
          AND u.createdAt < :cutoff
        """)
    int deleteStalePendingVerification(@Param("cutoff") LocalDateTime cutoff);

    /**
     * Listado para el panel de administración con filtros opcionales
     * (cualquiera de los tres puede venir null → sin filtrar por ese campo).
     */
    @Query("""
        SELECT u FROM UserModel u
        WHERE (:status IS NULL OR u.status = :status)
          AND (:role IS NULL OR u.role = :role)
          AND (:search IS NULL OR u.usernameLower LIKE CONCAT('%', CAST(:search AS string), '%')
                                OR LOWER(u.email) LIKE CONCAT('%', CAST(:search AS string), '%'))
        ORDER BY u.createdAt DESC
        """)
    Page<UserModel> searchForAdmin(
            @Param("status") UserStatus status,
            @Param("role")   Role       role,
            @Param("search") String     search,
            Pageable pageable
    );
}
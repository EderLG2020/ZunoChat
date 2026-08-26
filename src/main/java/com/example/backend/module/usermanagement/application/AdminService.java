package com.example.backend.module.usermanagement.application;

import com.example.backend.common.email.EmailService;
import com.example.backend.common.enums.Role;
import com.example.backend.common.enums.UserStatus;
import com.example.backend.common.exception.AppException;
import com.example.backend.common.response.AppCode;
import com.example.backend.common.security.IUserStatusCache;
import com.example.backend.module.usermanagement.domain.UserModel;
import com.example.backend.module.usermanagement.dto.AdminUserResponse;
import com.example.backend.module.usermanagement.persistence.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Lógica real detrás de AdminController (antes eran endpoints de ejemplo que
 * no tocaban la base de datos).
 *
 * Reglas de rango, para que un ADMIN no pueda escalar privilegios:
 *  - Nadie puede moderarse a sí mismo.
 *  - Nadie puede moderar a un SUPERADMIN (ni otro SUPERADMIN).
 *  - Un ADMIN solo puede moderar cuentas con rol USER.
 */
@Service
@RequiredArgsConstructor
public class AdminService {

    private final UserRepository userRepository;
    private final EmailService emailService;
    private final IUserStatusCache userStatusCache;

    @Transactional(readOnly = true)
    public Page<AdminUserResponse> listUsers(UserStatus status, Role role, String search, int page, int size) {
        String normalizedSearch = (search == null || search.isBlank()) ? null : search.trim().toLowerCase();
        return userRepository
                .searchForAdmin(status, role, normalizedSearch, PageRequest.of(page, size))
                .map(AdminUserResponse::from);
    }

    @Transactional
    public void ban(Long actorId, Long targetId, String reason) {
        UserModel actor = loadUser(actorId);
        UserModel target = loadUser(targetId);
        assertCanModerate(actor, target);

        target.setStatus(UserStatus.BANNED);
        target.setUpdatedBy(actorId);
        userRepository.save(target);
        userStatusCache.invalidate(target.getUsername());

        emailService.sendAccountStatusChanged(target.getEmail(), target.getUsername(), "BANNED", reason);
    }

    @Transactional
    public void activate(Long actorId, Long targetId) {
        UserModel actor = loadUser(actorId);
        UserModel target = loadUser(targetId);
        assertCanModerate(actor, target);

        target.setStatus(UserStatus.ACTIVE);
        target.setUpdatedBy(actorId);
        userRepository.save(target);
        userStatusCache.invalidate(target.getUsername());

        emailService.sendAccountStatusChanged(target.getEmail(), target.getUsername(), "ACTIVE", null);
    }

    @Transactional
    public void delete(Long actorId, Long targetId) {
        UserModel actor = loadUser(actorId);
        UserModel target = loadUser(targetId);
        assertCanModerate(actor, target);

        // Soft delete — conserva el historial de mensajes/conversaciones
        // (senderId/receiverId siguen apuntando a un id válido).
        target.setStatus(UserStatus.DELETED);
        target.setUpdatedBy(actorId);
        userRepository.save(target);
        userStatusCache.invalidate(target.getUsername());
    }

    @Transactional
    public void assignRole(Long actorId, Long targetId, String roleStr) {
        UserModel actor = loadUser(actorId);
        UserModel target = loadUser(targetId);
        assertCanModerate(actor, target);

        Role newRole;
        try {
            newRole = Role.valueOf(roleStr.trim().toUpperCase());
        } catch (IllegalArgumentException e) {
            throw new AppException(AppCode.USER_ROLE_INVALID);
        }
        if (newRole == Role.SUPERADMIN)
            throw new AppException(AppCode.USER_INSUFFICIENT_RANK, "El rol SUPERADMIN no se puede asignar por API");

        target.setRole(newRole);
        target.setUpdatedBy(actorId);
        userRepository.save(target);
        userStatusCache.invalidate(target.getUsername()); // el JWT viejo trae permisos desactualizados igual, pero esto cubre el status
    }

    // ─── Helpers ─────────────────────────────────────────────────────────────

    private UserModel loadUser(Long id) {
        return userRepository.findById(id).orElseThrow(() -> new AppException(AppCode.USER_NOT_FOUND));
    }

    private void assertCanModerate(UserModel actor, UserModel target) {
        if (actor.getId().equals(target.getId()))
            throw new AppException(AppCode.USER_SELF_ACTION);
        if (target.getRole() == Role.SUPERADMIN)
            throw new AppException(AppCode.USER_INSUFFICIENT_RANK);
        if (actor.getRole() == Role.ADMIN && target.getRole() != Role.USER)
            throw new AppException(AppCode.USER_INSUFFICIENT_RANK);
    }
}

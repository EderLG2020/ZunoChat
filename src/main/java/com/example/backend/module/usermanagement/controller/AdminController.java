package com.example.backend.module.usermanagement.controller;

import com.example.backend.common.enums.Role;
import com.example.backend.common.enums.UserStatus;
import com.example.backend.common.response.ApiResponse;
import com.example.backend.common.response.AppCode;
import com.example.backend.common.util.JwtUtil;
import com.example.backend.module.usermanagement.application.AdminService;
import com.example.backend.module.usermanagement.dto.AdminUserResponse;
import com.example.backend.module.usermanagement.dto.AssignRoleRequest;
import com.example.backend.module.usermanagement.dto.ModerateUserRequest;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

/**
 * Panel de administración. El token JWT trae el claim "permissions"
 * (ver Role#getPermissions), que JwtFilter registra como GrantedAuthority:
 *
 *   @PreAuthorize("hasAuthority('dashboard:editar')")
 *   @PreAuthorize("hasRole('SUPERADMIN')")
 *
 * Reglas de rango (ver AdminService#assertCanModerate): nadie se modera a sí
 * mismo, nadie modera a un SUPERADMIN, y un ADMIN solo modera cuentas USER.
 */
@RestController
@RequestMapping("/api/admin")
@RequiredArgsConstructor
@Validated
public class AdminController {

    private final AdminService adminService;

    @GetMapping("/dashboard")
    @PreAuthorize("hasAuthority('dashboard:ver')")
    public ResponseEntity<ApiResponse<String>> verDashboard() {
        return ResponseEntity.ok(
                ApiResponse.ok(AppCode.OK_GENERIC, "Dashboard visible para ADMIN y SUPERADMIN", null));
    }

    @GetMapping("/usuarios")
    @PreAuthorize("hasAuthority('usuarios:ver')")
    public ResponseEntity<ApiResponse<Page<AdminUserResponse>>> listarUsuarios(
            @RequestParam(required = false) UserStatus status,
            @RequestParam(required = false) Role role,
            @RequestParam(required = false) String search,
            @RequestParam(defaultValue = "0") @Min(0) int page,
            @RequestParam(defaultValue = "20") @Min(1) @Max(100) int size
    ) {
        Page<AdminUserResponse> result = adminService.listUsers(status, role, search, page, size);
        return ResponseEntity.ok(ApiResponse.ok(AppCode.OK_USERS_LISTED, result));
    }

    @PatchMapping("/usuarios/{id}/ban")
    @PreAuthorize("hasAuthority('usuarios:bannear')")
    public ResponseEntity<ApiResponse<Void>> banearUsuario(@PathVariable Long id, @RequestBody(required = false) ModerateUserRequest body) {
        adminService.ban(JwtUtil.currentUserId(), id, body != null ? body.reason() : null);
        return ResponseEntity.ok(ApiResponse.ok(AppCode.OK_USER_BANNED));
    }

    @PatchMapping("/usuarios/{id}/activar")
    @PreAuthorize("hasAuthority('usuarios:activar')")
    public ResponseEntity<ApiResponse<Void>> activarUsuario(@PathVariable Long id) {
        adminService.activate(JwtUtil.currentUserId(), id);
        return ResponseEntity.ok(ApiResponse.ok(AppCode.OK_USER_ACTIVATED));
    }

    @DeleteMapping("/usuarios/{id}")
    @PreAuthorize("hasAuthority('usuarios:eliminar')")
    public ResponseEntity<ApiResponse<Void>> eliminarUsuario(@PathVariable Long id) {
        adminService.delete(JwtUtil.currentUserId(), id);
        return ResponseEntity.ok(ApiResponse.ok(AppCode.OK_USER_DELETED));
    }

    @PatchMapping("/usuarios/{id}/rol")
    @PreAuthorize("hasAuthority('roles:asignar')")
    public ResponseEntity<ApiResponse<Void>> asignarRol(@PathVariable Long id, @Valid @RequestBody AssignRoleRequest req) {
        adminService.assignRole(JwtUtil.currentUserId(), id, req.role());
        return ResponseEntity.ok(ApiResponse.ok(AppCode.OK_ROLE_ASSIGNED));
    }
}

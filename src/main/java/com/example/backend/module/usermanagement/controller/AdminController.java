package com.example.backend.module.usermanagement.controller;

import com.example.backend.common.response.ApiResponse;
import com.example.backend.common.response.AppCode;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

/**
 * Ejemplo de uso de permisos granulares con @PreAuthorize.
 *
 * El token JWT contiene el claim "permissions": ["dashboard:ver", "dashboard:editar", ...]
 * El JwtFilter los registra como GrantedAuthority, permitiendo:
 *
 *   @PreAuthorize("hasAuthority('dashboard:editar')")
 *   @PreAuthorize("hasRole('SUPERADMIN')")
 *   @PreAuthorize("hasAnyAuthority('usuarios:bannear', 'usuarios:eliminar')")
 */
@RestController
@RequestMapping("/api/admin")
public class AdminController {

    @GetMapping("/dashboard")
    @PreAuthorize("hasAuthority('dashboard:ver')")
    public ResponseEntity<ApiResponse<String>> verDashboard() {
        return ResponseEntity.ok(
                ApiResponse.ok(AppCode.OK_GENERIC, "Dashboard visible para ADMIN y SUPERADMIN", null));
    }

    @PutMapping("/dashboard")
    @PreAuthorize("hasAuthority('dashboard:editar')")
    public ResponseEntity<ApiResponse<String>> editarDashboard() {
        return ResponseEntity.ok(
                ApiResponse.ok(AppCode.OK_GENERIC, "Dashboard editable para ADMIN y SUPERADMIN", null));
    }

    @GetMapping("/usuarios")
    @PreAuthorize("hasAuthority('usuarios:ver')")
    public ResponseEntity<ApiResponse<String>> listarUsuarios() {
        return ResponseEntity.ok(
                ApiResponse.ok(AppCode.OK_USERS_LISTED, "Lista de usuarios (ADMIN / SUPERADMIN)", null));
    }

    @PatchMapping("/usuarios/{id}/ban")
    @PreAuthorize("hasAuthority('usuarios:bannear')")
    public ResponseEntity<ApiResponse<Void>> banearUsuario(@PathVariable Long id) {
        return ResponseEntity.ok(ApiResponse.ok(AppCode.OK_USER_BANNED));
    }

    @DeleteMapping("/usuarios/{id}")
    @PreAuthorize("hasAuthority('usuarios:eliminar')")
    public ResponseEntity<ApiResponse<Void>> eliminarUsuario(@PathVariable Long id) {
        return ResponseEntity.ok(ApiResponse.ok(AppCode.OK_USER_DELETED));
    }

    @PatchMapping("/usuarios/{id}/rol")
    @PreAuthorize("hasAuthority('roles:asignar')")
    public ResponseEntity<ApiResponse<Void>> asignarRol(@PathVariable Long id) {
        return ResponseEntity.ok(ApiResponse.ok(AppCode.OK_ROLE_ASSIGNED));
    }
}
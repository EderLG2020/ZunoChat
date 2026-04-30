package com.example.backend.module.usermanagement.controller;

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
@RequestMapping("/admin")
public class AdminController {

    @GetMapping("/dashboard")
    @PreAuthorize("hasAuthority('dashboard:ver')")
    public String verDashboard() {
        return "Dashboard visible para ADMIN y SUPERADMIN";
    }

    @PutMapping("/dashboard")
    @PreAuthorize("hasAuthority('dashboard:editar')")
    public String editarDashboard() {
        return "Dashboard editable para ADMIN y SUPERADMIN";
    }

    @GetMapping("/usuarios")
    @PreAuthorize("hasAuthority('usuarios:ver')")
    public String listarUsuarios() {
        return "Lista de usuarios (ADMIN / SUPERADMIN)";
    }

    @PatchMapping("/usuarios/{id}/ban")
    @PreAuthorize("hasAuthority('usuarios:bannear')")
    public String banearUsuario(@PathVariable Long id) {
        return "Usuario " + id + " baneado";
    }

    @DeleteMapping("/usuarios/{id}")
    @PreAuthorize("hasAuthority('usuarios:eliminar')")
    public String eliminarUsuario(@PathVariable Long id) {
        return "Usuario " + id + " eliminado (solo SUPERADMIN)";
    }

    @PatchMapping("/usuarios/{id}/rol")
    @PreAuthorize("hasAuthority('roles:asignar')")
    public String asignarRol(@PathVariable Long id) {
        return "Rol asignado (solo SUPERADMIN)";
    }
}

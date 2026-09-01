package com.example.backend.common.enums;

/**
 * Rol de un miembro dentro de una conversación GROUP (independiente del Role
 * de sistema — un USER normal puede ser OWNER de sus propios grupos).
 *
 * Jerarquía: OWNER > ADMIN > MEMBER.
 *  - OWNER: único por grupo (el creador, salvo transferencia). Puede todo:
 *    agregar/quitar miembros y ADMIN, promover/degradar, transferir la
 *    propiedad. No puede "salir" del grupo sin transferir antes.
 *  - ADMIN: puede agregar miembros y quitar/degradar MEMBER, pero no puede
 *    tocar a otro ADMIN ni al OWNER.
 *  - MEMBER: solo puede salir del grupo.
 */
public enum GroupRole {
    OWNER,
    ADMIN,
    MEMBER
}

package com.example.backend.module.usermanagement.persistence;

import com.example.backend.common.enums.Role;
import com.example.backend.common.enums.UserStatus;
import com.example.backend.module.usermanagement.domain.UserModel;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface UserRepository extends JpaRepository<UserModel, Long> {

    long countByRole(Role role);
    Optional<UserModel> findByUsername(String username);
    Optional<UserModel> findByEmail(String email);
    Optional<UserModel> findByUsernameOrEmail(String username, String email);
    boolean existsByUsername(String username);
    boolean existsByEmail(String email);
    boolean existsByDni(String dni);

    @Query("""
        SELECT u FROM UserModel u
        WHERE u.status = :status
          AND u.id <> :excludeId
          AND LOWER(u.username) LIKE LOWER(CONCAT('%', :term, '%'))
        ORDER BY u.username ASC
    """)
    List<UserModel> searchByUsername(
            @Param("term")      String     term,
            @Param("excludeId") Long       excludeId,
            @Param("status")    UserStatus status,
            Pageable            pageable
    );
}
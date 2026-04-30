package com.example.backend.module.usermanagement.persistence;

import com.example.backend.module.usermanagement.domain.UserModel;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface UserRepository extends JpaRepository<UserModel, Long> {

    Optional<UserModel> findByUsername(String username);

    Optional<UserModel> findByEmail(String email);

    /** Para login con username o email */
    Optional<UserModel> findByUsernameOrEmail(String username, String email);

    boolean existsByUsername(String username);

    boolean existsByEmail(String email);

    boolean existsByDni(String dni);
}

package com.example.backend.common.config;

import com.example.backend.common.enums.Role;
import com.example.backend.common.enums.UserStatus;
import com.example.backend.module.usermanagement.domain.UserModel;
import com.example.backend.module.usermanagement.persistence.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import net.datafaker.Faker;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

import java.util.Locale;
import java.util.Random;

@Slf4j
@Component
@RequiredArgsConstructor
public class DataSeeder implements ApplicationRunner {

    private final UserRepository  userRepository;
    private final PasswordEncoder passwordEncoder;

    private static final int TOTAL_USERS = 200;

    private final Faker  faker  = new Faker(new Locale("es"));
    private final Random random = new Random();

    @Override
    public void run(ApplicationArguments args) {
        log.info("[DataSeeder] Iniciando...");

        seedSuperAdmin();
        seedAdmin("admin1");
        seedAdmin("admin2");
        seedFakeUsers(TOTAL_USERS);

        log.info("[DataSeeder] Completado.");
    }

    // ─── Super admin ─────────────────────────────────────────────────────────

    private void seedSuperAdmin() {
        if (userRepository.existsByUsername("superadmin")) {
            log.info("[DataSeeder] superadmin ya existe, omitiendo.");
            return;
        }
        userRepository.save(UserModel.builder()
                .username("superadmin")
                .email("superadmin@zunochat.com")
                .dni("00000000")
                .password(passwordEncoder.encode("Super@2024!"))
                .role(Role.SUPERADMIN)
                .status(UserStatus.ACTIVE)
                .build());
        log.info("[DataSeeder] superadmin creado.");
    }

    // ─── Admins ───────────────────────────────────────────────────────────────

    private void seedAdmin(String username) {
        if (userRepository.existsByUsername(username)) {
            log.info("[DataSeeder] {} ya existe, omitiendo.", username);
            return;
        }
        userRepository.save(UserModel.builder()
                .username(username)
                .email(username + "@zunochat.com")
                .dni(generateDni())
                .password(passwordEncoder.encode("Admin@2024!"))
                .role(Role.ADMIN)
                .status(UserStatus.ACTIVE)
                .build());
        log.info("[DataSeeder] {} creado.", username);
    }

    // ─── Usuarios fake ────────────────────────────────────────────────────────

    /**
     * Solo crea usuarios si la tabla todavía no tiene suficientes usuarios con rol USER.
     * En reinicios posteriores, el conteo ya alcanza TOTAL_USERS y no hace nada.
     */
    private void seedFakeUsers(int total) {
        long existing = userRepository.countByRole(Role.USER);

        if (existing >= total) {
            log.info("[DataSeeder] Ya existen {} usuarios USER, omitiendo seed.", existing);
            return;
        }

        int needed  = (int) (total - existing);
        int created = 0;

        log.info("[DataSeeder] Creando {} usuarios fake (ya existen {})...", needed, existing);

        while (created < needed) {
            try {
                String username = generateUsername();
                userRepository.save(UserModel.builder()
                        .username(username)
                        .email(username + "@mail.com")
                        .dni(generateDni())
                        .password(passwordEncoder.encode("User@2024!"))
                        .role(Role.USER)
                        .status(UserStatus.ACTIVE)
                        .build());
                created++;
            } catch (Exception e) {
                // colisión de username/email/dni único — se regenera y reintenta
                log.debug("[DataSeeder] Colisión, regenerando...");
            }
        }

        log.info("[DataSeeder] {} usuarios fake creados.", created);
    }

    // ─── Helpers ─────────────────────────────────────────────────────────────

    private String generateUsername() {
        String firstName = faker.name().firstName().toLowerCase().replaceAll("[^a-z]", "");
        return firstName + "_" + random.nextInt(9999);
    }

    private String generateDni() {
        return String.valueOf(10_000_000 + random.nextInt(89_999_999));
    }
}
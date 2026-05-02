package com.example.backend.common.config;

import com.example.backend.common.enums.Role;
import com.example.backend.common.enums.UserStatus;
import com.example.backend.module.usermanagement.domain.UserModel;
import com.example.backend.module.usermanagement.persistence.UserRepository;
import net.datafaker.Faker;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
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

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    private static final int TOTAL_USERS = 200;

    private final Faker faker = new Faker(new Locale("es"));
    private final Random random = new Random();

    @Override
    public void run(ApplicationArguments args) {

        log.info("[DataSeeder] Iniciando carga de datos...");

        seedSuperAdmin();
        seedAdmin("admin1");
        seedAdmin("admin2");

        seedFakeUsers(TOTAL_USERS);

        log.info("[DataSeeder] Seeder completado.");
    }

    private void seedSuperAdmin() {

        if (userRepository.existsByUsername("superadmin")) {
            log.info("[DataSeeder] SUPERADMIN ya existe.");
            return;
        }

        UserModel user = UserModel.builder()
                .username("superadmin")
                .email("superadmin@zunochat.com")
                .dni("00000000")
                .password(passwordEncoder.encode("Super@2024!"))
                .role(Role.SUPERADMIN)
                .status(UserStatus.ACTIVE)
                .build();

        userRepository.save(user);

        log.info("[DataSeeder] SUPERADMIN creado.");
    }

    private void seedAdmin(String username) {

        if (userRepository.existsByUsername(username)) {
            log.info("[DataSeeder] {} ya existe.", username);
            return;
        }

        UserModel user = UserModel.builder()
                .username(username)
                .email(username + "@zunochat.com")
                .dni(generateDni())
                .password(passwordEncoder.encode("Admin@2024!"))
                .role(Role.ADMIN)
                .status(UserStatus.ACTIVE)
                .build();

        userRepository.save(user);

        log.info("[DataSeeder] {} creado.", username);
    }

    private void seedFakeUsers(int total) {

        int created = 0;

        while (created < total) {

            try {

                String username = generateUsername();

                UserModel user = UserModel.builder()
                        .username(username)
                        .email(username + "@mail.com")
                        .dni(generateDni())
                        .password(passwordEncoder.encode("User@2024!"))
                        .role(Role.USER)
                        .status(UserStatus.ACTIVE)
                        .build();

                userRepository.save(user);

                created++;

            } catch (Exception e) {

                // colisión de username/email único
                log.debug("[DataSeeder] Colisión detectada, regenerando...");
            }
        }

        log.info("[DataSeeder] {} usuarios fake creados.", created);
    }

    private String generateUsername() {

        String firstName = faker.name().firstName().toLowerCase();
        int number = random.nextInt(9999);

        return firstName + "_" + number;
    }

    private String generateDni() {

        return String.valueOf(
                10000000 + random.nextInt(89999999)
        );
    }
}
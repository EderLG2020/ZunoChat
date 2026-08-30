package com.example.backend.common.config;

import com.example.backend.common.enums.ConversationStatus;
import com.example.backend.common.enums.MessageStatus;
import com.example.backend.common.enums.MessageType;
import com.example.backend.common.enums.Role;
import com.example.backend.common.enums.UserStatus;
import com.example.backend.module.messagemanagement.domain.ConversationModel;
import com.example.backend.module.messagemanagement.domain.MessageModel;
import com.example.backend.module.messagemanagement.persistence.ConversationRepository;
import com.example.backend.module.messagemanagement.persistence.MessageRepository;
import com.example.backend.module.usermanagement.domain.UserModel;
import com.example.backend.module.usermanagement.persistence.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import net.datafaker.Faker;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.context.annotation.Profile;
import org.springframework.data.domain.PageRequest;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Random;
import java.util.Set;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Solo corre con spring.profiles.active=dev. Las contraseñas de
 * superadmin/admin1/admin2/usuarios fake están hardcodeadas en este archivo
 * (Super@2024! / Admin@2024! / User@2024!) — cualquiera que lea el repo las
 * conoce, así que @Profile("dev") evita que esto se ejecute por accidente
 * (o por copiar application.properties sin cambiar el profile) contra una
 * base de datos compartida o de producción.
 */
@Slf4j
@Component
@Profile("dev")
@RequiredArgsConstructor
public class DataSeeder implements ApplicationRunner {

    private final UserRepository         userRepository;
    private final ConversationRepository conversationRepository;
    private final MessageRepository      messageRepository;
    private final PasswordEncoder        passwordEncoder;

    private static final int TOTAL_USERS         = 200;
    private static final int TOTAL_CONVERSATIONS = 40;
    private static final int MIN_MESSAGES        = 5;
    private static final int MAX_MESSAGES        = 20;

    /**
     * pravatar.cc ofrece 70 fotos "stock" fijas servidas por id (?img=1..70),
     * sin necesidad de API key ni de descargar/almacenar nada nosotros: el
     * frontend carga la URL directo. Se cicla con módulo para repartir las
     * 70 fotos entre los usuarios seedeados.
     */
    private static final int    PRAVATAR_COUNT = 70;
    private final AtomicInteger avatarCounter  = new AtomicInteger(0);

    private static final String[] SAMPLE_MESSAGES = {
            "Hola! ¿Cómo estás?", "Todo bien por acá, ¿y vos?", "¿Viste el partido de ayer?",
            "Sí! Estuvo increíble el final", "¿A qué hora nos juntamos?", "Puedo a las 5, ¿te sirve?",
            "Dale, ahí nos vemos", "Che, ¿me pasás el archivo cuando puedas?", "Ya te lo mando",
            "Gracias!!", "De nada :)", "¿Ya almorzaste?", "No, estoy re ocupado hoy",
            "Aviso cuando salgas de la reunión", "Perfecto, hablamos luego",
            "Jajaja no lo puedo creer", "En serio? contame más", "Después te cuento con calma",
            "¿Vas a venir el finde?", "Todavía no sé, te confirmo", "Buenísimo, gracias por avisar",
            "¿Cómo va el proyecto?", "Avanzando de a poco, casi lo termino",
            "Cualquier cosa me escribís", "Anotado, gracias", "Buen día!",
            "Buenas noches, que descanses", "¿Pudiste revisar lo que te mandé?",
            "Sí, quedó todo bien", "Dale un check cuando puedas", "Ya casi llego",
            "Te espero en la entrada", "¿Nos vemos mañana?", "Claro, ahí estaré",
            "Mil gracias por la ayuda", "No hay problema, para eso estamos",
            "¿Alguna novedad?", "Todavía nada nuevo", "Recién me entero, gracias por avisar"
    };

    private final Faker  faker  = new Faker(new Locale("es"));
    private final Random random = new Random();

    @Override
    public void run(ApplicationArguments args) {
        log.info("[DataSeeder] Iniciando...");

        seedSuperAdmin();
        seedAdmin("admin1");
        seedAdmin("admin2");
        seedFakeUsers(TOTAL_USERS);
        seedConversationsAndMessages(TOTAL_CONVERSATIONS);

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
                .avatar(nextAvatarUrl())
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
                .avatar(nextAvatarUrl())
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
                        .avatar(nextAvatarUrl())
                        .build());
                created++;
            } catch (Exception e) {
                // colisión de username/email/dni único — se regenera y reintenta
                log.debug("[DataSeeder] Colisión, regenerando...");
            }
        }

        log.info("[DataSeeder] {} usuarios fake creados.", created);
    }

    // ─── Conversaciones + mensajes ──────────────────────────────────────────────

    /**
     * Solo corre si todavía no hay ninguna conversación en la base — a
     * diferencia de seedFakeUsers, no intenta "completar hasta N" en
     * reinicios posteriores: una vez que hay datos de conversación reales
     * (creados por el seed o por uso real de la app) no los toca.
     */
    private void seedConversationsAndMessages(int totalConversations) {
        if (conversationRepository.count() > 0) {
            log.info("[DataSeeder] Ya existen conversaciones, omitiendo seed.");
            return;
        }

        List<UserModel> pool = userRepository.findByRole(Role.USER, PageRequest.of(0, 150));
        if (pool.size() < 2) {
            log.info("[DataSeeder] No hay suficientes usuarios USER para crear conversaciones.");
            return;
        }

        Set<String> usedPairs = new HashSet<>();
        int created  = 0;
        int attempts = 0;
        int maxAttempts = totalConversations * 5;

        while (created < totalConversations && attempts < maxAttempts) {
            attempts++;

            UserModel a = pool.get(random.nextInt(pool.size()));
            UserModel b = pool.get(random.nextInt(pool.size()));
            if (a.getId().equals(b.getId())) continue;

            Long u1 = Math.min(a.getId(), b.getId());
            Long u2 = Math.max(a.getId(), b.getId());
            if (!usedPairs.add(u1 + "-" + u2)) continue;

            UserModel user1 = a.getId().equals(u1) ? a : b;
            UserModel user2 = a.getId().equals(u1) ? b : a;

            seedConversation(user1, user2);
            created++;
        }

        log.info("[DataSeeder] {} conversaciones creadas con mensajes.", created);
    }

    private void seedConversation(UserModel user1, UserModel user2) {
        ConversationModel conv = conversationRepository.save(ConversationModel.builder()
                .user1Id(user1.getId())
                .user2Id(user2.getId())
                .user1Username(user1.getUsername())
                .user2Username(user2.getUsername())
                .user1Avatar(user1.getAvatar())
                .user2Avatar(user2.getAvatar())
                .status(ConversationStatus.OFFLINE)
                .build());

        int messageCount = MIN_MESSAGES + random.nextInt(MAX_MESSAGES - MIN_MESSAGES + 1);

        // Reparte los mensajes en los últimos ~20 días, avanzando en el tiempo
        // para que el orden de inserción (id ascendente) coincida con el
        // orden cronológico simulado, igual que en una conversación real.
        LocalDateTime timestamp = LocalDateTime.now()
                .minusDays(1 + random.nextInt(20))
                .minusHours(random.nextInt(24));

        MessageModel last = null;

        for (int i = 0; i < messageCount; i++) {
            boolean   fromUser1 = random.nextBoolean();
            UserModel sender    = fromUser1 ? user1 : user2;
            UserModel receiver  = fromUser1 ? user2 : user1;

            timestamp = timestamp.plusMinutes(1 + random.nextInt(180));
            if (timestamp.isAfter(LocalDateTime.now())) timestamp = LocalDateTime.now();

            String text = randomChatMessage();
            boolean isLast = i == messageCount - 1;

            MessageModel msg = messageRepository.save(MessageModel.builder()
                    .conversationId(conv.getId())
                    .senderId(sender.getId())
                    .receiverId(receiver.getId())
                    .type(MessageType.TEXT)
                    .textContent(text)
                    .status(isLast ? MessageStatus.SENT : MessageStatus.READ)
                    .readAt(isLast ? null : timestamp)
                    .build());

            // sentAt es @CreationTimestamp (se autoasigna al insertar) — este
            // UPDATE aparte lo pisa para simular un historial con fechas
            // repartidas en el pasado en vez de "todo enviado ahora mismo".
            messageRepository.backdateSentAt(msg.getId(), timestamp);
            msg.setSentAt(timestamp);
            last = msg;
        }

        if (last == null) return;

        String preview = last.getTextContent().length() > 50
                ? last.getTextContent().substring(0, 50)
                : last.getTextContent();

        boolean lastToUser1 = last.getReceiverId().equals(user1.getId());

        conv.setLastMessagePreview(preview);
        conv.setLastMessageSenderId(last.getSenderId());
        conv.setLastMessageAt(last.getSentAt());
        conv.setUnreadCountUser1(lastToUser1 ? 1 + random.nextInt(3) : 0);
        conv.setUnreadCountUser2(lastToUser1 ? 0 : 1 + random.nextInt(3));
        conversationRepository.save(conv);
    }

    private String randomChatMessage() {
        return SAMPLE_MESSAGES[random.nextInt(SAMPLE_MESSAGES.length)];
    }

    // ─── Helpers ─────────────────────────────────────────────────────────────

    private String generateUsername() {
        String firstName = faker.name().firstName().toLowerCase().replaceAll("[^a-z]", "");
        return firstName + "_" + random.nextInt(9999);
    }

    private String generateDni() {
        return String.valueOf(10_000_000 + random.nextInt(89_999_999));
    }

    private String nextAvatarUrl() {
        int img = (avatarCounter.getAndIncrement() % PRAVATAR_COUNT) + 1;
        return "https://i.pravatar.cc/300?img=" + img;
    }
}

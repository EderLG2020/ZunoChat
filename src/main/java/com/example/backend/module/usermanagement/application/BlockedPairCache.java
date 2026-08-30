package com.example.backend.module.usermanagement.application;

import com.example.backend.module.usermanagement.persistence.BlockedUserRepository;
import com.google.common.cache.Cache;
import com.google.common.cache.CacheBuilder;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;

/**
 * Cachea "¿userA y userB tienen algún bloqueo entre sí?" — MessageService lo
 * consulta en CADA envío de mensaje (existsBetween), y para la gran mayoría
 * de pares de usuarios el resultado es casi siempre el mismo (nadie bloqueó
 * a nadie). Sin esto, cada mensaje pagaba un round-trip extra a BD solo para
 * confirmar lo mismo una y otra vez.
 *
 * TTL de 5 min como red de seguridad + invalidate() explícito en
 * BlockService.block()/unblock() para que el efecto sea inmediato en el
 * caso normal (una sola instancia). En multi-instancia, invalidate() solo
 * limpia el cache de la instancia que atendió el bloqueo — las demás quedan
 * con el valor viejo hasta por 5 min (mismo trade-off que UserStatusCache,
 * pero sin variante Redis: el impacto de un falso negativo acá es mucho más
 * bajo — en el peor caso, un mensaje se entrega mientras el bloqueo termina
 * de propagarse, no una brecha de seguridad como la de un baneo).
 */
@Slf4j
@Component
public class BlockedPairCache {

    private final BlockedUserRepository blockedUserRepository;

    private final Cache<String, Boolean> cache = CacheBuilder.newBuilder()
            .expireAfterWrite(5, TimeUnit.MINUTES)
            .maximumSize(50_000)
            .build();

    public BlockedPairCache(BlockedUserRepository blockedUserRepository) {
        this.blockedUserRepository = blockedUserRepository;
    }

    public boolean isBlocked(Long userA, Long userB) {
        String key = key(userA, userB);
        try {
            return cache.get(key, () -> blockedUserRepository.existsBetween(userA, userB));
        } catch (ExecutionException e) {
            log.warn("[BlockedPairCache] No se pudo resolver el bloqueo entre {} y {}: {}", userA, userB, e.getMessage());
            return blockedUserRepository.existsBetween(userA, userB);
        }
    }

    /** Usar al bloquear/desbloquear para que el par no quede con el valor viejo hasta que expire el TTL. */
    public void invalidate(Long userA, Long userB) {
        cache.invalidate(key(userA, userB));
    }

    private String key(Long userA, Long userB) {
        long min = Math.min(userA, userB);
        long max = Math.max(userA, userB);
        return min + ":" + max;
    }
}

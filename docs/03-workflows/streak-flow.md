# Flujo de Racha (Streak)

Extraído de `04-architecture/components.md` (módulo `streak`). Ver también `04-architecture/decisions/ADR-009-streak-mutual-optin.md` y `02-domain/business-rules.md`.

```
[MessageService.sendMessage()] ── tras persistir el mensaje, solo si !isGroup ──┐
                                                                                 │
                                                                                 ▼
                                                    [StreakService.recordInteraction(convId, senderId, receiverId)]
                                                                     │  @Transactional(REQUIRES_NEW) — nunca propaga
                                                                     │  una excepción hacia el envío del mensaje
                                                                     ▼
                                        [StreakRepository.findByConversationIdForUpdate] (PESSIMISTIC_WRITE)
                                                                     │
                                                                     ▼
                                                          [StreakCalculator.apply(...)]
                                        ├─ ambos ya escribieron hoy y ya estaba contado  → NONE
                                        ├─ último día mutuo = ayer                        → INCREMENT (currentCount++)
                                        ├─ gap > 1 día, o primera vez                     → RESET (currentCount = 1)
                                        └─ solo uno de los dos escribió hoy               → NONE (solo guarda su fecha)
                                                                     │
                                                                     ▼
                                        si INCREMENT/RESET → [StreakEventPublisher] → /topic/streak.{conversationId}

[Cliente] ── Configuración: activar racha ──┐
        └─► PATCH /api/streaks/{id} {enabled:true}
                └─► [StreakService.requestActivation]
                        ├─ sin solicitud previa → requestStatus=PENDING, publica REQUEST_SENT
                        └─ el otro ya la pidió  → auto-acepta, enabled=true, publica REQUEST_ACCEPTED

[Cliente] ── responde solicitud ──┐
        └─► POST /api/streaks/{id}/respond {accept}
                └─► [StreakService.respondToActivation] → ACCEPTED/DECLINED

[StreakExpiryScheduler] ── @Scheduled(cron "0 5 0 * * *", zone UTC) ──┐
        ├─ ACTIVE con último día mutuo = ayer     → AT_RISK
        └─ ACTIVE/AT_RISK sin actividad ni ayer   → BROKEN (currentCount=0, longestCount se conserva)
```

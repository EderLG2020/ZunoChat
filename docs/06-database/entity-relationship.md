# Entity-Relationship

TODO: no existe un diagrama ER. Las relaciones entre entidades están descritas en tablas dentro de `database-design.md`:

- `conversations.user1_id` / `user2_id` → `users.id`
- `messages.conversation_id` → `conversations.id`
- `messages.sender_id` / `receiver_id` → `users.id`
- `streaks.conversation_id` → `conversations.id` (única por conversación `DIRECT` con racha solicitada)

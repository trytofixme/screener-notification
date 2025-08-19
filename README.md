# Screener Notification Service

Сервис слушает события из Kafka и отправляет уведомления в Telegram.

## Создание Telegram-бота

1. В Telegram найдите пользователя [@BotFather](https://t.me/BotFather).
2. Отправьте команду `/newbot` и следуйте инструкциям по созданию бота.
3. BotFather выдаст *token*, сохраните его — он понадобится сервису.
4. Добавьте бота в нужный чат или канал и получите `chat_id`.
   Проще всего отправить сообщение боту и вызвать `https://api.telegram.org/bot<token>/getUpdates`.
5. Укажите токен и идентификатор чата в `application.yaml`:

```yaml
telegram:
  bot-token: YOUR_BOT_TOKEN
  chat-id: YOUR_CHAT_ID
```

После запуска сервиса бот будет отправлять сформированные уведомления в указанный чат.
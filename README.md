# Сервис справочников

Spring Boot 3 (Spring MVC) + EclipseLink + Vaadin, Java 21, PostgreSQL 16.

## Запуск базы данных

Требуется Docker и Docker Compose.

1. (Опционально) задайте параметры подключения:
   ```bash
   cp .env.example .env   # и отредактируйте значения
   ```
2. Поднимите контейнер:
   ```bash
   docker compose up -d --build
   ```
3. Проверьте состояние (должен быть `healthy`):
   ```bash
   docker compose ps
   ```
4. Подключение к БД вручную:
   ```bash
   docker compose exec postgres psql -U postgres -d reference_data
   ```

Остановка:
```bash
docker compose down        # данные сохраняются в томе postgres_data
docker compose down -v     # удалить вместе с данными
```

Скрипты из `docker/postgres/init/` выполняются только при первом создании тома.
Если нужно применить их заново — выполните `docker compose down -v`.

## Запуск приложения

Параметры подключения читаются из переменных окружения `DB_HOST`, `DB_PORT`, `DB_NAME`,
`DB_USERNAME`, `DB_PASSWORD` (значения по умолчанию совпадают с `.env.example`).

```bash
export $(grep -v '^#' .env | xargs)
mvn spring-boot:run
```

Приложение: http://localhost:8080

## Авторизация и документация API

- При первом старте создаётся администратор (роль `EDITOR`): логин `APP_ADMIN_USERNAME` (по умолчанию `admin`),
  пароль `APP_ADMIN_PASSWORD`. Если пароль не задан, он генерируется и выводится в лог (`WARN`).
- Роли: `VIEWER` — только чтение, `EDITOR` — чтение и изменение. Других пользователей пока добавляйте в таблицу
  `app_user` вручную (пароль — BCrypt-хеш).
- REST API (`/api/**`) использует HTTP Basic; интерфейс — форму входа `/login`.
- Swagger UI: http://localhost:8080/swagger-ui.html

## Миграции БД

Схема управляется Flyway (`src/main/resources/db/migration`). Для БД, созданной ранее EclipseLink, включён
`baseline-on-migrate`: существующие таблицы принимаются за версию 1, применяются только новые миграции.

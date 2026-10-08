# School Management System — ЛР1 (монолит)

Backend информационной системы образовательной организации: учителя, ученики, родители, классы, предметы,
домашние задания, решения, оценки и рассылки. Монолит на Spring Boot, PostgreSQL в Docker, миграции Liquibase.

## Стек

| | |
|---|---|
| Язык / сборка | Java 21, Maven |
| Фреймворк | Spring Boot 3.5 (Web, Data JPA, Validation, Actuator) |
| БД | PostgreSQL 16 (Docker) |
| Миграции | Liquibase (`src/main/resources/db/changelog`) |
| Маппинг Entity ↔ DTO | MapStruct |
| Документация API | springdoc-openapi (OpenAPI 3 + Swagger UI) |
| Пароли | BCrypt (`spring-security-crypto`) |
| Тесты | JUnit 5, Mockito, Testcontainers, JaCoCo (порог покрытия 70%) |

## Быстрый старт

```bash
docker compose up --build
```

- Swagger UI: http://localhost:8080/swagger-ui
- OpenAPI JSON: http://localhost:8080/v3/api-docs
- Health: http://localhost:8080/actuator/health

`docker compose` поднимает два контейнера:

- `postgres` — PostgreSQL 16 с healthcheck и volume `pgdata`;
- `app` — приложение, стартует после того, как БД стала healthy. При старте Liquibase накатывает
  миграции из `src/main/resources/db/changelog`.

Все параметры задаются переменными окружения. Значения по умолчанию прописаны в `docker-compose.yml`,
переопределить их можно через `.env`:

```bash
cp .env.example .env
```

| Переменная | По умолчанию | Назначение |
|---|---|---|
| `DB_NAME` / `DB_USER` / `DB_PASSWORD` | `school` | БД и учётка PostgreSQL |
| `DB_PORT` | `5432` | порт БД на хосте |
| `APP_PORT` | `8080` | порт приложения на хосте |
| `LIQUIBASE_CONTEXTS` | `demo` | `demo` — схема + демо-данные, `prod` — только схема |

Остановить и удалить данные: `docker compose down -v`.

### Демо-данные (`LIQUIBASE_CONTEXTS=demo`)

Пароль у всех демо-пользователей — `password` (в БД хранится только BCrypt-хеш).

| id пользователя | логин | роль | профиль |
|---|---|---|---|
| 1 | `admin` | SUPERVISOR | — |
| 2 | `ivanova` | TEACHER | учитель id=1, математика, 7А и 7Б |
| 3 | `petrov` | TEACHER | учитель id=2, русский язык, 7А и 7Б |
| 4 | `smirnov.a` | STUDENT | ученик id=1, 7А |
| 5 | `kuznetsova.o` | STUDENT | ученик id=2, 7А |
| 6 | `volkov.d` | STUDENT | ученик id=3, 7Б |
| 7 | `smirnova.e` | PARENT | родитель id=1, мать ученика id=1 |
| 8 | `kuznetsov.p` | PARENT | родитель id=2, отец ученика id=2 |

Плюс классы 7А/7Б/8А, предметы, два ДЗ для 7А и одно отправленное решение (ДЗ id=1, ученик id=1).

Пример сценария «оценка + уведомление»:

```bash
# выставить оценку: в одной транзакции обновляется решение и создаётся уведомление ученику и маме
curl -X POST localhost:8080/api/assignments/1/grade \
  -H 'Content-Type: application/json' -d '{"studentId": 1, "grade": 5}'

# входящие мамы (user id = 7)
curl 'localhost:8080/api/messages/inbox?userId=7'

# оценки с точки зрения родителя
curl localhost:8080/api/users/7/grades
```

Рассылка всему классу 7А (ученикам и родителям):

```bash
curl -X POST localhost:8080/api/messages/broadcast -H 'Content-Type: application/json' -d '{
  "senderId": 2, "scope": "CLASS", "classId": 1, "audience": "ALL",
  "subject": "Родительское собрание", "body": "В пятницу в 18:00, кабинет 204"
}'
```

## Локальный запуск без Docker-образа приложения

```bash
docker compose up -d postgres
mvn spring-boot:run
```

## Тесты и покрытие

```bash
mvn verify
```

- Нужен запущенный Docker: интеграционные тесты поднимают PostgreSQL через Testcontainers и прогоняют
  на нём реальные миграции Liquibase.
- `mvn verify` падает, если покрытие строк ниже 70% (`jacoco:check`).
- HTML-отчёт о покрытии: `target/site/jacoco/index.html`.

| Тест | Что проверяет |
|---|---|
| `UserServiceTest`, `GradeServiceTest`, `MessageServiceTest` | unit-тесты сервисов (Mockito) |
| `UserApiIntegrationTest` | CRUD пользователей, роли, валидация, коды ошибок, Swagger |
| `ReferenceDataIntegrationTest` | CRUD предметов и классов, уникальность, удаление со связями |
| `PeopleIntegrationTest` | учителя и их нагрузка, ученики, `X-Total-Count`, связи родитель–ученик |
| `AssignmentFlowIntegrationTest` | ДЗ → решение → оценка + уведомление, infinite scroll, оценки по ролям |
| `MessagingIntegrationTest` | личные сообщения, входящие, прочтение, рассылки по классу/параллели/школе |

## Архитектура

```
controller  → REST-эндпоинты, валидация DTO (@Valid), HTTP-статусы
service     → бизнес-логика и транзакции
repository  → Spring Data JPA
entity      → JPA-сущности (+ Bean Validation на уровне Entity), enum'ы
dto         → запросы/ответы (Java records), Entity наружу не отдаются
mapper      → MapStruct: Entity → DTO
exception   → свои исключения + глобальный @RestControllerAdvice
config      → BCrypt, OpenAPI
```

### Модель данных

| Таблица | Связь |
|---|---|
| `users` ↔ `roles` через `user_roles` | **Many-to-Many** |
| `classes` → `students` | **One-to-Many** |
| `teachers` → `assignments`, `assignments` → `submissions` | **One-to-Many** |
| `users` → `teachers` / `students` / `parents` | One-to-One (профиль) |
| `parent_student` (`relation`: MOTHER / FATHER / GUARDIAN) | **Many-to-Many с доп. полем** |
| `teacher_class_subject` (`academic_year`) | **Many-to-Many с доп. полем** |
| `message_recipients` (`is_read`, `read_at`) | **Many-to-Many с доп. полями** |

Все enum'ы хранятся в БД строками (`@Enumerated(EnumType.STRING)` + `VARCHAR` с `CHECK`-ограничением).

Миграции:

- `001-init-schema.sql` — таблицы, внешние ключи, индексы, ограничения;
- `002-reference-data.sql` — справочник ролей (применяется всегда);
- `003-demo-data.sql` — демо-данные (только с контекстом `demo`).

## API

Полное описание — в Swagger UI. Основные эндпоинты:

| Метод | Путь | Описание |
|---|---|---|
| GET/POST | `/api/users` | список (≤50) / создание пользователя |
| GET/PUT/DELETE | `/api/users/{id}` | пользователь |
| POST / DELETE | `/api/users/{id}/roles`, `/api/users/{id}/roles/{role}` | роли пользователя (M:N) |
| GET | `/api/users/{id}/grades` | оценки: ученик — свои, родитель — детей, учитель — по своим ДЗ |
| CRUD | `/api/subjects`, `/api/classes` | справочники |
| GET | `/api/classes/{id}/students` | ученики класса |
| CRUD | `/api/teachers` | профили учителей |
| GET/POST/DELETE | `/api/teachers/{id}/workload` | нагрузка: класс + предмет + учебный год |
| CRUD | `/api/students` | профили учеников |
| GET | `/api/students/page` | **пагинация с `X-Total-Count`** |
| GET/POST/DELETE | `/api/students/{id}/parents` | контакты родителей / привязка родителя |
| CRUD | `/api/parents`, GET `/api/parents/{id}/children` | родители и их дети |
| GET | `/api/assignments?cursor=&size=` | **infinite scroll** (keyset по id, без total) |
| GET | `/api/assignments/page` | **пагинация с `X-Total-Count`** |
| POST/GET/PUT/DELETE | `/api/assignments`, `/api/assignments/{id}` | домашние задания |
| POST/GET | `/api/assignments/{id}/submissions` | отправка решения / список решений |
| POST | `/api/assignments/{id}/grade` | **оценка + уведомление (транзакция)** |
| POST | `/api/messages` | личное сообщение |
| POST | `/api/messages/broadcast` | **массовая рассылка (транзакция)** |
| GET | `/api/messages/{id}`, `/api/messages/inbox?userId=` | сообщение / входящие |
| PATCH | `/api/messages/{id}/read?userId=` | отметить прочитанным |

### Пагинация

- Все `findAll`-эндпоинты принимают `page` (≥0) и `size` (1..50, по умолчанию 20). `size > 50` → `400`.
  Дополнительно размер ограничен 50 в сервисном слое.
- **Infinite scroll** — `GET /api/assignments`: первый запрос без `cursor`, дальше `cursor = nextCursor`
  из предыдущего ответа. Ответ: `{ items, nextCursor, hasNext }`, общее количество не считается
  (запрос `WHERE id < :cursor ORDER BY id DESC LIMIT size+1`).
- **С общим количеством** — `GET /api/students/page` и `GET /api/assignments/page`: тело — массив,
  общее число записей — в заголовке `X-Total-Count`.

### Транзакции

1. **Выставление оценки + уведомление** (`GradeService.grade`, `POST /api/assignments/{id}/grade`).
   В одной транзакции: `UPDATE submissions` (оценка, статус `GRADED`, время) + `INSERT INTO messages` +
   `INSERT INTO message_recipients` для ученика и всех его родителей.
   *Обоснование:* оценка и уведомление должны сохраниться атомарно. Иначе возможны рассинхроны:
   родитель получил уведомление об оценке, которой в журнале нет, или оценка появилась «молча».
2. **Массовая рассылка** (`MessageService.broadcast`, `POST /api/messages/broadcast`).
   Одна запись в `messages` + N записей в `message_recipients` (ученики и/или родители класса, параллели
   или всей школы, без дублей).
   *Обоснование:* при ошибке на середине вставки нельзя оставить частично разосланное сообщение —
   либо его получают все адресаты, либо никто (откат всей транзакции).

### Ошибки

Все ошибки возвращаются в едином формате `ApiError` (`timestamp`, `status`, `error`, `message`, `path`,
`fieldErrors`):

| Статус | Когда |
|---|---|
| 201 Created | создание ресурса (+ заголовок `Location`) |
| 204 No Content | удаление |
| 400 Bad Request | ошибка валидации DTO/Entity, некорректный JSON, неверный тип параметра, `size > 50` |
| 404 Not Found | сущность не найдена, неизвестный путь |
| 405 Method Not Allowed | неподдерживаемый HTTP-метод |
| 409 Conflict | дубликат (логин, email, название), удаление записи, на которую есть ссылки |
| 422 Unprocessable Entity | нарушение бизнес-правила (учитель не ведёт предмет в классе, ученик из другого класса, рассылка от ученика и т.п.) |
| 500 Internal Server Error | непредвиденная ошибка |

Пароль и его хеш никогда не попадают в ответы: наружу отдаются только DTO.

## Git

- Ветки `feature/*`, слитые в `main` через merge-коммиты.
- Сообщения коммитов — Conventional Commits (`feat:`, `fix:`, `test:`, `docs:`, `chore:`).

## Соответствие требованиям ЛР1

| Требование | Где реализовано |
|---|---|
| Java + Spring Boot + Maven | `pom.xml` |
| PostgreSQL в Docker | `docker-compose.yml` (сервис `postgres`) |
| Миграции Liquibase | `src/main/resources/db/changelog` |
| Spring Data JPA | `repository/*` |
| Валидация на уровне контроллера и Entity | `@Valid` + jakarta-аннотации в `dto/*`; `@NotNull/@Size/...` в `entity/*` |
| Entity отделены от DTO | `entity/*` ↔ `dto/*`, маппинг через `mapper/*` (MapStruct) |
| Правильные HTTP-статусы, глобальный обработчик | `exception/GlobalExceptionHandler` |
| `findAll` не более 50 записей | `PaginationConstants.MAX_PAGE_SIZE`, `@Max(50)` на `size` |
| Infinite scroll без total | `GET /api/assignments?cursor=` |
| Пагинация с `X-Total-Count` | `GET /api/students/page`, `GET /api/assignments/page` |
| 2+ транзакционных запроса с обоснованием | `GradeService.grade`, `MessageService.broadcast` (см. «Транзакции») |
| One-to-Many | `classes → students`, `teachers → assignments`, `assignments → submissions` |
| Many-to-Many | `users ↔ roles` |
| Many-to-Many с доп. полем | `parent_student.relation`, `teacher_class_subject.academic_year`, `message_recipients.is_read/read_at` |
| Enum в БД строками | `@Enumerated(EnumType.STRING)` |
| OpenAPI 3 + Swagger UI | springdoc, `/swagger-ui` |
| JUnit 5 + Testcontainers, покрытие ≥ 70% | `src/test`, `jacoco-maven-plugin` (`check` на фазе `verify`) |
| Запуск через `docker compose up` | `Dockerfile`, `docker-compose.yml` |

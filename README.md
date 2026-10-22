# School Platform — лабораторная работа №2

Микросервисная версия системы управления школой из ЛР1. Внешние REST-контракты
сохранены за API Gateway, а модуль сообщений выделен в отдельный реактивный сервис со своей БД.

## Архитектура

```text
                         +------------------+
client :8080 ----------> |   API Gateway    |
                         +--------+---------+
                                  | lb:// via Eureka
                       +----------+----------+
                       |                     |
              +--------v---------+  +--------v---------+
              | academic-service |  | message-service  |
              | Reactor + JPA     |  | WebFlux + R2DBC  |
              | :8081             |  | :8082            |
              +--------+---------+  +--------+---------+
                       |  OpenFeign + Circuit Breaker  |
                       +<----------------------------->+
                       |                     |
              +--------v---------+  +--------v---------+
              | academic-db      |  | message-db       |
              | PostgreSQL :5432 |  | PostgreSQL :5433 |
              +------------------+  +------------------+

       Config Server :8888 <--- config-repo/*.yml
       Eureka Server :8761 <--- registration/discovery
```

| Модуль | Ответственность | Данные |
|---|---|---|
| `academic-service` | пользователи, роли, классы, люди, предметы, ДЗ, решения, оценки; реактивный internal directory | Spring Data JPA, Liquibase, `academic-db` |
| `message-service` | личные сообщения, рассылки, inbox, уведомления об оценках | Reactor/WebFlux, Spring Data R2DBC, `message-db` |
| `api-gateway` | единая точка входа, load-balanced routes | Spring Cloud Gateway |
| `config-server` | внешняя конфигурация сервисов | native repository `config-repo/` |
| `eureka-server` | service registry/discovery | Spring Cloud Netflix Eureka |

### Соответствие требованиям

- Все прикладные сервисы и Gateway регистрируются в Eureka.
- Порты, БД, Eureka, Feign и Resilience4j настраиваются через Config Server.
- Gateway маршрутизирует `/api/messages/**` в `message-service`, остальные `/api/**` — в `academic-service`.
- Межсервисные вызовы идут через OpenFeign по service id, без жёстких URL.
- Feign обёрнут Spring Cloud Circuit Breaker + Resilience4j. Для обоих направлений есть fallback.
- `message-service` — полностью реактивный WebFlux + R2DBC с реактивными транзакциями.
- `academic-service` сочетает Reactor и JPA: internal directory возвращает `Mono`, а JDBC-работа изолирована на `Schedulers.boundedElastic()`.

## Запуск

Требуются Docker Compose и свободные порты `5432`, `5433`, `8080`, `8081`, `8082`, `8761`, `8888`.

```bash
docker compose up --build
```

После запуска:

- API через Gateway: http://localhost:8080/api/users
- Eureka UI: http://localhost:8761
- Config Server: http://localhost:8888/academic-service/default
- Academic Swagger: http://localhost:8081/swagger-ui/index.html
- Message Swagger: http://localhost:8082/swagger-ui/index.html
- health: `http://localhost:<port>/actuator/health`

Остановка без удаления данных:

```bash
docker compose down
```

Удаление обоих тестовых БД:

```bash
docker compose down -v
```

## Примеры

Список пользователей через Gateway:

```bash
curl http://localhost:8080/api/users
```

Личное сообщение (валидация отправителя и адресатов выполняется Feign-вызовом):

```bash
curl -X POST http://localhost:8080/api/messages \
  -H 'Content-Type: application/json' \
  -d '{"senderId":2,"recipientIds":[4,7],"subject":"Тест","body":"Сообщение"}'
```

Рассылка классу:

```bash
curl -X POST http://localhost:8080/api/messages/broadcast \
  -H 'Content-Type: application/json' \
  -d '{"senderId":2,"scope":"CLASS","classId":1,"audience":"ALL","subject":"Собрание","body":"В пятницу"}'
```

Выставление оценки создаёт Feign-уведомление в `message-service`:

```bash
curl -X POST http://localhost:8080/api/assignments/1/grade \
  -H 'Content-Type: application/json' \
  -d '{"studentId":1,"grade":5}'
```

## Демонстрация преподавателю

Все пользовательские запросы ниже идут через Gateway на `localhost:8080`. После запуска дайте
Eureka около 30 секунд на первоначальное обновление registry.

1. Покажите семь изолированных контейнеров и их healthcheck:

   ```bash
   docker compose ps
   ```

   В результате оба PostgreSQL, Eureka, Config Server, два бизнес-сервиса и Gateway должны иметь
   статус `healthy`.

2. Откройте http://localhost:8761 и покажите четыре приложения со статусом `UP`:
   `ACADEMIC-SERVICE`, `MESSAGE-SERVICE`, `API-GATEWAY`, `CONFIG-SERVER`.

3. Покажите внешнюю конфигурацию сервисов:

   ```bash
   curl http://localhost:8888/academic-service/default | jq
   curl http://localhost:8888/message-service/default | jq
   ```

   В `propertySources` видны общий `application.yml` и конфигурация выбранного сервиса из
   `config-repo/`.

4. Проверьте маршрутизацию Gateway в `academic-service`:

   ```bash
   curl http://localhost:8080/api/users | jq
   ```

5. Отправьте сообщение. Gateway направит запрос в реактивный `message-service`, тот через Feign
   запросит имена пользователей у `academic-service`, затем сохранит сообщение через R2DBC:

   ```bash
   curl -X POST http://localhost:8080/api/messages \
     -H 'Content-Type: application/json' \
     -d '{"senderId":2,"recipientIds":[4,7],"subject":"Демонстрация ЛР2","body":"Feign + R2DBC"}' | jq

   curl 'http://localhost:8080/api/messages/inbox?userId=7' | jq
   ```

6. Покажите обратный Feign-вызов. Оценка сохраняется в JPA-сервисе, после чего уведомление
   создаётся в R2DBC-сервисе для ученика и его родителя:

   ```bash
   curl -X POST http://localhost:8080/api/assignments/1/grade \
     -H 'Content-Type: application/json' \
     -d '{"studentId":1,"grade":5}' | jq

   curl 'http://localhost:8080/api/messages/inbox?userId=7' | jq
   ```

   В первом ответе должны присутствовать `notificationId` и `notifiedRecipients: 2`, а во втором —
   сообщение типа `GRADE_NOTIFICATION`.

7. Покажите Circuit Breaker и fallback:

   ```bash
   docker compose stop academic-service

   for i in {1..12}; do
     curl -s -o /dev/null -w "attempt $i: HTTP %{http_code}\n" \
       -X POST http://localhost:8080/api/messages \
       -H 'Content-Type: application/json' \
       -d '{"senderId":2,"recipientIds":[4],"subject":"CB demo","body":"service unavailable"}'
   done

   curl http://localhost:8082/actuator/circuitbreakers | jq
   ```

   Запросы возвращают контролируемый `503`, а breaker
   `AcademicClientresolveDirectDirectAudienceRequest` переходит в `OPEN`.

   Верните сервис и продемонстрируйте восстановление:

   ```bash
   docker compose start academic-service
   sleep 35

   for i in {1..3}; do
     curl -s -o /dev/null -w "recovery $i: HTTP %{http_code}\n" \
       -X POST http://localhost:8080/api/messages \
       -H 'Content-Type: application/json' \
       -d '{"senderId":2,"recipientIds":[4],"subject":"CB recovery","body":"service available"}'
   done

   curl http://localhost:8082/actuator/circuitbreakers | jq
   ```

   После трёх ответов `201` состояние снова будет `CLOSED`.

Для показа реализации Reactor откройте `ReactiveMessageService` (`Mono`, `Flux`,
`TransactionalOperator`) и `ReactiveDirectoryService` (`Mono.fromCallable` и
`Schedulers.boundedElastic()` для безопасного вызова JPA).

## Конфигурация

Центральные файлы лежат в `config-repo/`:

- `application.yml` — Eureka, Feign timeouts, Circuit Breaker;
- `academic-service.yml` — JDBC/JPA/Liquibase;
- `message-service.yml` — R2DBC и SQL initialization;
- `api-gateway.yml` — Gateway routes.

Значения можно переопределить через `.env`; все поддерживаемые переменные видны в `docker-compose.yml`.

## Сборка и тесты

Проект собирается на Java 21:

```bash
mvn clean test
```

Интеграционные тесты требуют Docker. Они проверяют исходные сценарии ЛР1 на реальном PostgreSQL,
межсервисный контракт уведомлений и реактивную R2DBC-запись/inbox.

> Если системный Maven запущен на JDK новее 21, укажите `JAVA_HOME` на JDK 21. Docker-сборка уже
> зафиксирована на Eclipse Temurin 21.

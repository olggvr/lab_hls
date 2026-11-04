# School Platform — лабораторная работа №2

Микросервисная версия School Management System из ЛР1. Каждый бизнес-сервис владеет
своей PostgreSQL БД, регистрируется в Eureka, получает конфигурацию из Config Server и
доступен клиенту через API Gateway.

## Архитектура

```text
                                  +----------------+
client :8080 -------------------> |  API Gateway   |
                                  +-------+--------+
                                          | lb://, Eureka
                  +-----------------------+-----------------------+
                  |                       |                       |
          +-------v------+       +--------v-------+       +-------v-------+
          | User Service | <---- | Assignment     | <---- | Grades        |
          | JPA          | HTTP  | Service        | HTTP  | Service       |
          | :8081        |       | Reactor/R2DBC  |       | Reactor/JPA   |
          +-------+------+       | :8083          |       | :8084         |
                  ^              +--------+-------+       +-------+-------+
                  | HTTP                  |                       |
          +-------+---------+             |                       | HTTP
          | Message Service | <-----------+-----------------------+
          | Reactor/R2DBC   |
          | :8082           |
          +-----------------+

HTTP between reactive services: load-balanced WebClient + reactive Circuit Breaker
Grades -> User: OpenFeign + Circuit Breaker
Config Server :8888 <--- Git backend: config-repo/<service>/application.yml
Eureka Server :8761 <--- registration and discovery
PostgreSQL: user-db, assignment-db, grades-db, message-db
```

| Модуль | Ответственность | Стек данных |
|---|---|---|
| `user-service` | пользователи, ученики, учителя, родители, классы, предметы и workload | Spring Data JPA + Liquibase |
| `assignment-service` | CRUD домашних заданий, cursor/page pagination, сдача решений | Reactor/WebFlux + Spring Data R2DBC |
| `grades-service` | выставление и просмотр оценок, запуск уведомления | Reactor/WebFlux + Spring Data JPA + Liquibase |
| `message-service` | личные сообщения, рассылки, inbox, уведомления об оценках | Reactor/WebFlux + Spring Data R2DBC |
| `api-gateway` | единая точка входа и единый Swagger UI | Spring Cloud Gateway |
| `config-server` | внешняя Git-backed конфигурация | Spring Cloud Config |
| `eureka-server` | service registry/discovery | Spring Cloud Netflix Eureka |

### Соответствие заданию

- `Assignment Service` — требуемый сервис на Reactor + R2DBC.
- `Grades Service` — требуемый реактивный API на Reactor + Spring Data JPA. Блокирующие JPA-вызовы
  вынесены на `Schedulers.boundedElastic()`.
- Вызовы между реактивными сервисами выполняются неблокирующим load-balanced `WebClient` и
  защищены reactive Spring Cloud Circuit Breaker + Resilience4j fallback.
- Feign остаётся в цепочке `Grades Service → User Service`, поэтому требование ЛР2 по Feign
  выполнено; блокирующий вызов изолирован на `boundedElastic`.
- В `assignment-service` доступ к двум репозиториям разделён: `AssignmentService` работает только
  с `AssignmentRepository`, `SubmissionService` — только с `SubmissionRepository`.
- Реактивные контроллеры возвращают `Mono`/`Flux` напрямую; HTTP-статусы задаются аннотациями,
  без `ResponseEntity`.
- Все четыре бизнес-сервиса, Gateway и Config Server регистрируются в Eureka.
- Config Server читает профили из Git-репозитория `config-repo/`.
- Все внешние API и OpenAPI-описания агрегированы Gateway.
- JaCoCo проверяет порог line coverage сервисного слоя `≥ 70%` на фазе `verify`.

## Запуск

Нужны Docker Compose и свободные порты `5432–5435`, `8080–8084`, `8761`, `8888`.
Переопределить порты и параметры можно в `.env` по образцу `.env.example`.

```bash
docker compose up -d --build
docker compose ps
```

После старта:

- API Gateway: http://localhost:8080
- единый Swagger UI: http://localhost:8080/swagger-ui
- Eureka UI: http://localhost:8761
- Config Server: http://localhost:8888/user-service/default
- health: `http://localhost:<port>/actuator/health`

Остановка без удаления данных:

```bash
docker compose down
```

Полный сброс четырёх БД:

```bash
docker compose down -v
```

## Демонстрация преподавателю

Все бизнес-запросы ниже идут только через Gateway (`localhost:8080`). После первого запуска
дайте Eureka около 30 секунд на первичное обновление registry.

### 1. Инфраструктура, Eureka и Config Server

```bash
docker compose ps
curl -s http://localhost:8888/assignment-service/default | jq '.name, .propertySources[].name'
curl -s http://localhost:8888/grades-service/default | jq '.name, .propertySources[].name'
```

В `docker compose ps` должны быть 11 healthy-контейнеров. В Eureka UI будут `USER-SERVICE`,
`ASSIGNMENT-SERVICE`, `GRADES-SERVICE`, `MESSAGE-SERVICE`, `API-GATEWAY` и `CONFIG-SERVER` со статусом `UP`.
Ответ Config Server должен содержать Git property sources из `config-repo`.

### 2. Gateway и единый Swagger

```bash
curl -s http://localhost:8080/api/users | jq
curl -s http://localhost:8080/api/assignments | jq
curl -s http://localhost:8080/docs/grades/v3/api-docs | jq '.info.title, .paths | keys'
```

Откройте http://localhost:8080/swagger-ui и переключите выпадающий список между четырьмя
микросервисами.

### 3. Reactor + R2DBC и reactive WebClient: Assignment Service

Создайте ДЗ. Assignment Service через load-balanced WebClient проверит у User Service, что учитель ведёт
указанный предмет в классе, и сохранит запись через R2DBC:

```bash
curl -s -X POST http://localhost:8080/api/assignments \
  -H 'Content-Type: application/json' \
  -d '{"teacherId":1,"classId":1,"subjectId":1,"title":"Демо ЛР2","description":"WebClient + Reactor + R2DBC","dueDate":"2027-01-31"}' | jq

curl -s 'http://localhost:8080/api/assignments?size=2' | jq
curl -si 'http://localhost:8080/api/assignments/page?page=0&size=2'
```

В первом ответе будет `201`, cursor endpoint вернёт `nextCursor/hasNext`, а page endpoint —
заголовок `X-Total-Count`.

### 4. Reactor + JPA и цепочка межсервисных вызовов: Grades Service

В demo-данных есть решение ученика `studentId=1` по заданию `assignmentId=1`:

```bash
curl -s -X POST http://localhost:8080/api/assignments/1/grade \
  -H 'Content-Type: application/json' \
  -d '{"studentId":1,"grade":5}' | jq

curl -s http://localhost:8080/api/users/4/grades | jq
curl -s 'http://localhost:8080/api/messages/inbox?userId=7' | jq
```

Grades Service реактивно получит решение из Assignment Service, через Feign запросит аудиторию
у User Service, сохранит оценку через JPA и реактивно вызовет Message Service. В ответе будут `notificationId` и
`notifiedRecipients: 2`, а в inbox родителя — `GRADE_NOTIFICATION`.

### 5. Circuit Breaker и fallback

Остановите User Service и создайте серию reactive WebClient-вызовов из Message Service:

```bash
docker compose stop user-service

for i in {1..12}; do
  curl -s -o /dev/null -w "attempt $i: HTTP %{http_code}\n" \
    -X POST http://localhost:8080/api/messages \
    -H 'Content-Type: application/json' \
    -d '{"senderId":2,"recipientIds":[4],"subject":"CB demo","body":"User Service unavailable"}'
done

curl -s http://localhost:8082/actuator/circuitbreakers | jq
```

Вместо неконтролируемой ошибки fallback вернёт `503`, а после пяти неудач breaker перейдёт в
`OPEN`. Затем верните сервис, дождитесь Eureka и повторите вызовы:

```bash
docker compose start user-service
sleep 35

for i in {1..3}; do
  curl -s -o /dev/null -w "recovery $i: HTTP %{http_code}\n" \
    -X POST http://localhost:8080/api/messages \
    -H 'Content-Type: application/json' \
    -d '{"senderId":2,"recipientIds":[4],"subject":"CB recovery","body":"User Service available"}'
done
```

После half-open проб breaker вернётся в `CLOSED`.

## Сборка, тесты и покрытие

Проект собирается на Java 21. Интеграционные тесты используют реальный PostgreSQL через
Testcontainers. Фаза `verify` также строит JaCoCo-отчёты и проваливает сборку при покрытии
сервисного слоя ниже 70%.

```bash
mvn clean verify
```

Отчёт каждого сервиса: `services/<service>/target/site/jacoco/index.html`.

> Если системный Maven запущен на JDK новее 21, укажите `JAVA_HOME` на JDK 21. Docker-сборка
> зафиксирована на Eclipse Temurin 21.

## Центральная конфигурация

Конфигурация разложена по каталогам, как отдельные приложения Config Server:

- `config-repo/common/application.yml` — общие Eureka, Feign timeout и Resilience4j настройки;
- `config-repo/<service>/application.yml` — отдельный профиль каждого бизнес-сервиса;
- `config-repo/api-gateway/application.yml` — маршруты Gateway и агрегация OpenAPI.

Config Server ищет конфигурацию по шаблону
`config-repo/common,config-repo/{application}`, поэтому запрос
`/assignment-service/default` объединяет общий и сервисный файлы.

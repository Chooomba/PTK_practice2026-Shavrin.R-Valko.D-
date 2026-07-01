# FileShare

Система обмена файлами с авторизацией через Keycloak и хранением файлов в MinIO.

## Стек

- Backend: Java 21, Spring Boot 3, Spring Security OAuth2 Resource Server, JPA, Liquibase, MinIO SDK
- Frontend: React, TypeScript, Vite, MUI, react-oidc-context
- БД: PostgreSQL 15
- Auth: Keycloak 23 (OAuth2 / OpenID Connect)
- Хранилище: MinIO (S3 API)
- Запуск: Docker Compose

## Быстрый старт

```bash
docker compose up -d
```

После запуска:

| Сервис | URL |
| --- | --- |
| Frontend | http://localhost |
| Swagger UI | http://localhost:8080/swagger-ui.html |
| Keycloak Admin | http://localhost:8180/admin |
| MinIO API | http://localhost:9000 |
| MinIO Console | http://localhost:9001 |

Тестовые учетные данные:

| Назначение | Login | Password |
| --- | --- | --- |
| Пользователь | testuser | password |
| Keycloak Admin | admin | admin |

## Основные сценарии

- Вход через Keycloak.
- Загрузка файла в свой список.
- Просмотр списка своих файлов.
- Скачивание и удаление своих файлов.
- Отправка выбранных файлов другому пользователю.
- Загрузка файлов с устройства напрямую другому пользователю.

Получатель появляется в списке выбора после первого входа в приложение. Это сделано без Keycloak admin API: backend запоминает пользователей по данным из JWT.

## API

Все endpoints, кроме Swagger UI, требуют Bearer JWT.

| Метод | Путь | Описание |
| --- | --- | --- |
| GET | `/api/users` | Список известных пользователей для отправки файлов |
| GET | `/api/files` | Список своих файлов |
| GET | `/api/files/{id}` | Информация о своем файле |
| POST | `/api/files/upload` | Загрузить файл себе |
| POST | `/api/files/share` | Скопировать уже загруженные файлы получателю |
| POST | `/api/files/share/upload` | Загрузить файлы с устройства напрямую получателю |
| GET | `/api/files/{id}/download` | Скачать свой файл |
| DELETE | `/api/files/{id}` | Удалить свой файл |

Пример отправки уже загруженных файлов:

```json
{
  "recipientUserId": "receiver-user-id",
  "fileIds": ["5b9ef7d5-6c37-4ef9-8f0b-63e96191e9b9"]
}
```

## Ограничения

- Максимальный размер файла: 100 МБ.
- Запрещенные расширения задаются в `backend/src/main/resources/application.yml` через `file.forbidden-extensions`.
- При ошибках загрузки backend возвращает `ProblemDetail.detail` с конкретной причиной.
- Пользователь видит, скачивает и удаляет только файлы, владельцем которых он является.
- При отправке создается отдельная копия файла у получателя, поэтому удаление оригинала не ломает файл получателя.

## Docker и OIDC

В Docker Compose frontend получает токены от публичного адреса Keycloak `http://localhost:8180`, поэтому backend валидирует issuer `http://localhost:8180/realms/fileshare`.

При этом backend внутри Docker-сети получает JWK ключи по внутреннему адресу:

```yaml
SPRING_SECURITY_OAUTH2_RESOURCESERVER_JWT_JWK_SET_URI: http://keycloak:8080/realms/fileshare/protocol/openid-connect/certs
```

Это важно: если issuer настроить на `http://keycloak:8080`, токены из браузера будут отклоняться.

## Разработка

Backend:

```bash
cd backend
mvn spring-boot:run
```

Frontend:

```bash
cd frontend
npm install
npm run dev
```

Тесты:

```bash
cd backend
mvn test
```

Интеграционные тесты используют Testcontainers и требуют доступный Docker daemon.

## Структура

```text
fileshare/
├── backend/
│   ├── src/main/java/com/fileshare/
│   │   ├── controller/
│   │   ├── service/
│   │   ├── repository/
│   │   ├── entity/
│   │   ├── dto/
│   │   ├── mapper/
│   │   ├── security/
│   │   ├── config/
│   │   ├── exception/
│   │   └── storage/
│   └── src/test/
├── frontend/
│   └── src/
│       ├── api/
│       ├── components/
│       ├── hooks/
│       ├── pages/
│       └── types/
├── keycloak/
├── minio-init/
└── docker-compose.yml
```

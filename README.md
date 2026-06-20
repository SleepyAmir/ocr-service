# OCR Service

Standalone OCR microservice for the chatbot platform.

This service combines the professor's OCR sample idea with the platform microservice architecture:

- Tess4J OCR engine
- MongoDB storage
- Uploaded file bytes are stored in MongoDB like the professor sample
- REST API instead of Thymeleaf MVC
- Image and PDF support
- Keyword search inside OCR text
- Java 21 / Spring Boot 4.1.0 compatible with the main platform

## Endpoints

- `GET /api/v1/ocr/health`
- `POST /api/v1/ocr/documents` multipart `file`
- `GET /api/v1/ocr/documents?page=0&size=10`
- `GET /api/v1/ocr/documents/{id}`
- `GET /api/v1/ocr/documents/{id}/file`
- `GET /api/v1/ocr/documents/{id}/search?keyword=...`
- `GET /api/v1/ocr/search?keyword=...&page=0&size=10`

## Run locally

```bash
mvn spring-boot:run
```

Default port: `8090`

Swagger:

```text
http://localhost:8090/swagger-ui.html
```

## Docker Compose

From the root project:

```bash
docker compose up -d mongodb
docker compose up --build ocr-service
```

## Important note

Files are stored directly as `byte[]` in MongoDB to stay close to the professor's sample.
For large production files, use MongoDB GridFS instead.

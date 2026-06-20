FROM maven:3.9-eclipse-temurin-21 AS builder

WORKDIR /workspace
COPY pom.xml ./
COPY src/ src/

RUN --mount=type=cache,target=/root/.m2 mvn package -DskipTests -B --no-transfer-progress

FROM eclipse-temurin:21-jre-jammy

RUN apt-get update && \
    apt-get install -y --no-install-recommends \
        tesseract-ocr \
        tesseract-ocr-fas \
        tesseract-ocr-eng \
        libtesseract-dev && \
    rm -rf /var/lib/apt/lists/*

WORKDIR /app
COPY --from=builder /workspace/target/*.jar app.jar

ENV OCR_TESSDATA_PATH=/usr/share/tesseract-ocr/5/tessdata
ENV OCR_LANGUAGE=fas+eng
ENV OCR_SERVICE_PORT=8090

EXPOSE 8090

ENTRYPOINT ["java", "-XX:+UseContainerSupport", "-XX:MaxRAMPercentage=75.0", "-jar", "app.jar"]

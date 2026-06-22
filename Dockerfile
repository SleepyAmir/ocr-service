FROM maven:3.9-eclipse-temurin-21 AS builder

WORKDIR /workspace
COPY pom.xml ./
COPY src/ src/

RUN --mount=type=cache,target=/root/.m2 mvn package -DskipTests -B --no-transfer-progress

FROM eclipse-temurin:21-jre-jammy

# tesseract-ocr + libtesseract provide the native OCR engine/libraries.
# We DON'T rely on the apt language packs' install path (it varies between
# Tesseract 4/5 and Ubuntu versions, which caused "Error opening data file"
# and a native SIGSEGV crash). Instead we ship the traineddata files from the
# project itself into a fixed directory below.
RUN apt-get update && \
    apt-get install -y --no-install-recommends \
        tesseract-ocr \
        libtesseract-dev && \
    rm -rf /var/lib/apt/lists/*

WORKDIR /app
COPY --from=builder /workspace/target/*.jar app.jar

# Ship the project's own language data and point Tesseract at it explicitly.
# These files come from src/main/resources/tessdata (fas + eng).
COPY src/main/resources/tessdata/ /app/tessdata/

# Fixed, self-contained tessdata path (no dependency on apt's layout).
ENV OCR_TESSDATA_PATH=/app/tessdata
ENV OCR_LANGUAGE=fas+eng
ENV OCR_SERVICE_PORT=8090

EXPOSE 8090

ENTRYPOINT ["java", "-XX:+UseContainerSupport", "-XX:MaxRAMPercentage=75.0", "-jar", "app.jar"]

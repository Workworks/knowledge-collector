FROM eclipse-temurin:17-jdk-jammy AS build
WORKDIR /workspace
COPY . .
RUN chmod +x mvnw && ./mvnw -DskipTests package

FROM eclipse-temurin:17-jre-jammy
RUN apt-get update && apt-get install -y --no-install-recommends curl \
    && rm -rf /var/lib/apt/lists/*
WORKDIR /app
COPY --from=build /workspace/knowledge-collector-boot/target/knowledge-collector.jar /app/knowledge-collector.jar
RUN mkdir -p /data/database /data/article-content /data/snapshots /data/exports /data/logs
ENV KNOWLEDGE_COLLECTOR_DATA_DIR=/data \
    KNOWLEDGE_COLLECTOR_SERVER_ADDRESS=0.0.0.0 \
    KNOWLEDGE_COLLECTOR_SERVER_PORT=8080 \
    KNOWLEDGE_COLLECTOR_OLLAMA_BASE_URL=http://ollama:11434
EXPOSE 8080
VOLUME ["/data"]
HEALTHCHECK --interval=30s --timeout=5s --start-period=30s --retries=5 \
  CMD curl -fsS http://127.0.0.1:8080/actuator/health || exit 1
ENTRYPOINT ["java","-jar","/app/knowledge-collector.jar"]

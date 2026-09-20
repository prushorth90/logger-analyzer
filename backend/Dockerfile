FROM maven:3.9-eclipse-temurin-21 AS build
WORKDIR /app
COPY pom.xml .
RUN --mount=type=cache,target=/root/.m2 mvn -B -ntp dependency:go-offline
COPY src ./src
RUN --mount=type=cache,target=/root/.m2 mvn -B -ntp verify

FROM eclipse-temurin:21-jre-jammy
ARG OTEL_JAVA_AGENT_VERSION=2.20.1
RUN apt-get update \
    && apt-get install -y --no-install-recommends curl \
    && curl --fail --location --silent --show-error \
        "https://github.com/open-telemetry/opentelemetry-java-instrumentation/releases/download/v${OTEL_JAVA_AGENT_VERSION}/opentelemetry-javaagent.jar" \
        --output /opt/opentelemetry-javaagent.jar \
    && rm -rf /var/lib/apt/lists/* \
    && useradd --system --uid 10001 --create-home app
WORKDIR /app
COPY --from=build --chown=app:app /app/target/log-analyzer.jar app.jar
USER app
EXPOSE 8080
ENTRYPOINT ["java", "-javaagent:/opt/opentelemetry-javaagent.jar", "-jar", "app.jar"]
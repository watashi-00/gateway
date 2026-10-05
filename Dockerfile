FROM maven:3.9.6-eclipse-temurin-21 AS builder
WORKDIR /build

COPY pom.xml .
COPY local-maven-repo ./local-maven-repo
COPY src ./src

RUN mvn clean package -DskipTests

FROM eclipse-temurin:21-jre-jammy

WORKDIR /app

RUN apt-get update && apt-get install -y htop wrk procps curl && rm -rf /var/lib/apt/lists/*

RUN useradd -u 1001 -m watashi \
 && mkdir -p /app/data /app/logs \
 && chown -R watashi:watashi /app

COPY --from=builder --chown=watashi:watashi /build/target/gateway-*.jar /app/app.jar

ENV HEXACLOUD_STATE_DIR=/app/data
ENV JAVA_OPTS="-XX:+UseParallelGC -XX:+UseStringDeduplication -Dhexacloud.state.dir=/app/data"

EXPOSE 8079 8080 8081 8082

VOLUME ["/app/data"]
VOLUME ["/app/logs"]

USER root

ENTRYPOINT ["sh","-c","exec java $JAVA_OPTS -jar /app/app.jar"]
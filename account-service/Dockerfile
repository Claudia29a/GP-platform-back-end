# Stage 1: build the executable jar.
FROM eclipse-temurin:21-jdk-alpine AS build
WORKDIR /workspace

# Download dependencies first, so this layer is cached until build.gradle changes.
COPY gradlew settings.gradle build.gradle ./
COPY gradle gradle
RUN ./gradlew dependencies --no-daemon > /dev/null

COPY src src
# Tests run in the pipeline before the image is built.
RUN ./gradlew bootJar --no-daemon -x test
# Unpack the jar into app.jar + lib/, the layout class data sharing needs.
RUN java -Djarmode=tools -jar build/libs/*.jar extract --destination /workspace/extracted \
    && mv /workspace/extracted/*.jar /workspace/extracted/app.jar

# Stage 2: only the Java runtime and the application.
FROM eclipse-temurin:21-jre-alpine
# The base image can lag behind Alpine's security fixes.
RUN apk upgrade --no-cache && addgroup -S app && adduser -S app -G app && mkdir /app && chown app:app /app
USER app
WORKDIR /app
COPY --from=build --chown=app:app /workspace/extracted/ ./

# Let the JVM size its memory to the container's limit. The garbage collector is fixed, because the class archive
# below only works with the collector it was made with, and Azure's half CPU would otherwise pick another one.
# TieredStopAtLevel=1 uses only the quick compiler: start-up on half a CPU halves, and requests stay as fast
# (measured: sign-in 0.11 s either way).
ENV JAVA_TOOL_OPTIONS="-XX:MaxRAMPercentage=75 -XX:+UseSerialGC -XX:TieredStopAtLevel=1"

# Faster start-up (class data sharing): one practice start during the build saves the loaded classes to app.jsa,
# and every real start reads them from there. The practice start stops right after start-up and uses made-up
# settings: it never connects to a database or a mail server.
RUN ACCOUNT_DB_URL=jdbc:postgresql://localhost:5432/training ACCOUNT_DB_USER=training ACCOUNT_DB_PASSWORD=training \
    ACCOUNT_TOKEN_PRIVATE_KEY= ACCOUNT_DATA_KEY=MDEyMzQ1Njc4OWFiY2RlZjAxMjM0NTY3ODlhYmNkZWY= \
    MAIL_HOST=localhost MAIL_PORT=1 MAIL_USERNAME= MAIL_PASSWORD= MAIL_AUTH=false MAIL_STARTTLS_REQUIRED=false \
    MAIL_FROM=training@carebridge.test \
    java -XX:ArchiveClassesAtExit=app.jsa -Dspring.context.exit=onRefresh \
    -Dspring.flyway.enabled=false -Dspring.jpa.hibernate.ddl-auto=none \
    -Dspring.jpa.properties.hibernate.boot.allow_jdbc_metadata_access=false \
    -Dspring.jpa.database-platform=org.hibernate.dialect.PostgreSQLDialect \
    -jar app.jar > /dev/null

EXPOSE 8080
ENTRYPOINT ["java", "-XX:SharedArchiveFile=app.jsa", "-jar", "app.jar"]

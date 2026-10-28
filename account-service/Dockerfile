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

# Stage 2: only the Java runtime and the jar.
FROM eclipse-temurin:21-jre-alpine
RUN addgroup -S app && adduser -S app -G app
USER app
WORKDIR /app
COPY --from=build /workspace/build/libs/*.jar app.jar

EXPOSE 8080
# Let the JVM size its memory to the container's limit.
ENV JAVA_TOOL_OPTIONS="-XX:MaxRAMPercentage=75"
ENTRYPOINT ["java", "-jar", "app.jar"]

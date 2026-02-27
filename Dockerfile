FROM ghcr.io/rblaine95/eclipse-temurin:25 AS builder
WORKDIR /app

COPY . .

RUN ./gradlew :server:shadowJar --no-daemon --stacktrace

FROM ghcr.io/rblaine95/eclipse-temurin:25
WORKDIR /app
COPY --from=builder /app/server/build/libs/larpconnect.jar app.jar
EXPOSE 8080
ENTRYPOINT ["java", "-jar", "app.jar"]

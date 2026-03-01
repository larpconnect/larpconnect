FROM ghcr.io/rblaine95/eclipse-temurin:25 AS builder
WORKDIR /app

COPY . .

RUN ./gradlew :server:shadowJar --no-daemon --stacktrace

FROM ghcr.io/rblaine95/eclipse-temurin:25
# For most mortals
EXPOSE 8080

# For Render
EXPOSE 10000
WORKDIR /app
COPY --from=builder /app/server/build/libs/larpconnect.jar app.jar
EXPOSE 8080
ENTRYPOINT ["java", "-jar", "app.jar"]

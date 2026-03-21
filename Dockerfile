FROM ghcr.io/rblaine95/eclipse-temurin:25 AS builder
WORKDIR /app

COPY . .

RUN ./gradlew :server:shadowJar --no-daemon --stacktrace

FROM ghcr.io/rblaine95/eclipse-temurin:25
# For most mortals
EXPOSE 8080

# For Render
EXPOSE 10000

RUN useradd -m larpconnect
USER larpconnect

WORKDIR /app
COPY --from=builder --chown=larpconnect:larpconnect /app/server/build/libs/larpconnect.jar app.jar
EXPOSE 8080
ENTRYPOINT ["java", "-jar", "app.jar"]

FROM ghcr.io/rblaine95/eclipse-temurin:25 AS builder
WORKDIR /app
RUN apt-get update && apt-get install -y --no-install-recommends \
    curl \
    tar \
    && rm -rf /var/lib/apt/lists/*

# Define Go version and installation path
ENV GO_VERSION=1.23.0
ENV GOROOT=/usr/local/go
ENV PATH="${GOROOT}/bin:${PATH}"

# Download and extract Go
RUN curl -LfsSo /tmp/go.tar.gz https://go.dev{GO_VERSION}.linux-amd64.tar.gz \
    && tar -C /usr/local -xzf /tmp/go.tar.gz \
    && rm /tmp/go.tar.gz
COPY . .

RUN ./gradlew :server:shadowJar --no-daemon --stacktrace

FROM ghcr.io/rblaine95/eclipse-temurin:25
WORKDIR /app
COPY --from=builder /app/server/build/libs/larpconnect.jar app.jar
EXPOSE 8080
ENTRYPOINT ["java", "-jar", "app.jar"]

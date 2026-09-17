FROM ghcr.io/rblaine95/eclipse-temurin:25

# Standard HTTP port
EXPOSE 8080

# Cloud platform port (e.g. Render)
EXPOSE 10000

RUN useradd -m larpconnect
WORKDIR /app

# Stage prebuilt application distribution from host
COPY --chown=larpconnect:larpconnect server/build/install/server ./
RUN chmod +x /app/bin/server

USER larpconnect

ENTRYPOINT ["/app/bin/server"]
CMD ["server"]

#!/usr/bin/env bash
set -euo pipefail

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
CERT_DIR="${SCRIPT_DIR}/certs"
PEM_FILE="${CERT_DIR}/haproxy.pem"

if [ ! -f "${PEM_FILE}" ]; then
  mkdir -p "${CERT_DIR}"
  KEY_FILE="${CERT_DIR}/dev.key"
  CRT_FILE="${CERT_DIR}/dev.crt"

  openssl req -x509 -newkey rsa:2048 -nodes \
    -keyout "${KEY_FILE}" \
    -out "${CRT_FILE}" \
    -days 365 \
    -subj "/CN=localhost" \
    -addext "subjectAltName=DNS:localhost,IP:127.0.0.1" \
    2>/dev/null

  cat "${KEY_FILE}" "${CRT_FILE}" > "${PEM_FILE}"
  rm -f "${KEY_FILE}" "${CRT_FILE}"
  chmod 600 "${PEM_FILE}"
  echo "Generated HAProxy development certificate bundle: ${PEM_FILE}"
fi

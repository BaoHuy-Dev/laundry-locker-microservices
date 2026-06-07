#!/usr/bin/env bash
set -Eeuo pipefail

APP_DIR="${APP_DIR:-/opt/laundry-locker-microservices}"
ARCHIVE="${ARCHIVE:-/tmp/laundry-locker-microservices.tar.gz}"
NEW_DIR="${APP_DIR}.new"
BACKUP_DIR="${APP_DIR}.previous"

rollback() {
  echo "Rolling back to previous release..."
  if [ -d "$BACKUP_DIR" ]; then
    rm -rf "$APP_DIR"
    mv "$BACKUP_DIR" "$APP_DIR"
    cd "$APP_DIR"
    docker compose up -d --build --remove-orphans || true
  fi
}

trap 'echo "Deploy failed."; rollback' ERR

if [ ! -s "$ARCHIVE" ]; then
  echo "Artifact not found: $ARCHIVE" >&2
  exit 1
fi

command -v docker >/dev/null
docker compose version >/dev/null

rm -rf "$NEW_DIR"
mkdir -p "$NEW_DIR"
tar -xzf "$ARCHIVE" -C "$NEW_DIR"

cd "$NEW_DIR"
docker compose config >/tmp/laundry-locker-compose-check.yml

rm -rf "$BACKUP_DIR"
if [ -d "$APP_DIR" ]; then
  mv "$APP_DIR" "$BACKUP_DIR"
fi
mv "$NEW_DIR" "$APP_DIR"

cd "$APP_DIR"
docker compose up -d --build --remove-orphans
docker compose ps

rm -f "$ARCHIVE"
trap - ERR

echo "Deploy completed."

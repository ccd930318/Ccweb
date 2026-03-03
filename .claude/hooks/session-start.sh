#!/bin/bash
set -euo pipefail

# Only run in Claude Code remote (web) environments
if [ "${CLAUDE_CODE_REMOTE:-}" != "true" ]; then
  exit 0
fi

echo "==> Installing frontend dependencies..."
cd "$CLAUDE_PROJECT_DIR/frontend"
npm install

echo "==> Downloading Gradle wrapper (backend)..."
cd "$CLAUDE_PROJECT_DIR/backend"
./gradlew dependencies --no-daemon -q 2>&1 | tail -3 || true

echo "==> Starting PostgreSQL..."
pg_isready -q 2>/dev/null || pg_ctlcluster 16 main start 2>/dev/null || true
# Wait up to 10s for PostgreSQL to be ready
for i in $(seq 1 10); do
  pg_isready -q 2>/dev/null && break
  sleep 1
done

echo "==> Ensuring database and user exist..."
su -c "psql -tc \"SELECT 1 FROM pg_roles WHERE rolname='bt_user'\" | grep -q 1 || psql -c \"CREATE USER bt_user WITH PASSWORD 'bt_pass';\"" postgres 2>/dev/null || true
su -c "psql -tc \"SELECT 1 FROM pg_database WHERE datname='babytracker'\" | grep -q 1 || psql -c \"CREATE DATABASE babytracker OWNER bt_user;\"" postgres 2>/dev/null || true
su -c "psql -c \"GRANT ALL PRIVILEGES ON DATABASE babytracker TO bt_user;\"" postgres 2>/dev/null || true

echo "==> Starting backend (Spring Boot)..."
cd "$CLAUDE_PROJECT_DIR/backend"
nohup ./gradlew bootRun --no-daemon > /tmp/backend.log 2>&1 &
BACKEND_PID=$!
echo "Backend PID: $BACKEND_PID"

echo "==> Starting frontend (Next.js)..."
cd "$CLAUDE_PROJECT_DIR/frontend"
nohup npm run dev -- --port 3000 > /tmp/frontend.log 2>&1 &
FRONTEND_PID=$!
echo "Frontend PID: $FRONTEND_PID"

echo "==> Waiting for backend to be ready (up to 60s)..."
for i in $(seq 1 60); do
  curl -s http://localhost:8080/actuator/health 2>/dev/null | grep -q '"UP"' && break
  curl -s http://localhost:8080/api/auth/login -X POST -H "Content-Type: application/json" \
    -d '{"email":"x","password":"x"}' 2>/dev/null | grep -qE 'token|error|Bad|Invalid' && break
  sleep 1
done

echo "==> Session start hook complete."
echo "    Frontend: http://localhost:3000"
echo "    Backend:  http://localhost:8080"

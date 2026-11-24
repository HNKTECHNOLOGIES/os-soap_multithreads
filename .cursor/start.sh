#!/usr/bin/env bash
set -euo pipefail

# Start PostgreSQL if installed
if command -v pg_isready >/dev/null 2>&1; then
  if ! pg_isready -h localhost -q 2>/dev/null; then
    sudo pg_ctlcluster 16 main start 2>/dev/null \
      || sudo service postgresql start 2>/dev/null \
      || true
  fi
  for i in $(seq 1 30); do
    pg_isready -h localhost -q && break
    sleep 1
  done
  pg_isready -h localhost || echo "WARN: PostgreSQL non prêt" >&2
fi

# Ensure POC DB/user exist (idempotent, local only)
if command -v psql >/dev/null 2>&1 && pg_isready -h localhost -q 2>/dev/null; then
  sudo -u postgres psql -v ON_ERROR_STOP=0 <<'SQL' || true
DO $$
BEGIN
  IF NOT EXISTS (SELECT FROM pg_roles WHERE rolname = 'evaloc') THEN
    CREATE ROLE evaloc LOGIN PASSWORD 'evaloc';
  END IF;
END$$;
SELECT 'CREATE DATABASE gesloc OWNER evaloc'
WHERE NOT EXISTS (SELECT FROM pg_database WHERE datname = 'gesloc')\gexec
SQL
fi

echo "start EVALOC: PostgreSQL prêt (si disponible)."
# Keep start non-blocking: return after services are up
exit 0

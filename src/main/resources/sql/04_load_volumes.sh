#!/usr/bin/env bash
# Charge les volumes de données synthétiques dans gesloc.
# Usage: ./04_load_volumes.sh [1000|10000|50000|100000]
set -euo pipefail

NB_FICHES="${1:-1000}"
DB_NAME="${DB_NAME:-gesloc}"
DB_USER="${DB_USERNAME:-evaloc}"
export PGPASSWORD="${DB_PASSWORD:-evaloc}"

SCRIPT_DIR="$(cd "$(dirname "$0")" && pwd)"

echo "Chargement volume nb_fiches=${NB_FICHES} dans ${DB_NAME}..."
psql -h localhost -U "${DB_USER}" -d "${DB_NAME}" -v ON_ERROR_STOP=1 -f "${SCRIPT_DIR}/01_schema.sql"
psql -h localhost -U "${DB_USER}" -d "${DB_NAME}" -v ON_ERROR_STOP=1 -v nb_fiches="${NB_FICHES}" -f "${SCRIPT_DIR}/02_generate_data.sql"
echo "Terminé."

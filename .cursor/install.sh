#!/usr/bin/env bash
set -euo pipefail

# Resolve project root (linked repo or local POC path)
if [ -d /agent/repos/os-soap_multithreads ]; then
  cd /agent/repos/os-soap_multithreads
elif [ -d /agent/evaloc-soap-parallel ]; then
  cd /agent/evaloc-soap-parallel
elif [ -f /agent/pom.xml ]; then
  cd /agent
else
  echo "Aucun pom.xml EVALOC trouvé" >&2
  exit 1
fi

echo "Install EVALOC dans: $(pwd)"

# Ensure Java + Maven (idempotent on Ubuntu)
if ! command -v java >/dev/null 2>&1; then
  sudo apt-get update -qq
  sudo DEBIAN_FRONTEND=noninteractive apt-get install -y -qq openjdk-21-jdk-headless maven
fi
if ! command -v mvn >/dev/null 2>&1; then
  sudo apt-get update -qq
  sudo DEBIAN_FRONTEND=noninteractive apt-get install -y -qq maven
fi

# Ensure PostgreSQL client/server tools available
if ! command -v psql >/dev/null 2>&1; then
  sudo apt-get update -qq
  sudo DEBIAN_FRONTEND=noninteractive apt-get install -y -qq postgresql postgresql-contrib
fi

# Prefetch Maven dependencies (no tests)
mvn -B -q -DskipTests dependency:resolve dependency:resolve-plugins
mvn -B -q -DskipTests package

echo "Install EVALOC terminé."

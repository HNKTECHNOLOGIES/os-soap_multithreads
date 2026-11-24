# Installation

## Prérequis

- JDK **8+** (compilation `--release 8` ; runtime testé également sur JDK 21)
- Maven 3.8+
- PostgreSQL 12+ (16 recommandé)
- Variables d’environnement : `DB_USERNAME`, `DB_PASSWORD` (ne pas committer les secrets)

## Création de la base POC

```bash
sudo -u postgres psql <<'SQL'
CREATE USER evaloc WITH PASSWORD 'evaloc';
CREATE DATABASE gesloc OWNER evaloc;
GRANT ALL PRIVILEGES ON DATABASE gesloc TO evaloc;
SQL

export DB_USERNAME=evaloc
export DB_PASSWORD=evaloc

# Volumes : 1000 | 10000 | 50000 | 100000
bash src/main/resources/sql/04_load_volumes.sh 1000
```

## Configuration

Éditer `src/main/resources/application.properties` (rechargée au démarrage, sans recompilation du code métier) :

```properties
db.url=jdbc:postgresql://localhost:5432/gesloc
db.username=${DB_USERNAME:evaloc}
db.password=${DB_PASSWORD}
parallel.enabled=true
parallel.partitions=4
os.timeout.ms=2500
```

## Build & tests

```bash
mvn clean test
mvn -Pintegration-tests test   # force les propriétés DB de test
mvn package -DskipTests
```

## Démarrage

```bash
java -cp "target/evaloc-soap-parallel-1.0.0-SNAPSHOT.jar:target/lib/*" fr.evaloc.Application
```

Vérifier : `curl -s http://localhost:8080/ws/declarations?wsdl | head`

## Exemple SoapUI

Fichier : `src/test/resources/soapui/rechercherDeclarations-samples.xml`

```bash
curl -s -H 'Content-Type: text/xml;charset=UTF-8' \
  -H 'SOAPAction: http://evaloc.fr/declarations/rechercherDeclarations' \
  --data @- http://localhost:8080/ws/declarations <<'XML'
<soapenv:Envelope xmlns:soapenv="http://schemas.xmlsoap.org/soap/envelope/"
                  xmlns:dec="http://evaloc.fr/declarations">
  <soapenv:Body>
    <dec:rechercherDeclarationsRequest>
      <dec:numeroPage>1</dec:numeroPage>
      <dec:taillePage>10</dec:taillePage>
    </dec:rechercherDeclarationsRequest>
  </soapenv:Body>
</soapenv:Envelope>
XML
```

## WAR futur

```bash
mvn -Pwar package
```

Le packaging WAR reste une adaptation prévue ; le POC autonome utilise le JAR + `Endpoint.publish`.

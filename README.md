# evaloc-soap-parallel

POC **EVALOC** — Nouvel OS SOAP pour CLIR DSFR avec exécution **parallèle** des requêtes SQL sur PostgreSQL GESLOC.

## Objectif

À chaque appel SOAP synchrone `rechercherDeclarations` :

1. Interroger PostgreSQL GESLOC (JDBC + HikariCP)
2. Partitionner la recherche sur des plages d'`idFiche`
3. Exécuter les partitions en parallèle (`CompletableFuture` + `ThreadPoolExecutor`)
4. Fusionner, dédoublonner, paginer
5. Respecter un budget interne configurable (défaut **2500 ms**, DAC **3000 ms**)

Sans Spring Boot / Spring Framework / Hibernate / JPA / Spring Batch. Sans tables techniques intermédiaires.

## Stack

| Technologie | Version cible |
|-------------|---------------|
| Java | 8 (`--release 8`) |
| Maven | 3.8+ |
| JAX-WS / JAXB | javax.* (jaxws-rt 2.3.5) |
| PostgreSQL | 12+ |
| HikariCP | 4.0.3 |
| SLF4J / Logback | 1.7 / 1.2 |
| JUnit / Mockito | 4.13 / 4.11 |

## Démarrage rapide

```bash
# 1. Base de test
sudo -u postgres psql -c "CREATE USER evaloc WITH PASSWORD 'evaloc';"
sudo -u postgres psql -c "CREATE DATABASE gesloc OWNER evaloc;"
export DB_USERNAME=evaloc DB_PASSWORD=evaloc
bash src/main/resources/sql/04_load_volumes.sh 1000

# 2. Compiler / tester
mvn clean test

# 3. Lancer l'OS SOAP
mvn -q package -DskipTests
java -cp "target/evaloc-soap-parallel-1.0.0-SNAPSHOT.jar:target/lib/*" fr.evaloc.Application
```

WSDL : [http://localhost:8080/ws/declarations?wsdl](http://localhost:8080/ws/declarations?wsdl)

## Documentation

- [ARCHITECTURE.md](ARCHITECTURE.md)
- [SPECIFICATIONS.md](SPECIFICATIONS.md)
- [INSTALLATION.md](INSTALLATION.md)
- [PERFORMANCE.md](PERFORMANCE.md)

## Modes

| Propriété | Effet |
|-----------|--------|
| `parallel.enabled=false` | Une requête SQL unique (référence) |
| `parallel.enabled=true` | N partitions (1/2/4/8, défaut 4) |

## Hypothèses à valider

1. Collation PostgreSQL `C` sur `idFiche` pour aligner l’ordre SQL et `String.compareTo` Java.
2. Les percentiles `percentile_disc` sur les fiches éligibles donnent des partitions suffisamment équilibrées.
3. Les index existants sur `fiche(idFiche)` et `localSurveille(idFiche, idLocal)` suffisent ; aucune modification automatique des tables métiers hors environnement de POC.

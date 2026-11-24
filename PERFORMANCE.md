# Performance — méthode et limites

> **Important** : ce document ne contient **aucun résultat inventé**.  
> Les chiffres doivent être produits sur votre environnement via le harness et `EXPLAIN`.

## Scénarios à comparer

1. Requête SQL unique (`parallel.enabled=false`)
2. 2 / 4 / 8 partitions parallèles
3. Parallélisme natif PostgreSQL (`max_parallel_workers_per_gather`) si disponible — mesure côté DBA
4. Concurrence client SOAP : 1, 5, 10, 20 appels simultanés

## Métriques

| Métrique | Source |
|----------|--------|
| avg / min / max / p50 / p95 / p99 | `PerformanceHarness` |
| Timeouts | compteur harness + logs `EVALOC_002` |
| Connexions actives | HikariCP JMX / logs pool |
| CPU / mémoire | `pidstat`, `top`, outils OS |
| Plan SQL | `src/main/resources/sql/03_explain_sample.sql` |

## Lancer le harness

```bash
# Prérequis : base chargée + application non nécessaire (le harness bootstrap le contexte)
mvn -q test-compile
mvn -q exec:java -Dexec.classpathScope=test \
  -Dexec.mainClass=fr.evaloc.perf.PerformanceHarness \
  -Dexec.args="5 20"
```

Arguments : `concurrency iterations`.

Comparer ensuite en changeant `parallel.partitions` et `parallel.enabled` dans `application.properties`.

## Limites documentées

### Pages profondes

Chaque partition lit jusqu’à `offset + taillePage` lignes. Une page très profonde (ex. page 2000) augmente la mémoire et le coût SQL. Mitigation possible future : keyset pagination (hors périmètre POC).

### Modifications concurrentes

Le COUNT et la page sont obtenus dans des requêtes / partitions distinctes. Des écritures concurrentes peuvent produire un léger décalage COUNT ↔ contenu de page. Acceptable pour le POC ; à cadrer en production.

### Timeout JDBC

`CompletableFuture.cancel(true)` **ne garantit pas** l’arrêt d’une requête PostgreSQL. Le POC tente `Statement.cancel()` ; le driver / serveur peut encore finaliser le cancel après le Fault SOAP.

### DAC 3 secondes

Le budget interne (`os.timeout.ms=2500`) laisse une marge avant le timeout DAC. Valider sur volumes 50k / 100k avec concurrence réelle avant conclusions.

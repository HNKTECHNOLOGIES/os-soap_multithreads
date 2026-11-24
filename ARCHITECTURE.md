# Architecture — evaloc-soap-parallel

## Vue d'ensemble

```
Client SOAP (DAC / SoapUI)
        |
        v
DeclarationWebServiceImpl  (JAX-WS Endpoint.publish)
        |
        v
DeclarationService  ---- parallel.enabled ----+
        |                                     |
        v                                     v
SequentialDeclarationService     ParallelDeclarationService
        |                                     |
        |                    PartitionService (bornes en cache)
        |                                     |
        +----------> DeclarationDAOImpl <-----+
                           |
                      HikariCP pool
                           |
                      PostgreSQL GESLOC
```

## Packages

| Package | Rôle |
|---------|------|
| `fr.evaloc` | Bootstrap `Application` |
| `config` | DataSource, Executor, assemblage manuel |
| `ws` | SEI + impl SOAP + Faults |
| `ws.generated` | Classes JAXB/JAX-WS générées (wsimport) |
| `service` | Orchestration séquentielle / parallèle |
| `dao` | JDBC, SQL builder, registre Statement.cancel |
| `dto` | Objets métier |
| `mapper` | Fusion / pagination / mapping SOAP |
| `concurrent` | Partition + calcul des bornes |
| `exception` | Exceptions OS + codes EVALOC_00x |
| `util` | Properties, corrélation, budget temps |

## Parallélisme

- `ThreadPoolExecutor` **partagé** (créé une fois), jamais `ForkJoinPool.commonPool()`.
- Chaque tâche obtient sa **propre** connexion JDBC via HikariCP.
- `CompletableFuture.supplyAsync(..., executor)` + `allOf().get(remaining, MILLISECONDS)`.
- En timeout : `Statement.cancel()` via registre + `future.cancel(true)` (sans garantie d’arrêt JDBC).

## Partitionnement

Bornes calculées une fois (TTL configurable) via `percentile_disc` sur `fiche.idFiche` des fiches éligibles (`typeFiche='4' AND etat<>'6'`).

Exemple 4 partitions / bornes B1,B2,B3 :

- P0 : `idFiche < B1`
- P1 : `idFiche >= B1 AND idFiche < B2`
- P2 : `idFiche >= B2 AND idFiche < B3`
- P3 : `idFiche >= B3`

## Pagination globale

Chaque partition récupère au plus `offset + taillePage` lignes. Fusion ordonnée + dédoublonnage `idFiche` + slice global. Le COUNT par partition est additionné.

## Adaptation WAR

Profil Maven `-Pwar` préparé pour un packaging WAR futur sur serveur d’applications. Le POC courant reste un JAR autonome avec `Endpoint.publish`.

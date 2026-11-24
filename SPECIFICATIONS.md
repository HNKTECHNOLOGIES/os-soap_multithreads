# Spécifications fonctionnelles — OS SOAP déclarations

## Opération

- **Nom** : `rechercherDeclarations`
- **Style** : document/literal, Contract First (`declaration.wsdl` + `declaration.xsd`)
- **Synchronisme** : réponse SOAP synchrone

## Critères (tous facultatifs)

| Champ | Description |
|-------|-------------|
| idFiche | Identifiant fiche (VARCHAR) |
| idDeclaFonciere | Déclaration foncière |
| cdDept / cdCommune | Localisation |
| invariant / idLocal | Local |
| etats | Liste d’états |
| typesLocaux | Liste de types de locaux |
| critereTri / sensTri | Tri (liste blanche) |
| numeroPage / taillePage | Pagination |

Filtres métier fixes (non surchargeables) :

- `f.typeFiche = '4'`
- `f.etat <> '6'`
- `DISTINCT ON (l.idFiche)` avec `ORDER BY l.idFiche, l.idLocal, f.dtCreation DESC`

## Réponse

- Liste des déclarations (champs métier listés dans le cahier des charges)
- `nombreTotal`, `nombrePages`, `numeroPage`, `taillePage`

## Codes SOAP Fault

| Code | Cause |
|------|--------|
| EVALOC_001 | Critères invalides |
| EVALOC_002 | Timeout OS / DAC |
| EVALOC_003 | Erreur SQL |
| EVALOC_004 | Erreur technique |
| EVALOC_005 | Saturation pool threads |

Aucune stack trace n’est exposée au client.

## Timeouts

| Paramètre | Défaut | Rôle |
|-----------|--------|------|
| `os.timeout.ms` | 2500 | Budget interne OS |
| `os.dac.timeout.ms` | 3000 | Référence DAC (documentation / supervision) |
| `statement_timeout` PG | temps restant | Timeout session SQL |

En cas de timeout : annulation des tâches, tentative `Statement.cancel()`, **pas de résultats partiels**, SOAP Fault EVALOC_002.

## Règles non inventées

Toute règle métier hors cahier des charges est signalée comme **hypothèse à valider** (collation, équilibre des percentiles, index additionnels).

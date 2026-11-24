-- Analyse EXPLAIN de la requête de référence (à exécuter manuellement)
EXPLAIN (ANALYZE, BUFFERS)
SELECT DISTINCT ON (l.idFiche)
       l.idDeclaFonciere,
       f.etat,
       l.cdDept,
       l.cdCommune,
       l.cdPrefix,
       l.cdSection,
       l.noPlan,
       l.dtChangement,
       l.typeLocal,
       l.idLocal,
       l.invariant,
       f.idFiche,
       f.dtCreation
FROM glccurlschema.localSurveille l
JOIN glccurlschema.fiche f
     ON f.idFiche = l.idFiche
WHERE f.typeFiche = '4'
  AND f.etat <> '6'
ORDER BY
    l.idFiche,
    l.idLocal,
    f.dtCreation DESC
LIMIT 50;

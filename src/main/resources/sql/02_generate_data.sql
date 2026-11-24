-- Génération de données synthétiques.
-- Variable : :nb_fiches (passer via psql -v nb_fiches=1000)
-- Exemple : psql -d gesloc -v nb_fiches=10000 -f 02_generate_data.sql

\set ON_ERROR_STOP on

TRUNCATE glccurlschema.localSurveille CASCADE;
TRUNCATE glccurlschema.fiche CASCADE;

-- Insère :nb_fiches fiches typeFiche=4 (éligibles) + 5% typeFiche=autres / etat=6
INSERT INTO glccurlschema.fiche (idFiche, typeFiche, etat, dtCreation)
SELECT
    -- Identifiants alphanumériques non numériques (pas d'hypothèse sur le préfixe)
    'F' || lpad(to_hex(i), 8, '0') || '-' || substr(md5(i::text), 1, 8) AS idFiche,
    CASE WHEN i % 20 = 0 THEN '3' ELSE '4' END AS typeFiche,
    CASE
        WHEN i % 20 = 0 THEN '6'
        WHEN i % 7 = 0 THEN '5'
        WHEN i % 5 = 0 THEN '3'
        WHEN i % 3 = 0 THEN '2'
        ELSE '1'
    END AS etat,
    TIMESTAMP '2020-01-01' + ((i % 1500) || ' days')::interval AS dtCreation
FROM generate_series(1, :nb_fiches) AS s(i);

-- 1 à 4 locaux par fiche (plusieurs locaux / même fiche pour tester DISTINCT ON)
INSERT INTO glccurlschema.localSurveille (
    idLocal, idFiche, idDeclaFonciere, cdDept, cdCommune, cdPrefix, cdSection,
    noPlan, dtChangement, typeLocal, invariant
)
SELECT
    'L' || lpad(to_hex(f.ord * 10 + n), 8, '0') AS idLocal,
    f.idFiche,
    'DF' || lpad((f.ord % 100000)::text, 6, '0') AS idDeclaFonciere,
    lpad(((f.ord % 95) + 1)::text, 2, '0') AS cdDept,
    lpad(((f.ord % 900) + 1)::text, 3, '0') AS cdCommune,
    lpad((n)::text, 3, '0') AS cdPrefix,
    chr(65 + ((f.ord % 26)::int)) || chr(65 + (((f.ord / 26) % 26)::int)) AS cdSection,
    lpad(((f.ord + n) % 9999)::text, 4, '0') AS noPlan,
    f.dtCreation + (n || ' hours')::interval AS dtChangement,
    CASE n WHEN 1 THEN 'AP' WHEN 2 THEN 'MA' WHEN 3 THEN 'DEP' ELSE 'AUT' END AS typeLocal,
    'INV' || lpad((f.ord % 50000)::text, 8, '0') AS invariant
FROM (
    SELECT idFiche, dtCreation,
           row_number() OVER (ORDER BY idFiche) AS ord
    FROM glccurlschema.fiche
) f
CROSS JOIN generate_series(1, 1 + (f.ord % 4)) AS g(n);

ANALYZE glccurlschema.fiche;
ANALYZE glccurlschema.localSurveille;

SELECT
    (SELECT count(*) FROM glccurlschema.fiche) AS nb_fiches,
    (SELECT count(*) FROM glccurlschema.fiche WHERE typeFiche = '4' AND etat <> '6') AS nb_fiches_eligibles,
    (SELECT count(*) FROM glccurlschema.localSurveille) AS nb_locaux;

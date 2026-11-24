-- Schéma et tables métier GESLOC (données fictives pour le POC)
-- Hypothèse validée localement : collation C pour aligner ORDER BY PostgreSQL et String.compareTo Java.

CREATE SCHEMA IF NOT EXISTS glccurlschema;

DROP TABLE IF EXISTS glccurlschema.localSurveille CASCADE;
DROP TABLE IF EXISTS glccurlschema.fiche CASCADE;

CREATE TABLE glccurlschema.fiche (
    idFiche       VARCHAR(64)  NOT NULL COLLATE "C",
    typeFiche     VARCHAR(8)   NOT NULL,
    etat          VARCHAR(8)   NOT NULL,
    dtCreation    TIMESTAMP    NOT NULL,
    CONSTRAINT pk_fiche PRIMARY KEY (idFiche)
);

CREATE TABLE glccurlschema.localSurveille (
    idLocal          VARCHAR(64)  NOT NULL COLLATE "C",
    idFiche          VARCHAR(64)  NOT NULL COLLATE "C",
    idDeclaFonciere  VARCHAR(64),
    cdDept           VARCHAR(3),
    cdCommune        VARCHAR(5),
    cdPrefix         VARCHAR(5),
    cdSection        VARCHAR(10),
    noPlan           VARCHAR(10),
    dtChangement     TIMESTAMP,
    typeLocal        VARCHAR(8),
    invariant        VARCHAR(32),
    CONSTRAINT pk_local_surveille PRIMARY KEY (idLocal),
    CONSTRAINT fk_local_fiche FOREIGN KEY (idFiche)
        REFERENCES glccurlschema.fiche (idFiche)
);

-- Index métier : PK fiche déjà indexée ; index sur localSurveille pour jointures / filtres
CREATE INDEX idx_local_idfiche ON glccurlschema.localSurveille (idFiche);
CREATE INDEX idx_local_idfiche_idlocal ON glccurlschema.localSurveille (idFiche, idLocal);
CREATE INDEX idx_local_dept_commune ON glccurlschema.localSurveille (cdDept, cdCommune);
CREATE INDEX idx_local_invariant ON glccurlschema.localSurveille (invariant);
CREATE INDEX idx_fiche_type_etat ON glccurlschema.fiche (typeFiche, etat);
CREATE INDEX idx_fiche_type_etat_id ON glccurlschema.fiche (typeFiche, etat, idFiche);

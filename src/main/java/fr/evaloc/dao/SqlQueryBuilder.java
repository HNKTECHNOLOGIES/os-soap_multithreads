package fr.evaloc.dao;

import fr.evaloc.concurrent.Partition;
import fr.evaloc.dto.SearchCriteria;

import java.util.ArrayList;
import java.util.List;

/**
 * Construit dynamiquement les requêtes SQL (PreparedStatement) en respectant
 * la logique {@code DISTINCT ON (l.idFiche)} et la position des filtres.
 * <p>
 * Les filtres sur {@code fiche} (typeFiche, etat, idFiche, partition) sont appliqués
 * dans le WHERE interne. Les filtres sur {@code localSurveille} sont également
 * dans le WHERE interne ; le DISTINCT ON conserve une ligne par idFiche selon
 * l'ORDER BY métier de référence.
 */
public final class SqlQueryBuilder {

    private final String schema;

    public SqlQueryBuilder(String schema) {
        this.schema = schema == null ? "glccurlschema" : schema;
    }

    /**
     * Requête de recherche avec DISTINCT ON, bornes de partition et limite haute
     * pour la pagination globale (offset + pageSize).
     */
    public BuiltQuery buildSearchQuery(SearchCriteria criteria,
                                       Partition partition,
                                       int fetchLimit,
                                       long statementTimeoutMs) {
        List<Object> params = new ArrayList<Object>();
        StringBuilder sql = new StringBuilder();

        sql.append("SELECT DISTINCT ON (l.idFiche) ")
                .append("l.idDeclaFonciere, f.etat, l.cdDept, l.cdCommune, l.cdPrefix, ")
                .append("l.cdSection, l.noPlan, l.dtChangement, l.typeLocal, l.idLocal, ")
                .append("l.invariant, f.idFiche, f.dtCreation ")
                .append("FROM ").append(schema).append(".localSurveille l ")
                .append("JOIN ").append(schema).append(".fiche f ON f.idFiche = l.idFiche ")
                .append("WHERE f.typeFiche = '4' AND f.etat <> '6' ");

        appendPartitionPredicates(sql, params, partition);
        appendCriteriaPredicates(sql, params, criteria);

        // ORDER BY métier DISTINCT ON : idFiche, idLocal, dtCreation DESC
        // Puis re-tri applicatif global après fusion.
        sql.append("ORDER BY l.idFiche, l.idLocal, f.dtCreation DESC ");

        if (fetchLimit > 0) {
            sql.append("LIMIT ? ");
            params.add(Integer.valueOf(fetchLimit));
        }

        return new BuiltQuery(sql.toString(), params, statementTimeoutMs);
    }

    /**
     * Comptage des idFiche distincts respectant les mêmes critères métier.
     */
    public BuiltQuery buildCountQuery(SearchCriteria criteria,
                                      Partition partition,
                                      long statementTimeoutMs) {
        List<Object> params = new ArrayList<Object>();
        StringBuilder sql = new StringBuilder();

        sql.append("SELECT COUNT(*) FROM (")
                .append("SELECT DISTINCT ON (l.idFiche) l.idFiche ")
                .append("FROM ").append(schema).append(".localSurveille l ")
                .append("JOIN ").append(schema).append(".fiche f ON f.idFiche = l.idFiche ")
                .append("WHERE f.typeFiche = '4' AND f.etat <> '6' ");

        appendPartitionPredicates(sql, params, partition);
        appendCriteriaPredicates(sql, params, criteria);

        sql.append("ORDER BY l.idFiche, l.idLocal, f.dtCreation DESC")
                .append(") AS distinct_fiches");

        return new BuiltQuery(sql.toString(), params, statementTimeoutMs);
    }

    private void appendPartitionPredicates(StringBuilder sql,
                                           List<Object> params,
                                           Partition partition) {
        if (partition == null) {
            return;
        }
        if (partition.getLowerInclusive() != null) {
            sql.append("AND f.idFiche >= ? ");
            params.add(partition.getLowerInclusive());
        }
        if (partition.getUpperExclusive() != null) {
            sql.append("AND f.idFiche < ? ");
            params.add(partition.getUpperExclusive());
        }
    }

    private void appendCriteriaPredicates(StringBuilder sql,
                                          List<Object> params,
                                          SearchCriteria criteria) {
        if (criteria == null) {
            return;
        }
        if (notBlank(criteria.getIdFiche())) {
            sql.append("AND f.idFiche = ? ");
            params.add(criteria.getIdFiche().trim());
        }
        if (notBlank(criteria.getIdDeclaFonciere())) {
            sql.append("AND l.idDeclaFonciere = ? ");
            params.add(criteria.getIdDeclaFonciere().trim());
        }
        if (notBlank(criteria.getCdDept())) {
            sql.append("AND l.cdDept = ? ");
            params.add(criteria.getCdDept().trim());
        }
        if (notBlank(criteria.getCdCommune())) {
            sql.append("AND l.cdCommune = ? ");
            params.add(criteria.getCdCommune().trim());
        }
        if (notBlank(criteria.getInvariant())) {
            sql.append("AND l.invariant = ? ");
            params.add(criteria.getInvariant().trim());
        }
        if (notBlank(criteria.getIdLocal())) {
            sql.append("AND l.idLocal = ? ");
            params.add(criteria.getIdLocal().trim());
        }
        appendInList(sql, params, "f.etat", criteria.getEtats());
        appendInList(sql, params, "l.typeLocal", criteria.getTypesLocaux());
    }

    private static void appendInList(StringBuilder sql,
                                     List<Object> params,
                                     String column,
                                     List<String> values) {
        if (values == null || values.isEmpty()) {
            return;
        }
        List<String> cleaned = new ArrayList<String>();
        for (String v : values) {
            if (notBlank(v)) {
                cleaned.add(v.trim());
            }
        }
        if (cleaned.isEmpty()) {
            return;
        }
        sql.append("AND ").append(column).append(" IN (");
        for (int i = 0; i < cleaned.size(); i++) {
            if (i > 0) {
                sql.append(", ");
            }
            sql.append("?");
            params.add(cleaned.get(i));
        }
        sql.append(") ");
    }

    private static boolean notBlank(String s) {
        return s != null && !s.trim().isEmpty();
    }

    /**
     * Requête SQL prête à être bindée.
     */
    public static final class BuiltQuery {
        private final String sql;
        private final List<Object> parameters;
        private final long statementTimeoutMs;

        public BuiltQuery(String sql, List<Object> parameters, long statementTimeoutMs) {
            this.sql = sql;
            this.parameters = parameters;
            this.statementTimeoutMs = statementTimeoutMs;
        }

        public String getSql() {
            return sql;
        }

        public List<Object> getParameters() {
            return parameters;
        }

        public long getStatementTimeoutMs() {
            return statementTimeoutMs;
        }
    }
}

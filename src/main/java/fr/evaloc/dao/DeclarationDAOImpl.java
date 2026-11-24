package fr.evaloc.dao;

import fr.evaloc.concurrent.Partition;
import fr.evaloc.dto.DeclarationDTO;
import fr.evaloc.dto.SearchCriteria;
import fr.evaloc.exception.OsDatabaseException;
import fr.evaloc.exception.OsTimeoutException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.SQLTimeoutException;
import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

/**
 * Implémentation JDBC du DAO : PreparedStatement, try-with-resources,
 * connexion dédiée par appel (jamais partagée entre threads).
 */
public class DeclarationDAOImpl implements DeclarationDAO {

    private static final Logger LOG = LoggerFactory.getLogger(DeclarationDAOImpl.class);

    private final DataSource dataSource;
    private final SqlQueryBuilder queryBuilder;
    private final CancellableStatementRegistry statementRegistry;

    public DeclarationDAOImpl(DataSource dataSource,
                              String schema,
                              CancellableStatementRegistry statementRegistry) {
        this.dataSource = dataSource;
        this.queryBuilder = new SqlQueryBuilder(schema);
        this.statementRegistry = statementRegistry;
    }

    @Override
    public List<DeclarationDTO> rechercher(Partition partition,
                                           SearchCriteria criteria,
                                           int fetchLimit,
                                           long statementTimeoutMs,
                                           String cancelKey) {
        SqlQueryBuilder.BuiltQuery query =
                queryBuilder.buildSearchQuery(criteria, partition, fetchLimit, statementTimeoutMs);

        long tConnStart = System.nanoTime();
        try (Connection connection = dataSource.getConnection()) {
            long connMs = (System.nanoTime() - tConnStart) / 1_000_000L;
            applyStatementTimeout(connection, statementTimeoutMs);

            long tSqlStart = System.nanoTime();
            try (PreparedStatement ps = connection.prepareStatement(query.getSql())) {
                if (cancelKey != null) {
                    statementRegistry.register(cancelKey, ps);
                }
                bindParameters(ps, query.getParameters());
                try (ResultSet rs = ps.executeQuery()) {
                    List<DeclarationDTO> results = mapRows(rs);
                    long sqlMs = (System.nanoTime() - tSqlStart) / 1_000_000L;
                    LOG.info("DAO rechercher partition={} thread={} connWaitMs={} sqlMs={} rows={}",
                            partition == null ? "ALL" : partition.getId(),
                            Thread.currentThread().getName(),
                            connMs,
                            sqlMs,
                            results.size());
                    return results;
                }
            } finally {
                if (cancelKey != null) {
                    statementRegistry.unregister(cancelKey);
                }
            }
        } catch (SQLTimeoutException e) {
            throw new OsTimeoutException("Timeout SQL lors de la recherche", e);
        } catch (SQLException e) {
            if (isCancelledOrTimeout(e)) {
                throw new OsTimeoutException("Requête SQL annulée ou timed-out", e);
            }
            throw new OsDatabaseException("Erreur SQL lors de la recherche", e);
        }
    }

    @Override
    public long compter(Partition partition,
                        SearchCriteria criteria,
                        long statementTimeoutMs,
                        String cancelKey) {
        SqlQueryBuilder.BuiltQuery query =
                queryBuilder.buildCountQuery(criteria, partition, statementTimeoutMs);

        long tConnStart = System.nanoTime();
        try (Connection connection = dataSource.getConnection()) {
            long connMs = (System.nanoTime() - tConnStart) / 1_000_000L;
            applyStatementTimeout(connection, statementTimeoutMs);

            long tSqlStart = System.nanoTime();
            try (PreparedStatement ps = connection.prepareStatement(query.getSql())) {
                if (cancelKey != null) {
                    statementRegistry.register(cancelKey, ps);
                }
                bindParameters(ps, query.getParameters());
                try (ResultSet rs = ps.executeQuery()) {
                    long count = 0L;
                    if (rs.next()) {
                        count = rs.getLong(1);
                    }
                    long sqlMs = (System.nanoTime() - tSqlStart) / 1_000_000L;
                    LOG.info("DAO compter partition={} thread={} connWaitMs={} sqlMs={} count={}",
                            partition == null ? "ALL" : partition.getId(),
                            Thread.currentThread().getName(),
                            connMs,
                            sqlMs,
                            count);
                    return count;
                }
            } finally {
                if (cancelKey != null) {
                    statementRegistry.unregister(cancelKey);
                }
            }
        } catch (SQLTimeoutException e) {
            throw new OsTimeoutException("Timeout SQL lors du comptage", e);
        } catch (SQLException e) {
            if (isCancelledOrTimeout(e)) {
                throw new OsTimeoutException("Requête SQL de comptage annulée ou timed-out", e);
            }
            throw new OsDatabaseException("Erreur SQL lors du comptage", e);
        }
    }

    private static void applyStatementTimeout(Connection connection, long statementTimeoutMs)
            throws SQLException {
        if (statementTimeoutMs <= 0) {
            return;
        }
        // Timeout local à la session/transaction PostgreSQL
        try (PreparedStatement ps = connection.prepareStatement(
                "SET LOCAL statement_timeout = " + Math.max(1L, statementTimeoutMs))) {
            // SET LOCAL nécessite une transaction ; à défaut on utilise SET
        } catch (SQLException ignored) {
            // fallback ci-dessous
        }
        try (PreparedStatement ps = connection.prepareStatement(
                "SET statement_timeout TO '" + Math.max(1L, statementTimeoutMs) + "ms'")) {
            ps.execute();
        }
    }

    private static void bindParameters(PreparedStatement ps, List<Object> parameters)
            throws SQLException {
        for (int i = 0; i < parameters.size(); i++) {
            ps.setObject(i + 1, parameters.get(i));
        }
    }

    private static List<DeclarationDTO> mapRows(ResultSet rs) throws SQLException {
        List<DeclarationDTO> list = new ArrayList<DeclarationDTO>();
        while (rs.next()) {
            DeclarationDTO dto = new DeclarationDTO();
            dto.setIdDeclaFonciere(rs.getString("idDeclaFonciere"));
            dto.setEtat(rs.getString("etat"));
            dto.setCdDept(rs.getString("cdDept"));
            dto.setCdCommune(rs.getString("cdCommune"));
            dto.setCdPrefix(rs.getString("cdPrefix"));
            dto.setCdSection(rs.getString("cdSection"));
            dto.setNoPlan(rs.getString("noPlan"));
            dto.setDtChangement(toDate(rs.getTimestamp("dtChangement")));
            dto.setTypeLocal(rs.getString("typeLocal"));
            dto.setIdLocal(rs.getString("idLocal"));
            dto.setInvariant(rs.getString("invariant"));
            dto.setIdFiche(rs.getString("idFiche"));
            dto.setDtCreation(toDate(rs.getTimestamp("dtCreation")));
            list.add(dto);
        }
        return list;
    }

    private static Date toDate(Timestamp ts) {
        return ts == null ? null : new Date(ts.getTime());
    }

    private static boolean isCancelledOrTimeout(SQLException e) {
        String state = e.getSQLState();
        // 57014 = query_canceled (PostgreSQL)
        if ("57014".equals(state)) {
            return true;
        }
        String msg = e.getMessage();
        return msg != null && (msg.toLowerCase().contains("cancel")
                || msg.toLowerCase().contains("timeout")
                || msg.toLowerCase().contains("timed out"));
    }
}

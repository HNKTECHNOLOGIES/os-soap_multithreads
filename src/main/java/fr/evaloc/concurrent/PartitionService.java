package fr.evaloc.concurrent;

import fr.evaloc.exception.OsDatabaseException;
import fr.evaloc.exception.OsValidationException;
import fr.evaloc.util.AppProperties;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.atomic.AtomicReference;

/**
 * Calcule et met en cache des bornes de partitions équilibrées sur {@code idFiche}.
 * Les bornes ne sont pas recalculées à chaque appel SOAP.
 * <p>
 * Hypothèse à valider : les percentiles PostgreSQL ({@code percentile_disc})
 * sur la distribution réelle des {@code idFiche} des fiches éligibles
 * ({@code typeFiche='4'} et {@code etat &lt;&gt; '6'}) produisent des partitions
 * suffisamment équilibrées pour le POC.
 */
public class PartitionService {

    private static final Logger LOG = LoggerFactory.getLogger(PartitionService.class);

    private static final List<Integer> ALLOWED_PARTITION_COUNTS =
            Collections.unmodifiableList(Arrays.asList(1, 2, 4, 8));

    private final DataSource dataSource;
    private final String schema;
    private final int partitionCount;
    private final long ttlMillis;

    private final AtomicReference<CachedBounds> cache = new AtomicReference<CachedBounds>();

    public PartitionService(DataSource dataSource, AppProperties props) {
        this.dataSource = dataSource;
        this.schema = props.get("db.schema", "glccurlschema");
        this.partitionCount = props.getInt("parallel.partitions", 4);
        this.ttlMillis = props.getLong("partition.bounds.ttl.ms", 3_600_000L);
        validatePartitionCount(this.partitionCount);
    }

    public PartitionService(DataSource dataSource, String schema, int partitionCount, long ttlMillis) {
        this.dataSource = dataSource;
        this.schema = schema;
        this.partitionCount = partitionCount;
        this.ttlMillis = ttlMillis;
        validatePartitionCount(this.partitionCount);
    }

    /**
     * Retourne les partitions courantes (depuis le cache si valide).
     */
    public List<Partition> getPartitions() {
        CachedBounds cached = cache.get();
        long now = System.currentTimeMillis();
        if (cached != null && (ttlMillis <= 0 || now - cached.computedAtMillis < ttlMillis)) {
            return cached.partitions;
        }
        synchronized (this) {
            cached = cache.get();
            if (cached != null && (ttlMillis <= 0 || now - cached.computedAtMillis < ttlMillis)) {
                return cached.partitions;
            }
            List<Partition> computed = computePartitions();
            cache.set(new CachedBounds(computed, System.currentTimeMillis()));
            LOG.info("Bornes de partitions recalculées: count={}, partitions={}",
                    computed.size(), computed);
            return computed;
        }
    }

    /**
     * Force le recalcul des bornes (tests / maintenance).
     */
    public void invalidateCache() {
        cache.set(null);
    }

    public int getPartitionCount() {
        return partitionCount;
    }

    /**
     * Construit N partitions disjointes couvrant l'intégralité des idFiche
     * à partir de bornes intermédiaires triées.
     *
     * @param bounds bornes intermédiaires (N-1 valeurs), déjà triées
     */
    public static List<Partition> buildPartitionsFromBounds(List<String> bounds) {
        int n = bounds.size() + 1;
        List<Partition> partitions = new ArrayList<Partition>(n);
        for (int i = 0; i < n; i++) {
            String lower = (i == 0) ? null : bounds.get(i - 1);
            String upper = (i == n - 1) ? null : bounds.get(i);
            partitions.add(new Partition(i, lower, upper));
        }
        return Collections.unmodifiableList(partitions);
    }

    /**
     * Vérifie couverture complète, absence de chevauchement et ordre des bornes.
     */
    public static void assertDisjointAndCovering(List<Partition> partitions) {
        if (partitions == null || partitions.isEmpty()) {
            throw new OsValidationException("Aucune partition définie");
        }
        for (int i = 0; i < partitions.size(); i++) {
            Partition p = partitions.get(i);
            if (i == 0 && p.getLowerInclusive() != null) {
                throw new OsValidationException("La première partition doit avoir lower=null");
            }
            if (i == partitions.size() - 1 && p.getUpperExclusive() != null) {
                throw new OsValidationException("La dernière partition doit avoir upper=null");
            }
            if (i > 0) {
                Partition prev = partitions.get(i - 1);
                if (prev.getUpperExclusive() == null
                        || !prev.getUpperExclusive().equals(p.getLowerInclusive())) {
                    throw new OsValidationException(
                            "Partitions non jointives entre " + prev.getId() + " et " + p.getId());
                }
            }
            if (p.getLowerInclusive() != null && p.getUpperExclusive() != null
                    && p.getLowerInclusive().compareTo(p.getUpperExclusive()) >= 0) {
                throw new OsValidationException(
                        "Borne invalide pour " + p.getId() + ": lower >= upper");
            }
        }
    }

    private List<Partition> computePartitions() {
        if (partitionCount == 1) {
            return buildPartitionsFromBounds(Collections.<String>emptyList());
        }

        List<Double> fractions = new ArrayList<Double>();
        for (int i = 1; i < partitionCount; i++) {
            fractions.add((double) i / (double) partitionCount);
        }

        // percentile_disc sur les idFiche distincts des fiches éligibles
        StringBuilder sql = new StringBuilder();
        sql.append("SELECT ");
        for (int i = 0; i < fractions.size(); i++) {
            if (i > 0) {
                sql.append(", ");
            }
            sql.append("percentile_disc(").append(fractions.get(i))
                    .append(") WITHIN GROUP (ORDER BY idFiche) AS b").append(i);
        }
        sql.append(" FROM ").append(schema).append(".fiche")
                .append(" WHERE typeFiche = '4' AND etat <> '6'");

        try (Connection connection = dataSource.getConnection();
             PreparedStatement ps = connection.prepareStatement(sql.toString());
             ResultSet rs = ps.executeQuery()) {
            if (!rs.next()) {
                LOG.warn("Aucune fiche éligible pour calculer les bornes; partition unique");
                return buildPartitionsFromBounds(Collections.<String>emptyList());
            }
            List<String> bounds = new ArrayList<String>();
            for (int i = 0; i < fractions.size(); i++) {
                String bound = rs.getString(i + 1);
                if (bound != null) {
                    // Déduplication des bornes identiques (peu de valeurs distinctes)
                    if (bounds.isEmpty() || !bound.equals(bounds.get(bounds.size() - 1))) {
                        bounds.add(bound);
                    }
                }
            }
            List<Partition> partitions = buildPartitionsFromBounds(bounds);
            assertDisjointAndCovering(partitions);
            return partitions;
        } catch (SQLException e) {
            throw new OsDatabaseException("Échec du calcul des bornes de partition", e);
        }
    }

    private static void validatePartitionCount(int count) {
        if (!ALLOWED_PARTITION_COUNTS.contains(Integer.valueOf(count))) {
            throw new OsValidationException(
                    "Nombre de partitions invalide: " + count + " (autorisés: 1, 2, 4, 8)");
        }
    }

    private static final class CachedBounds {
        private final List<Partition> partitions;
        private final long computedAtMillis;

        private CachedBounds(List<Partition> partitions, long computedAtMillis) {
            this.partitions = partitions;
            this.computedAtMillis = computedAtMillis;
        }
    }
}

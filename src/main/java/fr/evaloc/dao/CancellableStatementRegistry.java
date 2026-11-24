package fr.evaloc.dao;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

/**
 * Registre des {@link Statement} JDBC actifs pour tenter {@link Statement#cancel()}
 * en cas de timeout. {@code CompletableFuture.cancel(true)} ne suffit pas.
 */
public class CancellableStatementRegistry {

    private static final Logger LOG = LoggerFactory.getLogger(CancellableStatementRegistry.class);

    private final ConcurrentMap<String, Statement> active = new ConcurrentHashMap<String, Statement>();

    public void register(String key, Statement statement) {
        if (key != null && statement != null) {
            active.put(key, statement);
        }
    }

    public void unregister(String key) {
        if (key != null) {
            active.remove(key);
        }
    }

    /**
     * Tente d'annuler toutes les requêtes JDBC actives pour les clés données.
     */
    public void cancelAll(Iterable<String> keys) {
        for (String key : keys) {
            Statement statement = active.get(key);
            if (statement != null) {
                try {
                    statement.cancel();
                    LOG.warn("Statement.cancel() invoqué pour key={}", key);
                } catch (SQLException e) {
                    LOG.warn("Échec Statement.cancel() pour key={}: {}", key, e.getMessage());
                }
            }
        }
    }

    public void cancelAllRegistered() {
        List<String> keys = new ArrayList<String>(active.keySet());
        cancelAll(keys);
    }

    public int size() {
        return active.size();
    }
}

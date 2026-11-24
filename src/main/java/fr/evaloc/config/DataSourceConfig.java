package fr.evaloc.config;

import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;
import fr.evaloc.util.AppProperties;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.sql.DataSource;

/**
 * Configuration du pool JDBC HikariCP (une connexion par thread, jamais partagée).
 */
public final class DataSourceConfig {

    private static final Logger LOG = LoggerFactory.getLogger(DataSourceConfig.class);

    private DataSourceConfig() {
    }

    /**
     * Crée le DataSource HikariCP à partir des propriétés applicatives.
     */
    public static HikariDataSource createDataSource(AppProperties props) {
        HikariConfig config = new HikariConfig();
        config.setJdbcUrl(props.getRequired("db.url"));
        config.setUsername(props.getRequired("db.username"));
        config.setPassword(props.get("db.password", ""));
        config.setMaximumPoolSize(props.getInt("db.pool.maximumSize", 10));
        config.setMinimumIdle(props.getInt("db.pool.minimumIdle", 2));
        config.setConnectionTimeout(props.getLong("db.pool.connectionTimeout", 500L));
        config.setPoolName(props.get("db.pool.poolName", "evaloc-hikari"));
        config.setAutoCommit(true);
        // Validité rapide des connexions
        config.setConnectionTestQuery("SELECT 1");

        LOG.info("Initialisation HikariCP poolName={}, max={}, minIdle={}, connectionTimeout={}ms",
                config.getPoolName(),
                config.getMaximumPoolSize(),
                config.getMinimumIdle(),
                config.getConnectionTimeout());

        return new HikariDataSource(config);
    }

    public static void closeQuietly(DataSource dataSource) {
        if (dataSource instanceof HikariDataSource) {
            HikariDataSource hikari = (HikariDataSource) dataSource;
            if (!hikari.isClosed()) {
                hikari.close();
            }
        }
    }
}

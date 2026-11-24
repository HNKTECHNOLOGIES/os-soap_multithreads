package fr.evaloc.config;

import com.zaxxer.hikari.HikariDataSource;
import fr.evaloc.concurrent.PartitionService;
import fr.evaloc.dao.CancellableStatementRegistry;
import fr.evaloc.dao.DeclarationDAO;
import fr.evaloc.dao.DeclarationDAOImpl;
import fr.evaloc.mapper.SoapDeclarationMapper;
import fr.evaloc.service.DeclarationService;
import fr.evaloc.service.DeclarationServiceFactory;
import fr.evaloc.util.AppProperties;
import fr.evaloc.ws.DeclarationWebService;
import fr.evaloc.ws.DeclarationWebServiceImpl;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.concurrent.ThreadPoolExecutor;

/**
 * Assemblage manuel des composants (pas de Spring).
 */
public final class ApplicationContext implements AutoCloseable {

    private static final Logger LOG = LoggerFactory.getLogger(ApplicationContext.class);

    private final AppProperties properties;
    private final HikariDataSource dataSource;
    private final ThreadPoolExecutor executor;
    private final CancellableStatementRegistry statementRegistry;
    private final PartitionService partitionService;
    private final DeclarationDAO declarationDAO;
    private final DeclarationService declarationService;
    private final DeclarationWebService declarationWebService;

    public ApplicationContext(AppProperties properties) {
        this.properties = properties;
        this.dataSource = DataSourceConfig.createDataSource(properties);
        this.executor = ExecutorConfig.createExecutor(properties);
        this.statementRegistry = new CancellableStatementRegistry();
        String schema = properties.get("db.schema", "glccurlschema");
        this.partitionService = new PartitionService(dataSource, properties);
        this.declarationDAO = new DeclarationDAOImpl(dataSource, schema, statementRegistry);
        this.declarationService = DeclarationServiceFactory.create(
                properties, declarationDAO, partitionService, executor, statementRegistry);
        this.declarationWebService = new DeclarationWebServiceImpl(
                declarationService, new SoapDeclarationMapper(), properties);
        LOG.info("ApplicationContext initialisé (parallel.enabled={})",
                properties.getBoolean("parallel.enabled", true));
    }

    public static ApplicationContext bootstrap() {
        return new ApplicationContext(AppProperties.loadDefault());
    }

    public AppProperties getProperties() {
        return properties;
    }

    public DeclarationWebService getDeclarationWebService() {
        return declarationWebService;
    }

    public DeclarationService getDeclarationService() {
        return declarationService;
    }

    public DeclarationDAO getDeclarationDAO() {
        return declarationDAO;
    }

    public PartitionService getPartitionService() {
        return partitionService;
    }

    public HikariDataSource getDataSource() {
        return dataSource;
    }

    public ThreadPoolExecutor getExecutor() {
        return executor;
    }

    public String soapEndpointUrl() {
        String host = properties.get("soap.host", "localhost");
        int port = properties.getInt("soap.port", 8080);
        String context = properties.get("soap.context", "/ws/declarations");
        return "http://" + host + ":" + port + context;
    }

    @Override
    public void close() {
        LOG.info("Arrêt ApplicationContext");
        ExecutorConfig.shutdownQuietly(executor);
        DataSourceConfig.closeQuietly(dataSource);
    }
}

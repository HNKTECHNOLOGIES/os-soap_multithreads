package fr.evaloc.service;

import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;
import fr.evaloc.concurrent.PartitionService;
import fr.evaloc.dao.CancellableStatementRegistry;
import fr.evaloc.dao.DeclarationDAO;
import fr.evaloc.dao.DeclarationDAOImpl;
import fr.evaloc.dto.DeclarationDTO;
import fr.evaloc.dto.SearchCriteria;
import fr.evaloc.dto.SearchResult;
import fr.evaloc.mapper.DeclarationResultMerger;
import fr.evaloc.support.DbTestSupport;
import fr.evaloc.util.TimeBudget;
import org.junit.After;
import org.junit.Assume;
import org.junit.Before;
import org.junit.Test;

import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;

/**
 * Sur jeu de données stable, séquentiel et parallèle doivent produire
 * le même nombre total et la même page (mêmes idFiche dans le même ordre).
 */
public class ModeEquivalenceIntegrationTest {

    private HikariDataSource dataSource;
    private ThreadPoolExecutor executor;
    private SequentialDeclarationService sequential;
    private ParallelDeclarationService parallel;

    @Before
    public void setUp() {
        Assume.assumeTrue("PostgreSQL gesloc requis", DbTestSupport.isDatabaseAvailable());
        HikariConfig config = new HikariConfig();
        config.setJdbcUrl(DbTestSupport.URL);
        config.setUsername(DbTestSupport.USER);
        config.setPassword(DbTestSupport.PASSWORD);
        config.setMaximumPoolSize(8);
        dataSource = new HikariDataSource(config);

        CancellableStatementRegistry registry = new CancellableStatementRegistry();
        DeclarationDAO dao = new DeclarationDAOImpl(dataSource, "glccurlschema", registry);
        CriteriaValidator validator = new CriteriaValidator(50, 200);
        DeclarationResultMerger merger = new DeclarationResultMerger();
        sequential = new SequentialDeclarationService(dao, validator, merger);

        PartitionService partitionService = new PartitionService(
                dataSource, "glccurlschema", 4, 0L);
        executor = new ThreadPoolExecutor(4, 4, 60, TimeUnit.SECONDS,
                new ArrayBlockingQueue<Runnable>(8));
        parallel = new ParallelDeclarationService(
                dao, partitionService, executor, validator, merger, registry);
    }

    @After
    public void tearDown() {
        if (executor != null) {
            executor.shutdownNow();
        }
        if (dataSource != null) {
            dataSource.close();
        }
    }

    @Test
    public void sequentialAndParallel_samePageAndTotal() {
        SearchCriteria criteria = new SearchCriteria();
        criteria.setNumeroPage(1);
        criteria.setTaillePage(25);
        criteria.setCritereTri("idFiche");
        criteria.setSensTri("ASC");

        SearchResult seq = sequential.rechercherDeclarations(criteria, new TimeBudget(10_000));
        SearchResult par = parallel.rechercherDeclarations(criteria, new TimeBudget(10_000));

        assertThat(par.getNombreTotal(), is(seq.getNombreTotal()));
        assertThat(par.getDeclarations().size(), is(seq.getDeclarations().size()));
        for (int i = 0; i < seq.getDeclarations().size(); i++) {
            DeclarationDTO s = seq.getDeclarations().get(i);
            DeclarationDTO p = par.getDeclarations().get(i);
            assertThat(p.getIdFiche(), is(s.getIdFiche()));
        }
    }
}

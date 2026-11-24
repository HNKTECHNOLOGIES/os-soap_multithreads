package fr.evaloc.dao;

import fr.evaloc.concurrent.Partition;
import fr.evaloc.dto.DeclarationDTO;
import fr.evaloc.dto.SearchCriteria;
import fr.evaloc.support.DbTestSupport;
import org.junit.After;
import org.junit.Assume;
import org.junit.Before;
import org.junit.Test;

import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.junit.Assert.assertTrue;

public class DeclarationDAOIntegrationTest {

    private HikariDataSource dataSource;
    private DeclarationDAO dao;

    @Before
    public void setUp() {
        Assume.assumeTrue("PostgreSQL gesloc requis", DbTestSupport.isDatabaseAvailable());
        HikariConfig config = new HikariConfig();
        config.setJdbcUrl(DbTestSupport.URL);
        config.setUsername(DbTestSupport.USER);
        config.setPassword(DbTestSupport.PASSWORD);
        config.setMaximumPoolSize(4);
        dataSource = new HikariDataSource(config);
        dao = new DeclarationDAOImpl(dataSource, "glccurlschema", new CancellableStatementRegistry());
    }

    @After
    public void tearDown() {
        if (dataSource != null) {
            dataSource.close();
        }
    }

    @Test
    public void rechercher_returnsDistinctIdFiche() {
        List<DeclarationDTO> rows = dao.rechercher(
                null, new SearchCriteria(), 100, 5000, "it-search");
        Set<String> ids = new HashSet<String>();
        for (DeclarationDTO row : rows) {
            assertTrue(ids.add(row.getIdFiche()));
        }
        long count = dao.compter(null, new SearchCriteria(), 5000, "it-count");
        assertTrue(count >= rows.size());
    }

    @Test
    public void partition_bounds_filterRows() {
        Partition p = new Partition(0, null, "M");
        List<DeclarationDTO> rows = dao.rechercher(p, new SearchCriteria(), 50, 5000, "it-p0");
        for (DeclarationDTO row : rows) {
            assertTrue(row.getIdFiche().compareTo("M") < 0);
        }
        assertThat(rows.isEmpty() || rows.get(0).getIdFiche() != null, is(true));
    }
}

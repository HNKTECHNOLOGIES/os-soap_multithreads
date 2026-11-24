package fr.evaloc.dao;

import fr.evaloc.concurrent.Partition;
import fr.evaloc.dto.SearchCriteria;
import org.junit.Test;

import java.util.Arrays;

import static org.hamcrest.CoreMatchers.containsString;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.junit.Assert.assertFalse;

public class SqlQueryBuilderTest {

    private final SqlQueryBuilder builder = new SqlQueryBuilder("glccurlschema");

    @Test
    public void buildSearchQuery_usesPreparedParametersAndPartitionBounds() {
        SearchCriteria criteria = new SearchCriteria();
        criteria.setCdDept("75");
        criteria.setEtats(Arrays.asList("1", "2"));

        Partition partition = new Partition(1, "B1", "B2");
        SqlQueryBuilder.BuiltQuery query = builder.buildSearchQuery(criteria, partition, 100, 2000);

        assertThat(query.getSql(), containsString("DISTINCT ON (l.idFiche)"));
        assertThat(query.getSql(), containsString("f.idFiche >= ?"));
        assertThat(query.getSql(), containsString("f.idFiche < ?"));
        assertThat(query.getSql(), containsString("l.cdDept = ?"));
        assertFalse(query.getSql().contains("'75'"));
        // lower, upper, dept, 2 etats, LIMIT = 6
        assertThat(query.getParameters().size(), is(6));
    }

    @Test
    public void buildCountQuery_wrapsDistinctOn() {
        SqlQueryBuilder.BuiltQuery query = builder.buildCountQuery(new SearchCriteria(), null, 1000);
        assertThat(query.getSql(), containsString("COUNT(*)"));
        assertThat(query.getSql(), containsString("DISTINCT ON (l.idFiche)"));
    }
}

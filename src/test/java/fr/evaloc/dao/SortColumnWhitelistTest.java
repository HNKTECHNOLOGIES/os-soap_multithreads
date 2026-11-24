package fr.evaloc.dao;

import fr.evaloc.exception.OsValidationException;
import org.junit.Test;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.junit.Assert.fail;

public class SortColumnWhitelistTest {

    @Test
    public void resolveColumn_defaultsToIdFiche() {
        assertThat(SortColumnWhitelist.resolveColumn(null), is("f.idFiche"));
    }

    @Test
    public void resolveColumn_rejectsUnknown() {
        try {
            SortColumnWhitelist.resolveColumn("drop table");
            fail();
        } catch (OsValidationException expected) {
            assertThat(expected.getFaultCode(), is("EVALOC_001"));
        }
    }

    @Test
    public void resolveDirection_acceptsAscDesc() {
        assertThat(SortColumnWhitelist.resolveDirection("desc"), is("DESC"));
    }
}

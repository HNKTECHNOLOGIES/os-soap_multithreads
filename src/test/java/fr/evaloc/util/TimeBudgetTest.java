package fr.evaloc.util;

import fr.evaloc.exception.OsTimeoutException;
import org.junit.Test;

import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

public class TimeBudgetTest {

    @Test
    public void expiresAndThrows() throws Exception {
        TimeBudget budget = new TimeBudget(30);
        Thread.sleep(40);
        assertTrue(budget.isExpired());
        try {
            budget.requireRemainingMillis("phase");
            fail();
        } catch (OsTimeoutException expected) {
            // ok
        }
    }
}

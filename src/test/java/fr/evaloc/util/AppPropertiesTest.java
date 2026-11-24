package fr.evaloc.util;

import org.junit.Test;

import java.util.Properties;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;

public class AppPropertiesTest {

    @Test
    public void resolve_usesDefaultWhenEnvMissing() {
        assertThat(AppProperties.resolve("user=${MISSING_ENV_XYZ:evaloc}"), is("user=evaloc"));
    }

    @Test
    public void getInt_readsProperty() {
        Properties p = new Properties();
        p.setProperty("parallel.partitions", "4");
        AppProperties props = new AppProperties(p);
        assertThat(props.getInt("parallel.partitions", 1), is(4));
    }
}

package fr.evaloc.concurrent;

import fr.evaloc.exception.OsValidationException;
import org.junit.Test;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.fail;

public class PartitionServiceTest {

    @Test
    public void buildPartitions_onePartition_coversAll() {
        List<Partition> partitions = PartitionService.buildPartitionsFromBounds(
                Collections.<String>emptyList());
        assertThat(partitions.size(), is(1));
        assertNull(partitions.get(0).getLowerInclusive());
        assertNull(partitions.get(0).getUpperExclusive());
        PartitionService.assertDisjointAndCovering(partitions);
    }

    @Test
    public void buildPartitions_fourPartitions_disjointAndCovering() {
        List<String> bounds = Arrays.asList("B1", "B2", "B3");
        List<Partition> partitions = PartitionService.buildPartitionsFromBounds(bounds);
        assertThat(partitions.size(), is(4));

        assertNull(partitions.get(0).getLowerInclusive());
        assertThat(partitions.get(0).getUpperExclusive(), is("B1"));

        assertThat(partitions.get(1).getLowerInclusive(), is("B1"));
        assertThat(partitions.get(1).getUpperExclusive(), is("B2"));

        assertThat(partitions.get(2).getLowerInclusive(), is("B2"));
        assertThat(partitions.get(2).getUpperExclusive(), is("B3"));

        assertThat(partitions.get(3).getLowerInclusive(), is("B3"));
        assertNull(partitions.get(3).getUpperExclusive());

        PartitionService.assertDisjointAndCovering(partitions);
    }

    @Test
    public void assertDisjoint_detectsOverlap() {
        List<Partition> bad = Arrays.asList(
                new Partition(0, null, "M"),
                new Partition(1, "A", null)
        );
        try {
            PartitionService.assertDisjointAndCovering(bad);
            fail("expected OsValidationException");
        } catch (OsValidationException expected) {
            // ok
        }
    }
}

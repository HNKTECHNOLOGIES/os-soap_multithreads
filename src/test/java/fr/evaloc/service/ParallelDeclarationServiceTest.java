package fr.evaloc.service;

import fr.evaloc.concurrent.Partition;
import fr.evaloc.concurrent.PartitionService;
import fr.evaloc.dao.CancellableStatementRegistry;
import fr.evaloc.dao.DeclarationDAO;
import fr.evaloc.dto.DeclarationDTO;
import fr.evaloc.dto.SearchCriteria;
import fr.evaloc.dto.SearchResult;
import fr.evaloc.exception.OsOverloadException;
import fr.evaloc.exception.OsTimeoutException;
import fr.evaloc.mapper.DeclarationResultMerger;
import fr.evaloc.util.TimeBudget;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.mockito.invocation.InvocationOnMock;
import org.mockito.stubbing.Answer;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.junit.Assert.fail;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class ParallelDeclarationServiceTest {

    private ThreadPoolExecutor executor;
    private DeclarationDAO dao;
    private PartitionService partitionService;
    private ParallelDeclarationService service;

    @Before
    public void setUp() {
        executor = new ThreadPoolExecutor(2, 2, 60, TimeUnit.SECONDS,
                new ArrayBlockingQueue<Runnable>(2));
        dao = mock(DeclarationDAO.class);
        partitionService = mock(PartitionService.class);
        when(partitionService.getPartitions()).thenReturn(Arrays.asList(
                new Partition(0, null, "M"),
                new Partition(1, "M", null)
        ));
        service = new ParallelDeclarationService(
                dao,
                partitionService,
                executor,
                new CriteriaValidator(50, 200),
                new DeclarationResultMerger(),
                new CancellableStatementRegistry());
    }

    @After
    public void tearDown() {
        executor.shutdownNow();
    }

    @Test
    public void parallel_mergesWithoutDuplicates() {
        when(dao.compter(any(Partition.class), any(SearchCriteria.class), anyLong(), anyString()))
                .thenReturn(2L);
        when(dao.rechercher(any(Partition.class), any(SearchCriteria.class), anyInt(), anyLong(), anyString()))
                .thenAnswer(new Answer<List<DeclarationDTO>>() {
                    @Override
                    public List<DeclarationDTO> answer(InvocationOnMock invocation) {
                        Partition p = (Partition) invocation.getArguments()[0];
                        if (p.getIndex() == 0) {
                            return Collections.singletonList(dto("A"));
                        }
                        return Collections.singletonList(dto("B"));
                    }
                });

        SearchResult result = service.rechercherDeclarations(new SearchCriteria(), new TimeBudget(2000));
        assertThat(result.getNombreTotal(), is(4L));
        assertThat(result.getDeclarations().size(), is(2));
    }

    @Test
    public void timeout_returnsOsTimeoutException() {
        when(dao.compter(any(Partition.class), any(SearchCriteria.class), anyLong(), anyString()))
                .thenAnswer(new Answer<Long>() {
                    @Override
                    public Long answer(InvocationOnMock invocation) throws Throwable {
                        Thread.sleep(500);
                        return 1L;
                    }
                });
        when(dao.rechercher(any(Partition.class), any(SearchCriteria.class), anyInt(), anyLong(), anyString()))
                .thenReturn(Collections.singletonList(dto("X")));

        try {
            service.rechercherDeclarations(new SearchCriteria(), new TimeBudget(50));
            fail("expected timeout");
        } catch (OsTimeoutException expected) {
            assertThat(expected.getFaultCode(), is("EVALOC_002"));
        }
    }

    @Test
    public void overload_whenPoolSaturated() {
        ThreadPoolExecutor tiny = new ThreadPoolExecutor(1, 1, 60, TimeUnit.SECONDS,
                new ArrayBlockingQueue<Runnable>(1),
                new ThreadPoolExecutor.AbortPolicy());
        // Saturate: 1 running + 1 queued
        tiny.execute(new Runnable() {
            @Override
            public void run() {
                try {
                    Thread.sleep(2000);
                } catch (InterruptedException ignored) {
                    Thread.currentThread().interrupt();
                }
            }
        });
        tiny.execute(new Runnable() {
            @Override
            public void run() {
                try {
                    Thread.sleep(2000);
                } catch (InterruptedException ignored) {
                    Thread.currentThread().interrupt();
                }
            }
        });

        ParallelDeclarationService overloaded = new ParallelDeclarationService(
                dao, partitionService, tiny, new CriteriaValidator(50, 200),
                new DeclarationResultMerger(), new CancellableStatementRegistry());

        when(partitionService.getPartitions()).thenReturn(Arrays.asList(
                new Partition(0, null, "A"),
                new Partition(1, "A", "B"),
                new Partition(2, "B", null)
        ));

        try {
            overloaded.rechercherDeclarations(new SearchCriteria(), new TimeBudget(1000));
            fail("expected overload");
        } catch (OsOverloadException expected) {
            assertThat(expected.getFaultCode(), is("EVALOC_005"));
        } finally {
            tiny.shutdownNow();
        }
    }

    private static DeclarationDTO dto(String id) {
        DeclarationDTO d = new DeclarationDTO();
        d.setIdFiche(id);
        return d;
    }
}

package fr.evaloc.service;

import fr.evaloc.concurrent.Partition;
import fr.evaloc.concurrent.PartitionService;
import fr.evaloc.dao.CancellableStatementRegistry;
import fr.evaloc.dao.DeclarationDAO;
import fr.evaloc.dto.DeclarationDTO;
import fr.evaloc.dto.SearchCriteria;
import fr.evaloc.dto.SearchResult;
import fr.evaloc.exception.OsOverloadException;
import fr.evaloc.exception.OsTechnicalException;
import fr.evaloc.exception.OsTimeoutException;
import fr.evaloc.mapper.DeclarationResultMerger;
import fr.evaloc.util.CorrelationId;
import fr.evaloc.util.TimeBudget;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.RejectedExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.concurrent.atomic.AtomicLong;
import java.util.function.Supplier;

/**
 * Mode parallèle : partitions disjointes exécutées via {@link CompletableFuture}
 * sur un {@link java.util.concurrent.ThreadPoolExecutor} partagé.
 */
public class ParallelDeclarationService implements DeclarationService {

    private static final Logger LOG = LoggerFactory.getLogger(ParallelDeclarationService.class);

    private final DeclarationDAO declarationDAO;
    private final PartitionService partitionService;
    private final ExecutorService executorService;
    private final CriteriaValidator criteriaValidator;
    private final DeclarationResultMerger merger;
    private final CancellableStatementRegistry statementRegistry;

    public ParallelDeclarationService(DeclarationDAO declarationDAO,
                                      PartitionService partitionService,
                                      ExecutorService executorService,
                                      CriteriaValidator criteriaValidator,
                                      DeclarationResultMerger merger,
                                      CancellableStatementRegistry statementRegistry) {
        this.declarationDAO = declarationDAO;
        this.partitionService = partitionService;
        this.executorService = executorService;
        this.criteriaValidator = criteriaValidator;
        this.merger = merger;
        this.statementRegistry = statementRegistry;
    }

    @Override
    public SearchResult rechercherDeclarations(SearchCriteria criteria, TimeBudget budget) {
        budget.checkNotExpired("validation");
        SearchCriteria validated = criteriaValidator.validateAndNormalize(criteria);

        List<Partition> partitions = partitionService.getPartitions();
        int fetchLimit = validated.getOffset() + validated.getTaillePage();
        final String correlationId = CorrelationId.get();

        LOG.info("Mode PARALLELE partitions={} fetchLimit={} page={}/{}",
                partitions.size(), fetchLimit, validated.getNumeroPage(), validated.getTaillePage());

        List<String> cancelKeys = new ArrayList<String>();
        List<CompletableFuture<PartitionOutcome>> futures =
                new ArrayList<CompletableFuture<PartitionOutcome>>();

        try {
            for (final Partition partition : partitions) {
                final String searchKey = "par-search-" + partition.getId();
                final String countKey = "par-count-" + partition.getId();
                cancelKeys.add(searchKey);
                cancelKeys.add(countKey);

                final long submitNanos = System.nanoTime();
                Supplier<PartitionOutcome> supplier = new Supplier<PartitionOutcome>() {
                    @Override
                    public PartitionOutcome get() {
                        long waitMs = (System.nanoTime() - submitNanos) / 1_000_000L;
                        CorrelationId.set(correlationId);
                        try {
                            LOG.info("Partition {} démarrée sur thread={} waitInPoolMs={}",
                                    partition.getId(), Thread.currentThread().getName(), waitMs);

                            long remainingForCount = budget.requireRemainingMillis(
                                    "partition-count-" + partition.getId());
                            long count = declarationDAO.compter(
                                    partition, validated, remainingForCount, countKey);

                            long remainingForSearch = budget.requireRemainingMillis(
                                    "partition-search-" + partition.getId());
                            List<DeclarationDTO> rows = declarationDAO.rechercher(
                                    partition, validated, fetchLimit, remainingForSearch, searchKey);

                            LOG.info("Partition {} terminée: count={} rows={}",
                                    partition.getId(), count, rows.size());
                            return new PartitionOutcome(partition.getId(), rows, count);
                        } finally {
                            CorrelationId.clear();
                        }
                    }
                };

                try {
                    futures.add(CompletableFuture.supplyAsync(supplier, executorService));
                } catch (RejectedExecutionException e) {
                    cancelFutures(futures, cancelKeys);
                    throw new OsOverloadException("Saturation du pool de threads", e);
                }
            }

            long remaining = budget.requireRemainingMillis("await-partitions");
            try {
                CompletableFuture.allOf(futures.toArray(new CompletableFuture[0]))
                        .get(remaining, TimeUnit.MILLISECONDS);
            } catch (TimeoutException e) {
                cancelFutures(futures, cancelKeys);
                throw new OsTimeoutException(
                        "Timeout en attente des partitions (budget=" + budget.getBudgetMillis()
                                + "ms, écoulé=" + budget.elapsedMillis() + "ms)", e);
            } catch (InterruptedException e) {
                cancelFutures(futures, cancelKeys);
                Thread.currentThread().interrupt();
                throw new OsTimeoutException("Interruption pendant l'attente des partitions", e);
            } catch (ExecutionException e) {
                cancelFutures(futures, cancelKeys);
                throw unwrapExecutionException(e);
            }

            List<List<DeclarationDTO>> partitionRows = new ArrayList<List<DeclarationDTO>>();
            AtomicLong total = new AtomicLong(0L);
            for (CompletableFuture<PartitionOutcome> future : futures) {
                PartitionOutcome outcome = future.join();
                partitionRows.add(outcome.rows);
                total.addAndGet(outcome.count);
            }

            long tMerge = System.nanoTime();
            List<DeclarationDTO> page = merger.mergeSortAndPaginate(partitionRows, validated);
            long mergeMs = (System.nanoTime() - tMerge) / 1_000_000L;

            budget.checkNotExpired("apres-fusion");

            long nombreTotal = total.get();
            int nombrePages = SequentialDeclarationService.computePageCount(
                    nombreTotal, validated.getTaillePage());

            LOG.info("Mode PARALLELE terminé: total={} pageRows={} mergeMs={} elapsedMs={}",
                    nombreTotal, page.size(), mergeMs, budget.elapsedMillis());

            return new SearchResult(page, nombreTotal, nombrePages,
                    validated.getNumeroPage(), validated.getTaillePage());
        } catch (OsTimeoutException e) {
            cancelFutures(futures, cancelKeys);
            throw e;
        }
    }

    private void cancelFutures(List<CompletableFuture<PartitionOutcome>> futures,
                               List<String> cancelKeys) {
        statementRegistry.cancelAll(cancelKeys);
        for (CompletableFuture<PartitionOutcome> future : futures) {
            if (future != null && !future.isDone()) {
                // Attention : cancel(true) n'arrête pas forcément la requête JDBC
                future.cancel(true);
            }
        }
    }

    private static RuntimeException unwrapExecutionException(ExecutionException e) {
        Throwable cause = e.getCause();
        if (cause instanceof RuntimeException) {
            return (RuntimeException) cause;
        }
        if (cause instanceof Error) {
            throw (Error) cause;
        }
        return new OsTechnicalException("Erreur pendant l'exécution parallèle", cause);
    }

    private static final class PartitionOutcome {
        private final String partitionId;
        private final List<DeclarationDTO> rows;
        private final long count;

        private PartitionOutcome(String partitionId, List<DeclarationDTO> rows, long count) {
            this.partitionId = partitionId;
            this.rows = rows;
            this.count = count;
        }
    }
}

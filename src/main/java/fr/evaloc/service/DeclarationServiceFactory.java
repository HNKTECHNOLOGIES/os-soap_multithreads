package fr.evaloc.service;

import fr.evaloc.concurrent.PartitionService;
import fr.evaloc.dao.CancellableStatementRegistry;
import fr.evaloc.dao.DeclarationDAO;
import fr.evaloc.mapper.DeclarationResultMerger;
import fr.evaloc.util.AppProperties;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.concurrent.ExecutorService;

/**
 * Sélectionne le mode séquentiel ou parallèle selon {@code parallel.enabled}.
 */
public final class DeclarationServiceFactory {

    private static final Logger LOG = LoggerFactory.getLogger(DeclarationServiceFactory.class);

    private DeclarationServiceFactory() {
    }

    public static DeclarationService create(AppProperties props,
                                            DeclarationDAO dao,
                                            PartitionService partitionService,
                                            ExecutorService executor,
                                            CancellableStatementRegistry registry) {
        CriteriaValidator validator = new CriteriaValidator(props);
        DeclarationResultMerger merger = new DeclarationResultMerger();
        boolean parallel = props.getBoolean("parallel.enabled", true);
        if (parallel) {
            LOG.info("DéclarationService: mode PARALLELE (partitions={})",
                    props.getInt("parallel.partitions", 4));
            return new ParallelDeclarationService(
                    dao, partitionService, executor, validator, merger, registry);
        }
        LOG.info("DéclarationService: mode SEQUENTIEL");
        return new SequentialDeclarationService(dao, validator, merger);
    }
}

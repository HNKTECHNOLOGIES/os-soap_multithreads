package fr.evaloc.service;

import fr.evaloc.dao.DeclarationDAO;
import fr.evaloc.dto.DeclarationDTO;
import fr.evaloc.dto.SearchCriteria;
import fr.evaloc.dto.SearchResult;
import fr.evaloc.mapper.DeclarationResultMerger;
import fr.evaloc.util.TimeBudget;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Collections;
import java.util.List;

/**
 * Mode séquentiel : une seule requête SQL (partition null = couverture totale).
 */
public class SequentialDeclarationService implements DeclarationService {

    private static final Logger LOG = LoggerFactory.getLogger(SequentialDeclarationService.class);

    private final DeclarationDAO declarationDAO;
    private final CriteriaValidator criteriaValidator;
    private final DeclarationResultMerger merger;

    public SequentialDeclarationService(DeclarationDAO declarationDAO,
                                        CriteriaValidator criteriaValidator,
                                        DeclarationResultMerger merger) {
        this.declarationDAO = declarationDAO;
        this.criteriaValidator = criteriaValidator;
        this.merger = merger;
    }

    @Override
    public SearchResult rechercherDeclarations(SearchCriteria criteria, TimeBudget budget) {
        budget.checkNotExpired("validation");
        SearchCriteria validated = criteriaValidator.validateAndNormalize(criteria);

        int fetchLimit = validated.getOffset() + validated.getTaillePage();
        long timeoutMs = budget.requireRemainingMillis("sql-sequentiel");

        LOG.info("Mode SEQUENTIEL fetchLimit={} timeoutMs={} page={}/{}",
                fetchLimit, timeoutMs, validated.getNumeroPage(), validated.getTaillePage());

        long count = declarationDAO.compter(null, validated, timeoutMs, "seq-count");
        budget.checkNotExpired("apres-count");

        timeoutMs = budget.requireRemainingMillis("sql-sequentiel-search");
        List<DeclarationDTO> rows = declarationDAO.rechercher(
                null, validated, fetchLimit, timeoutMs, "seq-search");

        long tMerge = System.nanoTime();
        merger.sortGlobally(rows, validated);
        List<DeclarationDTO> page = merger.paginate(rows, validated.getOffset(), validated.getTaillePage());
        long mergeMs = (System.nanoTime() - tMerge) / 1_000_000L;

        int nombrePages = computePageCount(count, validated.getTaillePage());
        LOG.info("Mode SEQUENTIEL terminé: total={} pageRows={} mergeMs={} elapsedMs={}",
                count, page.size(), mergeMs, budget.elapsedMillis());

        return new SearchResult(page, count, nombrePages,
                validated.getNumeroPage(), validated.getTaillePage());
    }

    static int computePageCount(long total, int pageSize) {
        if (pageSize <= 0) {
            return 0;
        }
        if (total <= 0) {
            return 0;
        }
        return (int) ((total + pageSize - 1) / pageSize);
    }
}

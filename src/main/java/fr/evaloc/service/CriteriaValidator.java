package fr.evaloc.service;

import fr.evaloc.dao.SortColumnWhitelist;
import fr.evaloc.dto.SearchCriteria;
import fr.evaloc.exception.OsValidationException;
import fr.evaloc.util.AppProperties;

/**
 * Validation des critères SOAP avant exécution SQL.
 */
public class CriteriaValidator {

    private final int defaultPageSize;
    private final int maxPageSize;

    public CriteriaValidator(AppProperties props) {
        this.defaultPageSize = props.getInt("pagination.defaultSize", 50);
        this.maxPageSize = props.getInt("pagination.maxSize", 200);
    }

    public CriteriaValidator(int defaultPageSize, int maxPageSize) {
        this.defaultPageSize = defaultPageSize;
        this.maxPageSize = maxPageSize;
    }

    /**
     * Valide et normalise les critères (pagination, tri).
     */
    public SearchCriteria validateAndNormalize(SearchCriteria input) {
        if (input == null) {
            input = new SearchCriteria();
        }

        if (input.getNumeroPage() <= 0) {
            input.setNumeroPage(1);
        }
        if (input.getTaillePage() <= 0) {
            input.setTaillePage(defaultPageSize);
        }
        if (input.getTaillePage() > maxPageSize) {
            throw new OsValidationException(
                    "taillePage dépasse le maximum autorisé (" + maxPageSize + ")");
        }

        // Valide la liste blanche même si valeurs par défaut
        SortColumnWhitelist.resolveColumn(input.getCritereTri());
        SortColumnWhitelist.resolveDirection(input.getSensTri());

        return input;
    }
}

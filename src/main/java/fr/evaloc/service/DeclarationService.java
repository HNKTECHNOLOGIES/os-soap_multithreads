package fr.evaloc.service;

import fr.evaloc.dto.SearchCriteria;
import fr.evaloc.dto.SearchResult;
import fr.evaloc.util.TimeBudget;

/**
 * Contrat métier de recherche de déclarations (séquentiel ou parallèle).
 */
public interface DeclarationService {

    /**
     * Recherche synchrone respectant le budget temporel OS.
     */
    SearchResult rechercherDeclarations(SearchCriteria criteria, TimeBudget budget);
}

package fr.evaloc.dto;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Réponse métier paginée (avant mapping SOAP).
 */
public class SearchResult {

    private final List<DeclarationDTO> declarations;
    private final long nombreTotal;
    private final int nombrePages;
    private final int numeroPage;
    private final int taillePage;

    public SearchResult(List<DeclarationDTO> declarations,
                        long nombreTotal,
                        int nombrePages,
                        int numeroPage,
                        int taillePage) {
        this.declarations = declarations == null
                ? Collections.<DeclarationDTO>emptyList()
                : Collections.unmodifiableList(new ArrayList<DeclarationDTO>(declarations));
        this.nombreTotal = nombreTotal;
        this.nombrePages = nombrePages;
        this.numeroPage = numeroPage;
        this.taillePage = taillePage;
    }

    public List<DeclarationDTO> getDeclarations() {
        return declarations;
    }

    public long getNombreTotal() {
        return nombreTotal;
    }

    public int getNombrePages() {
        return nombrePages;
    }

    public int getNumeroPage() {
        return numeroPage;
    }

    public int getTaillePage() {
        return taillePage;
    }
}

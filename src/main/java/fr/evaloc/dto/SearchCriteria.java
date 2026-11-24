package fr.evaloc.dto;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Critères facultatifs de recherche SOAP.
 */
public class SearchCriteria {

    private String idFiche;
    private String idDeclaFonciere;
    private String cdDept;
    private String cdCommune;
    private String invariant;
    private String idLocal;
    private List<String> etats = new ArrayList<String>();
    private List<String> typesLocaux = new ArrayList<String>();
    private String critereTri;
    private String sensTri;
    private int numeroPage = 1;
    private int taillePage = 50;

    public String getIdFiche() {
        return idFiche;
    }

    public void setIdFiche(String idFiche) {
        this.idFiche = idFiche;
    }

    public String getIdDeclaFonciere() {
        return idDeclaFonciere;
    }

    public void setIdDeclaFonciere(String idDeclaFonciere) {
        this.idDeclaFonciere = idDeclaFonciere;
    }

    public String getCdDept() {
        return cdDept;
    }

    public void setCdDept(String cdDept) {
        this.cdDept = cdDept;
    }

    public String getCdCommune() {
        return cdCommune;
    }

    public void setCdCommune(String cdCommune) {
        this.cdCommune = cdCommune;
    }

    public String getInvariant() {
        return invariant;
    }

    public void setInvariant(String invariant) {
        this.invariant = invariant;
    }

    public String getIdLocal() {
        return idLocal;
    }

    public void setIdLocal(String idLocal) {
        this.idLocal = idLocal;
    }

    public List<String> getEtats() {
        return etats;
    }

    public void setEtats(List<String> etats) {
        this.etats = etats == null ? new ArrayList<String>() : new ArrayList<String>(etats);
    }

    public List<String> getTypesLocaux() {
        return typesLocaux;
    }

    public void setTypesLocaux(List<String> typesLocaux) {
        this.typesLocaux = typesLocaux == null
                ? new ArrayList<String>()
                : new ArrayList<String>(typesLocaux);
    }

    public String getCritereTri() {
        return critereTri;
    }

    public void setCritereTri(String critereTri) {
        this.critereTri = critereTri;
    }

    public String getSensTri() {
        return sensTri;
    }

    public void setSensTri(String sensTri) {
        this.sensTri = sensTri;
    }

    public int getNumeroPage() {
        return numeroPage;
    }

    public void setNumeroPage(int numeroPage) {
        this.numeroPage = numeroPage;
    }

    public int getTaillePage() {
        return taillePage;
    }

    public void setTaillePage(int taillePage) {
        this.taillePage = taillePage;
    }

    public int getOffset() {
        return Math.max(0, (numeroPage - 1) * taillePage);
    }

    public List<String> getEtatsUnmodifiable() {
        return Collections.unmodifiableList(etats);
    }

    public List<String> getTypesLocauxUnmodifiable() {
        return Collections.unmodifiableList(typesLocaux);
    }
}

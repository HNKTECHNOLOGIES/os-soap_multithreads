package fr.evaloc.dto;

import java.util.Date;
import java.util.Objects;

/**
 * Déclaration métier retournée par l'OS (une ligne après DISTINCT ON idFiche).
 */
public class DeclarationDTO {

    private String idDeclaFonciere;
    private String etat;
    private String cdDept;
    private String cdCommune;
    private String cdPrefix;
    private String cdSection;
    private String noPlan;
    private Date dtChangement;
    private String typeLocal;
    private String idLocal;
    private String invariant;
    private String idFiche;
    private Date dtCreation;

    public String getIdDeclaFonciere() {
        return idDeclaFonciere;
    }

    public void setIdDeclaFonciere(String idDeclaFonciere) {
        this.idDeclaFonciere = idDeclaFonciere;
    }

    public String getEtat() {
        return etat;
    }

    public void setEtat(String etat) {
        this.etat = etat;
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

    public String getCdPrefix() {
        return cdPrefix;
    }

    public void setCdPrefix(String cdPrefix) {
        this.cdPrefix = cdPrefix;
    }

    public String getCdSection() {
        return cdSection;
    }

    public void setCdSection(String cdSection) {
        this.cdSection = cdSection;
    }

    public String getNoPlan() {
        return noPlan;
    }

    public void setNoPlan(String noPlan) {
        this.noPlan = noPlan;
    }

    public Date getDtChangement() {
        return dtChangement;
    }

    public void setDtChangement(Date dtChangement) {
        this.dtChangement = dtChangement;
    }

    public String getTypeLocal() {
        return typeLocal;
    }

    public void setTypeLocal(String typeLocal) {
        this.typeLocal = typeLocal;
    }

    public String getIdLocal() {
        return idLocal;
    }

    public void setIdLocal(String idLocal) {
        this.idLocal = idLocal;
    }

    public String getInvariant() {
        return invariant;
    }

    public void setInvariant(String invariant) {
        this.invariant = invariant;
    }

    public String getIdFiche() {
        return idFiche;
    }

    public void setIdFiche(String idFiche) {
        this.idFiche = idFiche;
    }

    public Date getDtCreation() {
        return dtCreation;
    }

    public void setDtCreation(Date dtCreation) {
        this.dtCreation = dtCreation;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (!(o instanceof DeclarationDTO)) {
            return false;
        }
        DeclarationDTO that = (DeclarationDTO) o;
        return Objects.equals(idFiche, that.idFiche);
    }

    @Override
    public int hashCode() {
        return Objects.hash(idFiche);
    }
}

package fr.evaloc.dao;

import fr.evaloc.concurrent.Partition;
import fr.evaloc.dto.DeclarationDTO;
import fr.evaloc.dto.SearchCriteria;

import java.util.List;

/**
 * Accès JDBC aux déclarations (une connexion par appel de méthode, via le pool).
 */
public interface DeclarationDAO {

    /**
     * Recherche les déclarations d'une partition, limitées à {@code fetchLimit} lignes
     * (typiquement offset + taillePage pour la pagination globale).
     *
     * @param partition         partition idFiche (peut être null = toute la table)
     * @param criteria          critères métier
     * @param fetchLimit        LIMIT SQL (&gt; 0)
     * @param statementTimeoutMs timeout PostgreSQL statement_timeout
     * @param cancelKey         clé d'enregistrement pour Statement.cancel()
     */
    List<DeclarationDTO> rechercher(Partition partition,
                                    SearchCriteria criteria,
                                    int fetchLimit,
                                    long statementTimeoutMs,
                                    String cancelKey);

    /**
     * Compte les idFiche distincts pour une partition et les mêmes critères.
     */
    long compter(Partition partition,
                 SearchCriteria criteria,
                 long statementTimeoutMs,
                 String cancelKey);
}

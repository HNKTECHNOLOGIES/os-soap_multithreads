package fr.evaloc.mapper;

import fr.evaloc.dao.SortColumnWhitelist;
import fr.evaloc.dto.DeclarationDTO;
import fr.evaloc.dto.SearchCriteria;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.Date;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;

/**
 * Fusion ordonnée des résultats de partitions, dédoublonnage sur {@code idFiche},
 * et pagination globale.
 * <p>
 * Hypothèses à valider avec l'équipe métier / DBA :
 * <ul>
 *   <li>Ordre des chaînes Java {@code String.compareTo} équivalent à la collation
 *       PostgreSQL du POC (recommandé : collation {@code C} / {@code POSIX} sur idFiche).</li>
 *   <li>NULL considéré comme inférieur aux valeurs non nulles (aligné sur
 *       {@code NULLS FIRST} implicite ASC en PostgreSQL selon collation).</li>
 *   <li>Les modifications concurrentes peuvent créer une incohérence légère
 *       entre COUNT et page (documenté dans PERFORMANCE.md).</li>
 * </ul>
 */
public class DeclarationResultMerger {

    /**
     * Fusionne les listes de partitions, élimine les doublons idFiche,
     * applique un tri global déterministe, puis pagine.
     *
     * @param partitionResults résultats déjà limités à (offset + pageSize) par partition
     * @param criteria         critères (tri + pagination)
     * @return page demandée (sans métadonnées de total)
     */
    public List<DeclarationDTO> mergeSortAndPaginate(List<List<DeclarationDTO>> partitionResults,
                                                     SearchCriteria criteria) {
        List<DeclarationDTO> merged = mergeAndDeduplicate(partitionResults);
        sortGlobally(merged, criteria);
        return paginate(merged, criteria.getOffset(), criteria.getTaillePage());
    }

    /**
     * Fusion + dédoublonnage (conserve la première occurrence rencontrée).
     */
    public List<DeclarationDTO> mergeAndDeduplicate(List<List<DeclarationDTO>> partitionResults) {
        List<DeclarationDTO> merged = new ArrayList<DeclarationDTO>();
        Set<String> seen = new HashSet<String>();
        if (partitionResults == null) {
            return merged;
        }
        for (List<DeclarationDTO> part : partitionResults) {
            if (part == null) {
                continue;
            }
            for (DeclarationDTO dto : part) {
                if (dto == null || dto.getIdFiche() == null) {
                    continue;
                }
                if (seen.add(dto.getIdFiche())) {
                    merged.add(dto);
                }
            }
        }
        return merged;
    }

    public void sortGlobally(List<DeclarationDTO> list, SearchCriteria criteria) {
        String column = SortColumnWhitelist.resolveColumn(
                criteria == null ? null : criteria.getCritereTri());
        String direction = SortColumnWhitelist.resolveDirection(
                criteria == null ? null : criteria.getSensTri());
        final boolean asc = "ASC".equals(direction);
        Comparator<DeclarationDTO> comparator = buildComparator(column, asc);
        // Départage déterministe sur idFiche
        comparator = comparator.thenComparing(new Comparator<DeclarationDTO>() {
            @Override
            public int compare(DeclarationDTO a, DeclarationDTO b) {
                return compareNullableStrings(a.getIdFiche(), b.getIdFiche(), true);
            }
        });
        Collections.sort(list, comparator);
    }

    public List<DeclarationDTO> paginate(List<DeclarationDTO> sorted, int offset, int pageSize) {
        if (sorted == null || sorted.isEmpty() || pageSize <= 0) {
            return Collections.emptyList();
        }
        int from = Math.max(0, offset);
        if (from >= sorted.size()) {
            return Collections.emptyList();
        }
        int to = Math.min(sorted.size(), from + pageSize);
        return new ArrayList<DeclarationDTO>(sorted.subList(from, to));
    }

    private static Comparator<DeclarationDTO> buildComparator(String column, final boolean asc) {
        final String col = column.toLowerCase(Locale.ROOT);
        return new Comparator<DeclarationDTO>() {
            @Override
            public int compare(DeclarationDTO a, DeclarationDTO b) {
                int cmp;
                if (col.contains("etat")) {
                    cmp = compareNullableStrings(a.getEtat(), b.getEtat(), asc);
                } else if (col.contains("dtcreation")) {
                    cmp = compareNullableDates(a.getDtCreation(), b.getDtCreation(), asc);
                } else if (col.contains("iddeclafonciere")) {
                    cmp = compareNullableStrings(a.getIdDeclaFonciere(), b.getIdDeclaFonciere(), asc);
                } else if (col.contains("cddept")) {
                    cmp = compareNullableStrings(a.getCdDept(), b.getCdDept(), asc);
                } else if (col.contains("cdcommune")) {
                    cmp = compareNullableStrings(a.getCdCommune(), b.getCdCommune(), asc);
                } else if (col.contains("invariant")) {
                    cmp = compareNullableStrings(a.getInvariant(), b.getInvariant(), asc);
                } else if (col.contains("idlocal")) {
                    cmp = compareNullableStrings(a.getIdLocal(), b.getIdLocal(), asc);
                } else if (col.contains("dtchangement")) {
                    cmp = compareNullableDates(a.getDtChangement(), b.getDtChangement(), asc);
                } else if (col.contains("typelocal")) {
                    cmp = compareNullableStrings(a.getTypeLocal(), b.getTypeLocal(), asc);
                } else {
                    cmp = compareNullableStrings(a.getIdFiche(), b.getIdFiche(), asc);
                }
                return cmp;
            }
        };
    }

    /**
     * NULLS FIRST en ASC (NULL &lt; non-null), symétrique en DESC.
     */
    static int compareNullableStrings(String a, String b, boolean asc) {
        if (a == null && b == null) {
            return 0;
        }
        if (a == null) {
            return asc ? -1 : 1;
        }
        if (b == null) {
            return asc ? 1 : -1;
        }
        int cmp = a.compareTo(b);
        return asc ? cmp : -cmp;
    }

    static int compareNullableDates(Date a, Date b, boolean asc) {
        if (a == null && b == null) {
            return 0;
        }
        if (a == null) {
            return asc ? -1 : 1;
        }
        if (b == null) {
            return asc ? 1 : -1;
        }
        int cmp = a.compareTo(b);
        return asc ? cmp : -cmp;
    }
}

package fr.evaloc.service;

import fr.evaloc.dto.DeclarationDTO;
import fr.evaloc.dto.SearchCriteria;
import fr.evaloc.dto.SearchResult;
import fr.evaloc.mapper.DeclarationResultMerger;
import org.junit.Test;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;

/**
 * Vérifie que la logique de fusion parallèle produit le même ordre/page
 * qu'un jeu séquentiel stable (équivalence algorithmique hors DB).
 */
public class SequentialParallelEquivalenceTest {

    @Test
    public void samePageForStableDataset() {
        List<DeclarationDTO> sequential = Arrays.asList(
                dto("A"), dto("B"), dto("C"), dto("D"), dto("E"), dto("F"));

        List<List<DeclarationDTO>> partitions = Arrays.asList(
                Arrays.asList(dto("A"), dto("B"), dto("C")),
                Arrays.asList(dto("D"), dto("E"), dto("F"))
        );

        SearchCriteria criteria = new SearchCriteria();
        criteria.setCritereTri("idFiche");
        criteria.setSensTri("ASC");
        criteria.setNumeroPage(2);
        criteria.setTaillePage(2);

        DeclarationResultMerger merger = new DeclarationResultMerger();
        List<DeclarationDTO> seqSorted = new ArrayList<DeclarationDTO>(sequential);
        merger.sortGlobally(seqSorted, criteria);
        List<DeclarationDTO> seqPage = merger.paginate(seqSorted, criteria.getOffset(), criteria.getTaillePage());
        List<DeclarationDTO> parPage = merger.mergeSortAndPaginate(partitions, criteria);

        assertThat(parPage.size(), is(seqPage.size()));
        for (int i = 0; i < seqPage.size(); i++) {
            assertThat(parPage.get(i).getIdFiche(), is(seqPage.get(i).getIdFiche()));
        }
    }

    private static DeclarationDTO dto(String id) {
        DeclarationDTO d = new DeclarationDTO();
        d.setIdFiche(id);
        return d;
    }
}

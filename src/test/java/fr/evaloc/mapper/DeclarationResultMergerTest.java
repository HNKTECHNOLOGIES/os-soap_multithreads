package fr.evaloc.mapper;

import fr.evaloc.dto.DeclarationDTO;
import fr.evaloc.dto.SearchCriteria;
import org.junit.Test;

import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

public class DeclarationResultMergerTest {

    private final DeclarationResultMerger merger = new DeclarationResultMerger();

    @Test
    public void merge_removesDuplicatesOnIdFiche() {
        DeclarationDTO a1 = dto("F1", "L1");
        DeclarationDTO a2 = dto("F1", "L2");
        DeclarationDTO b1 = dto("F2", "L3");

        List<DeclarationDTO> merged = merger.mergeAndDeduplicate(
                Arrays.asList(Collections.singletonList(a1), Arrays.asList(a2, b1)));

        assertThat(merged.size(), is(2));
        Set<String> ids = new HashSet<String>();
        for (DeclarationDTO d : merged) {
            assertTrue(ids.add(d.getIdFiche()));
        }
    }

    @Test
    public void mergeSortAndPaginate_appliesGlobalPagination() {
        List<DeclarationDTO> p0 = Arrays.asList(dto("A", "1"), dto("C", "1"));
        List<DeclarationDTO> p1 = Arrays.asList(dto("B", "1"), dto("D", "1"));

        SearchCriteria criteria = new SearchCriteria();
        criteria.setCritereTri("idFiche");
        criteria.setSensTri("ASC");
        criteria.setNumeroPage(2);
        criteria.setTaillePage(2);

        List<DeclarationDTO> page = merger.mergeSortAndPaginate(Arrays.asList(p0, p1), criteria);
        assertThat(page.size(), is(2));
        assertEquals("C", page.get(0).getIdFiche());
        assertEquals("D", page.get(1).getIdFiche());
    }

    @Test
    public void nullsSortFirstInAsc() {
        assertThat(DeclarationResultMerger.compareNullableStrings(null, "A", true), is(-1));
        assertThat(DeclarationResultMerger.compareNullableStrings("A", null, true), is(1));
        assertThat(DeclarationResultMerger.compareNullableStrings(null, "A", false), is(1));
    }

    private static DeclarationDTO dto(String idFiche, String idLocal) {
        DeclarationDTO d = new DeclarationDTO();
        d.setIdFiche(idFiche);
        d.setIdLocal(idLocal);
        return d;
    }
}

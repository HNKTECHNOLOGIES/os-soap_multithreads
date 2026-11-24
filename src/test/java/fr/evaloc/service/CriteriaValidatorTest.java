package fr.evaloc.service;

import fr.evaloc.dto.SearchCriteria;
import fr.evaloc.exception.OsValidationException;
import org.junit.Test;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.junit.Assert.fail;

public class CriteriaValidatorTest {

    private final CriteriaValidator validator = new CriteriaValidator(50, 200);

    @Test
    public void normalize_defaultsPagination() {
        SearchCriteria c = validator.validateAndNormalize(new SearchCriteria());
        assertThat(c.getNumeroPage(), is(1));
        assertThat(c.getTaillePage(), is(50));
    }

    @Test
    public void reject_pageSizeTooLarge() {
        SearchCriteria c = new SearchCriteria();
        c.setTaillePage(500);
        try {
            validator.validateAndNormalize(c);
            fail();
        } catch (OsValidationException expected) {
            assertThat(expected.getFaultCode(), is("EVALOC_001"));
        }
    }
}

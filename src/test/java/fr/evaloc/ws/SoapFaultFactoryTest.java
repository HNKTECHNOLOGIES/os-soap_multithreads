package fr.evaloc.ws;

import fr.evaloc.exception.OsTimeoutException;
import fr.evaloc.exception.OsValidationException;
import org.junit.Test;

import javax.xml.ws.soap.SOAPFaultException;

import static org.hamcrest.CoreMatchers.containsString;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.junit.Assert.assertFalse;

public class SoapFaultFactoryTest {

    @Test
    public void validationMapsToEvaloc001() {
        assertThat(SoapFaultFactory.mapExceptionToCode(new OsValidationException("bad")),
                is("EVALOC_001"));
    }

    @Test
    public void timeoutMapsToEvaloc002() {
        SOAPFaultException fault = SoapFaultFactory.toFault(new OsTimeoutException("timeout"));
        assertThat(fault.getFault().getFaultString(), containsString("EVALOC_002"));
        assertFalse(fault.getFault().getFaultString().contains("at fr.evaloc"));
    }
}

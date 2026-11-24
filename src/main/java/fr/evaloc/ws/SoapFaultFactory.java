package fr.evaloc.ws;

import fr.evaloc.exception.OsDatabaseException;
import fr.evaloc.exception.OsException;
import fr.evaloc.exception.OsOverloadException;
import fr.evaloc.exception.OsTechnicalException;
import fr.evaloc.exception.OsTimeoutException;
import fr.evaloc.exception.OsValidationException;
import fr.evaloc.util.CorrelationId;
import fr.evaloc.ws.generated.OsFault;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.xml.namespace.QName;
import javax.xml.soap.SOAPException;
import javax.xml.soap.SOAPFactory;
import javax.xml.soap.SOAPFault;
import javax.xml.ws.soap.SOAPFaultException;

/**
 * Construit des SOAP Fault sans exposer les stack traces au client.
 */
public final class SoapFaultFactory {

    private static final Logger LOG = LoggerFactory.getLogger(SoapFaultFactory.class);
    private static final String NS = "http://evaloc.fr/declarations";

    private SoapFaultFactory() {
    }

    public static SOAPFaultException toFault(Throwable throwable) {
        String code;
        String message;
        if (throwable instanceof OsException) {
            OsException os = (OsException) throwable;
            code = os.getFaultCode();
            message = safeMessage(os.getMessage());
        } else if (throwable instanceof java.util.concurrent.RejectedExecutionException) {
            code = OsOverloadException.CODE;
            message = "Saturation du pool de traitements";
        } else {
            code = OsTechnicalException.CODE;
            message = "Erreur technique";
        }

        LOG.error("SOAP Fault code={} correlationId={} message={}",
                code, CorrelationId.get(), message);

        try {
            SOAPFault fault = SOAPFactory.newInstance().createFault();
            fault.setFaultCode(new QName(NS, code, "evaloc"));
            fault.setFaultString(message + " [" + code + "]"
                    + (CorrelationId.get() != null ? " corr=" + CorrelationId.get() : ""));

            OsFault detail = new OsFault();
            detail.setCode(code);
            detail.setMessage(message);
            detail.setCorrelationId(CorrelationId.get());
            // Detail JAXB optionnel : le faultString suffit pour le POC
            return new SOAPFaultException(fault);
        } catch (SOAPException e) {
            throw new OsTechnicalException("Impossible de créer la SOAP Fault", e);
        }
    }

    public static String mapExceptionToCode(Throwable t) {
        if (t instanceof OsValidationException) {
            return OsValidationException.CODE;
        }
        if (t instanceof OsTimeoutException) {
            return OsTimeoutException.CODE;
        }
        if (t instanceof OsDatabaseException) {
            return OsDatabaseException.CODE;
        }
        if (t instanceof OsOverloadException
                || t instanceof java.util.concurrent.RejectedExecutionException) {
            return OsOverloadException.CODE;
        }
        return OsTechnicalException.CODE;
    }

    private static String safeMessage(String message) {
        if (message == null || message.trim().isEmpty()) {
            return "Erreur OS";
        }
        // Pas de stack trace / SQL brut vers le client
        String cleaned = message.replaceAll("(?i)password[=:].*", "password=***");
        if (cleaned.length() > 500) {
            return cleaned.substring(0, 500);
        }
        return cleaned;
    }
}

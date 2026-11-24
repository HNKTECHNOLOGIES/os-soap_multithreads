package fr.evaloc.exception;

/**
 * Critères de recherche invalides — SOAP Fault {@code EVALOC_001}.
 */
public class OsValidationException extends OsException {

    public static final String CODE = "EVALOC_001";

    public OsValidationException(String message) {
        super(CODE, message);
    }
}

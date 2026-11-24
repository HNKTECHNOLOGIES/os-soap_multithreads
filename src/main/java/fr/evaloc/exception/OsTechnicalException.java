package fr.evaloc.exception;

/**
 * Erreur technique générique — SOAP Fault {@code EVALOC_004}.
 */
public class OsTechnicalException extends OsException {

    public static final String CODE = "EVALOC_004";

    public OsTechnicalException(String message) {
        super(CODE, message);
    }

    public OsTechnicalException(String message, Throwable cause) {
        super(CODE, message, cause);
    }
}

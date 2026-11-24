package fr.evaloc.exception;

/**
 * Erreur SQL / JDBC — SOAP Fault {@code EVALOC_003}.
 */
public class OsDatabaseException extends OsException {

    public static final String CODE = "EVALOC_003";

    public OsDatabaseException(String message) {
        super(CODE, message);
    }

    public OsDatabaseException(String message, Throwable cause) {
        super(CODE, message, cause);
    }
}

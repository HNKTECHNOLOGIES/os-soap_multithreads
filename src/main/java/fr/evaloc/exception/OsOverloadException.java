package fr.evaloc.exception;

/**
 * Saturation du pool de threads — SOAP Fault {@code EVALOC_005}.
 */
public class OsOverloadException extends OsException {

    public static final String CODE = "EVALOC_005";

    public OsOverloadException(String message) {
        super(CODE, message);
    }

    public OsOverloadException(String message, Throwable cause) {
        super(CODE, message, cause);
    }
}

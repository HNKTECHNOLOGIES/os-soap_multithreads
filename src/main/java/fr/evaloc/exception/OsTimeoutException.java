package fr.evaloc.exception;

/**
 * Timeout OS / DAC — SOAP Fault {@code EVALOC_002}.
 */
public class OsTimeoutException extends OsException {

    public static final String CODE = "EVALOC_002";

    public OsTimeoutException(String message) {
        super(CODE, message);
    }

    public OsTimeoutException(String message, Throwable cause) {
        super(CODE, message, cause);
    }
}

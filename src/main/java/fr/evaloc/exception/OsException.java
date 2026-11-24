package fr.evaloc.exception;

/**
 * Exception de base des erreurs OS EVALOC.
 */
public abstract class OsException extends RuntimeException {

    private final String faultCode;

    protected OsException(String faultCode, String message) {
        super(message);
        this.faultCode = faultCode;
    }

    protected OsException(String faultCode, String message, Throwable cause) {
        super(message, cause);
        this.faultCode = faultCode;
    }

    public String getFaultCode() {
        return faultCode;
    }
}

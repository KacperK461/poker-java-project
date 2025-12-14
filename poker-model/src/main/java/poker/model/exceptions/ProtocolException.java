package poker.model.exceptions;

/**
 * Exception thrown when protocol communication errors occur.
 */
public class ProtocolException extends RuntimeException {
    private final String code;

    public ProtocolException(String code, String message) {
        super(message);
        this.code = code;
    }

    public String getCode() {
        return code;
    }
}

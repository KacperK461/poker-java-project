package poker.model.exceptions;

/**
 * Exception thrown for security violations or fraud attempts.
 */
public class SecurityException extends RuntimeException {
    private final String code;

    public SecurityException(String code, String message) {
        super(message);
        this.code = code;
    }

    public String getCode() {
        return code;
    }
}

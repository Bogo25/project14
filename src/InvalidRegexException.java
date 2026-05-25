//Author: Bogomil Iliev
/**
 * Thrown when a regular expression is syntactically invalid.
 */
public class InvalidRegexException extends RuntimeException {
    /**
     * Creates an exception with a detailed validation message.
     *
     * @param message description of the regular expression error
     */
    public InvalidRegexException(String message) {
        super(message);
    }
}

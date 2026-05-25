//Author: Bogomil Iliev
/**
 * Thrown when a regular expression contains symbols outside the allowed alphabet.
 */
public class InvalidAlphabetException extends RuntimeException {
    /**
     * Creates an exception describing the unsupported characters.
     *
     * @param offense characters that are not allowed in the alphabet
     */
    public InvalidAlphabetException(String offense) {
        super("The alphabet can only consist of lowercase latin letters and numbers. " +
              "The offending characters are: '" + offense + "'");
    }
}

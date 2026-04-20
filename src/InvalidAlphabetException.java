public class InvalidAlphabetException extends RuntimeException {
    public InvalidAlphabetException(String offense) {
        super("The alphabet can only consist of lowercase" +
                "latin letters and numbers. The offending characters" +
                "are: '" + offense + "'");
    }
}

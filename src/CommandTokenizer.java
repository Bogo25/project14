//Author: Bogomil Iliev
import java.util.ArrayList;
import java.util.List;

/**
 * Splits a command line into tokens. Supports quoted parameters, for example:
 * save A1 "C:\\Temp\\my automaton.nfa"
 */
public class CommandTokenizer {
    private CommandTokenizer() {
    }

    /**
     * Splits the command line into tokens while preserving text inside double quotes.
     *
     * @param line command line entered by the user
     * @return parsed command tokens
     */
    public static List<String> tokenize(String line) {
        ArrayList<String> tokens = new ArrayList<>();

        if (line == null) {
            return tokens;
        }

        StringBuilder current = new StringBuilder();
        boolean inQuotes = false;
        boolean tokenStarted = false;

        for (int i = 0; i < line.length(); i++) {
            char c = line.charAt(i);

            if (c == '"') {
                inQuotes = !inQuotes;
                tokenStarted = true;
                continue;
            }

            if (Character.isWhitespace(c) && !inQuotes) {
                if (tokenStarted) {
                    tokens.add(current.toString());
                    current.setLength(0);
                    tokenStarted = false;
                }
                continue;
            }

            current.append(c);
            tokenStarted = true;
        }

        if (inQuotes) {
            throw new IllegalArgumentException("Missing closing quote in command.");
        }

        if (tokenStarted) {
            tokens.add(current.toString());
        }

        return tokens;
    }
}

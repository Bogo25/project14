//Author: Bogomil Iliev
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.*;

/**
 * Reads and writes automatons using a simple text format developed for this project.
 */
public class AutomatonFileManager {
    private static final String HEADER = "NFA_PROJECT_FORMAT_V1";

    private AutomatonFileManager() {
    }

    /**
     * Saves an automaton in the project text format.
     *
     * @param automaton automaton to save
     * @param path target file path
     * @throws IOException if the file cannot be written
     */
    public static void save(Automaton automaton, Path path) throws IOException {
        StringBuilder out = new StringBuilder();

        out.append(HEADER).append('\n');
        out.append("regex=").append(automaton.getRegex()).append('\n');
        out.append("alphabet=").append(formatAlphabet(automaton)).append('\n');
        out.append("states\n");

        ArrayList<Automaton.State> states = new ArrayList<>(automaton.getStates());
        states.sort(Comparator.comparingInt(Automaton.State::getId));

        for (Automaton.State state : states) {
            out.append(state.getId())
                    .append(';')
                    .append(state.getType())
                    .append('\n');
        }

        out.append("transitions\n");

        for (Automaton.State state : states) {
            ArrayList<Character> symbols = new ArrayList<>(state.getTransitions().keySet());
            symbols.sort(Comparator.comparingInt(AutomatonFileManager::printableSymbol));

            for (Character symbol : symbols) {
                ArrayList<Automaton.State> targets = new ArrayList<>(state.getTransitions().get(symbol));
                targets.sort(Comparator.comparingInt(Automaton.State::getId));

                for (Automaton.State target : targets) {
                    out.append(state.getId())
                            .append(';')
                            .append(symbol == Automaton.EPSILON ? "/" : symbol.toString())
                            .append(';')
                            .append(target.getId())
                            .append('\n');
                }
            }
        }

        if (path.getParent() != null) {
            Files.createDirectories(path.getParent());
        }
        Files.writeString(path, out.toString());
    }

    /**
     * Loads an automaton from the project text format.
     *
     * @param path source file path
     * @return loaded automaton
     * @throws IOException if the file cannot be read or has invalid format
     */
    public static Automaton open(Path path) throws IOException {
        List<String> lines = Files.readAllLines(path, StandardCharsets.UTF_8);

        if (lines.isEmpty() || !lines.getFirst().trim().equals(HEADER)) {
            throw new IOException("Invalid automaton file format: missing " + HEADER);
        }

        String regex = "";
        HashSet<Character> alphabet = new HashSet<>();
        HashMap<Integer, Automaton.State> statesById = new HashMap<>();
        ArrayList<String> transitionLines = new ArrayList<>();

        Section section = Section.HEADER;

        for (int i = 1; i < lines.size(); i++) {
            String line = lines.get(i).trim();

            if (line.isEmpty() || line.startsWith("#")) {
                continue;
            }

            if (line.equals("states")) {
                section = Section.STATES;
                continue;
            }

            if (line.equals("transitions")) {
                section = Section.TRANSITIONS;
                continue;
            }

            if (section == Section.HEADER) {
                if (line.startsWith("regex=")) {
                    regex = line.substring("regex=".length());
                } else if (line.startsWith("alphabet=")) {
                    alphabet.addAll(parseAlphabet(line.substring("alphabet=".length())));
                } else {
                    throw new IOException("Invalid header line: " + line);
                }
            } else if (section == Section.STATES) {
                String[] parts = line.split(";");
                if (parts.length != 2) {
                    throw new IOException("Invalid state line: " + line);
                }

                int id = Integer.parseInt(parts[0]);
                StateTypes type = StateTypes.valueOf(parts[1]);
                statesById.put(id, new Automaton.State(id, type));
            } else if (section == Section.TRANSITIONS) {
                transitionLines.add(line);
            }
        }

        for (String transitionLine : transitionLines) {
            String[] parts = transitionLine.split(";");
            if (parts.length != 3) {
                throw new IOException("Invalid transition line: " + transitionLine);
            }

            int fromId = Integer.parseInt(parts[0]);
            char symbol = parseSymbol(parts[1]);
            int toId = Integer.parseInt(parts[2]);

            Automaton.State from = statesById.get(fromId);
            Automaton.State to = statesById.get(toId);

            if (from == null || to == null) {
                throw new IOException("Transition references unknown state: " + transitionLine);
            }

            from.addTransition(symbol, to);
        }

        if (statesById.isEmpty()) {
            throw new IOException("Automaton file contains no states.");
        }

        return Automaton.fromLoadedData(new HashSet<>(statesById.values()), alphabet, regex);
    }

    private static int printableSymbol(Character c) {
        return c == Automaton.EPSILON ? '/' : c;
    }

    private static String formatAlphabet(Automaton automaton) {
        ArrayList<Character> chars = new ArrayList<>(automaton.getAlphabet());
        chars.sort(Character::compareTo);

        StringBuilder out = new StringBuilder();
        for (int i = 0; i < chars.size(); i++) {
            if (i > 0) {
                out.append(',');
            }
            out.append(chars.get(i));
        }
        return out.toString();
    }

    private static HashSet<Character> parseAlphabet(String text) throws IOException {
        HashSet<Character> result = new HashSet<>();

        if (text == null || text.isEmpty()) {
            return result;
        }

        String[] parts = text.split(",");
        for (String part : parts) {
            if (part.length() != 1) {
                throw new IOException("Invalid alphabet symbol: " + part);
            }
            result.add(part.charAt(0));
        }

        return result;
    }

    private static char parseSymbol(String text) throws IOException {
        if (text.equals("/")) {
            return Automaton.EPSILON;
        }
        if (text.length() != 1) {
            throw new IOException("Invalid transition symbol: " + text);
        }
        return text.charAt(0);
    }

    private enum Section {
        HEADER,
        STATES,
        TRANSITIONS
    }
}

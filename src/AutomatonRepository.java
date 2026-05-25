//Author: Bogomil Iliev
import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Stores automatons in memory and assigns unique identifiers to them.
 */
public class AutomatonRepository implements ListMethods {
    /**
     * Creates an empty repository for automata.
     */
    public AutomatonRepository() {
    }

    private final LinkedHashMap<String, Automaton> automatons = new LinkedHashMap<>();
    private int nextId = 1;

    @Override
    public void close(Collection<Automaton> automatonsToClose, boolean save) {
        ArrayList<String> idsToRemove = new ArrayList<>();

        for (Map.Entry<String, Automaton> entry : automatons.entrySet()) {
            if (automatonsToClose.contains(entry.getValue())) {
                idsToRemove.add(entry.getKey());
            }
        }

        for (String id : idsToRemove) {
            automatons.remove(id);
        }
    }

    @Override
    public void close() {
        automatons.clear();
    }

    /**
     * Removes one automaton from memory by its identifier.
     *
     * @param id identifier of the automaton to remove
     */
    public void close(String id) {
        if (!automatons.containsKey(id)) {
            throw new IllegalArgumentException("Unknown automaton id: " + id);
        }
        automatons.remove(id);
    }

    @Override
    public void list() {
        System.out.print(formatList());
    }

    /**
     * Builds the table used by the {@code list} command.
     *
     * @return formatted table with all stored automata
     */
    public String formatList() {
        StringBuilder out = new StringBuilder();

        if (automatons.isEmpty()) {
            return "No automatons loaded.\n";
        }

        out.append("+------+------------+--------+---------------+--------+------------+\n");
        out.append("| ID   | States     | Alpha  | Deterministic | Empty  | Regex      |\n");
        out.append("+------+------------+--------+---------------+--------+------------+\n");

        for (Map.Entry<String, Automaton> entry : automatons.entrySet()) {
            String id = entry.getKey();
            Automaton automaton = entry.getValue();

            out.append(String.format(
                    "| %-4s | %-10d | %-6d | %-13s | %-6s | %-6s |%n",
                    id,
                    automaton.getStates().size(),
                    automaton.getAlphabet().size(),
                    automaton.deterministic(),
                    automaton.empty(),
                    shorten(automaton.getRegex(), 10)
            ));
        }

        out.append("+------+------------+--------+---------------+--------+------------+\n");
        return out.toString();
    }

    private String shorten(String text, int maxLength) {
        if (text == null) {
            return "";
        }
        if (text.length() <= maxLength) {
            return text;
        }
        if (maxLength <= 3) {
            return text.substring(0, maxLength);
        }
        return text.substring(0, maxLength - 3) + "...";
    }

    @Override
    public Automaton getAutomaton(String id) {
        Automaton automaton = automatons.get(id);
        if (automaton == null) {
            throw new IllegalArgumentException("Unknown automaton id: " + id);
        }
        return automaton;
    }

    @Override
    public String addAutomaton(Automaton automaton) {
        String id = "A" + nextId++;
        automatons.put(id, automaton);
        return id;
    }

    /**
     * Checks whether the repository contains any automata.
     *
     * @return {@code true} if no automata are currently stored
     */
    public boolean isEmpty() {
        return automatons.isEmpty();
    }
}

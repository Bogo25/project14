//Author: Bogomil Iliev
import java.util.Collection;

/**
 * Operations for a collection of automata loaded in memory.
 */
public interface ListMethods {
    /**
     * Removes the given automata from memory.
     *
     * @param automatons automata that should be removed
     * @param save reserved flag for implementations that may save before closing
     */
    void close(Collection<Automaton> automatons, boolean save);

    /**
     * Removes all automata from memory.
     */
    void close();

    /**
     * Prints a list of all automata currently kept in memory.
     */
    void list();

    /**
     * Finds an automaton by its identifier.
     *
     * @param id identifier assigned by the repository
     * @return automaton with the given identifier
     */
    Automaton getAutomaton(String id);

    /**
     * Adds an automaton to memory and assigns a new identifier to it.
     *
     * @param automaton automaton to store
     * @return generated identifier, for example {@code A1}
     */
    String addAutomaton(Automaton automaton);
}

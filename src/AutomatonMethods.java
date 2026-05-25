//Author: Bogomil Iliev
/**
 * Basic operations supported by a nondeterministic finite automaton.
 */
public interface AutomatonMethods {
    /**
     * Returns a human-readable representation of the automaton.
     *
     * @return table containing states, state types and transitions
     */
    String print();

    /**
     * Checks whether the automaton is deterministic.
     *
     * @return {@code true} if there are no epsilon transitions and no state has more than
     *         one outgoing transition with the same symbol
     */
    boolean deterministic();

    /**
     * Checks whether the language of the automaton is empty.
     *
     * @return {@code true} if no final state is reachable from the start state
     */
    boolean empty();

    /**
     * Checks whether the automaton accepts the given word.
     *
     * @param word word to test; the empty word is represented by the empty string
     * @return {@code true} if the word is accepted by the automaton
     */
    boolean recognize(String word);

    /**
     * Creates a new automaton for the concatenation of this automaton and another one.
     *
     * @param a automaton to concatenate after this automaton
     * @return automaton accepting the concatenated language
     */
    Automaton concat(Automaton a);

    /**
     * Creates a new automaton for the union of this automaton and another one.
     *
     * @param a automaton to unite with this automaton
     * @return automaton accepting words from either language
     */
    Automaton union(Automaton a);

    /**
     * Creates a new automaton for the positive closure of this automaton.
     * Positive closure is also known as Kleene plus and means one or more repetitions.
     *
     * @return automaton accepting one or more repetitions of the original language
     */
    Automaton un();
}

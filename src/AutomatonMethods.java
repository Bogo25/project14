public interface AutomatonMethods {
    String print();
    boolean deterministic();
    boolean empty();
    boolean recognize(String word);

    //Unsure at the moment if it should be in the Automaton class
    Automaton concat(Automaton a);
    Automaton union(Automaton a);
    Automaton un();
}

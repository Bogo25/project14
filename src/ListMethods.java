import java.util.Collection;

public interface ListMethods {
    void close(Collection<Automaton> automatons, boolean save);
    void close();
    void list();
    Automaton getAutomaton(String id);
    String addAutomaton(Automaton automaton);
}

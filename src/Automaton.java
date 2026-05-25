//Author: Bogomil Iliev
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;

/**
 * Represents a nondeterministic finite automaton with epsilon transitions.
 * <p>
 * The constructor {@link #Automaton(String)} builds an automaton from a regular
 * expression using Thompson-style construction. Supported regular expression syntax:
 * lowercase latin letters and digits as symbols, {@code +} for union, {@code *} for
 * Kleene star, parentheses for grouping and {@code /} for the empty word.
 */
public class Automaton implements AutomatonMethods {
    /** Internal representation of an epsilon transition. Printed and saved as '/'. */
    public static final char EPSILON = '\0';

    /**
     * Represents one state of an automaton. Each state has a numeric identifier,
     * a type and a transition map.
     */
    public static class State {
        private static int nextId = 0;

        final int id;
        StateTypes type;
        HashMap<Character, HashSet<State>> transitions = new HashMap<>();

        /**
         * Creates a new state and assigns it the next available identifier.
         *
         * @param type role of the state in the automaton
         */
        public State(StateTypes type) {
            this.id = nextId++;
            this.type = type;
        }

        /**
         * Creates a state with a fixed identifier. This constructor is used when
         * loading an automaton from a file.
         *
         * @param id numeric identifier of the state
         * @param type role of the state in the automaton
         */
        public State(int id, StateTypes type) {
            this.id = id;
            this.type = type;
            if (id >= nextId) {
                nextId = id + 1;
            }
        }

        void addTransition(Character c, State next) {
            transitions.computeIfAbsent(c, _ -> new HashSet<>()).add(next);
        }

        void clearTransitions() {
            transitions.clear();
        }

        /**
         * Returns the numeric identifier of the state.
         *
         * @return state identifier
         */
        public int getId() {
            return id;
        }

        /**
         * Returns the role of the state.
         *
         * @return state type
         */
        public StateTypes getType() {
            return type;
        }

        /**
         * Returns an unmodifiable view of the outgoing transitions.
         *
         * @return transition map where each symbol points to a set of target states
         */
        public Map<Character, HashSet<State>> getTransitions() {
            return Collections.unmodifiableMap(transitions);
        }

        @Override
        public String toString() {
            return "q" + id;
        }
    }

    /** Helper object used while building larger automatons from smaller fragments. */
    private static class Fragment {
        State start;
        State end;
        HashSet<State> states;

        Fragment(State start, State end, HashSet<State> states) {
            this.start = start;
            this.end = end;
            this.states = states;
        }
    }

    private final HashSet<State> states = new HashSet<>();
    private final HashSet<Character> alphabet = new HashSet<>();
    private String regex = "";
    private Boolean deterministic = null;

    /**
     * Creates an automaton from already prepared states.
     *
     * @param states states of the automaton
     * @param alphabet allowed input symbols, without epsilon
     * @param regex regular expression description used for displaying the automaton
     */
    public Automaton(HashSet<State> states, HashSet<Character> alphabet, String regex) {
        this.states.addAll(states);
        this.alphabet.addAll(alphabet);
        this.regex = regex == null ? "" : regex;
    }

    /**
     * Creates an automaton from a regular expression.
     *
     * @param regex regular expression used to construct the automaton
     * @throws InvalidAlphabetException if the expression contains unsupported symbols
     * @throws InvalidRegexException if the expression has invalid syntax
     */
    public Automaton(String regex) throws InvalidAlphabetException, InvalidRegexException {
        setRegex(regex, true);

        RegexParser parser = new RegexParser(this.regex);
        Fragment fragment = parser.parse();

        this.states.addAll(fragment.states);
        markStartAndFinal(fragment.start, fragment.end);
    }

    private Automaton(Fragment fragment, Collection<Character> alphabet, String regex) {
        this.states.addAll(fragment.states);
        this.alphabet.addAll(alphabet);
        this.regex = regex == null ? "" : regex;
        markStartAndFinal(fragment.start, fragment.end);
    }

    /**
     * Builds an automaton from data loaded from a file and validates that it has
     * exactly one start state and at least one final state.
     *
     * @param states states loaded from the file
     * @param alphabet alphabet loaded from the file
     * @param regex textual regular expression description saved in the file
     * @return reconstructed automaton
     */
    public static Automaton fromLoadedData(HashSet<State> states, HashSet<Character> alphabet, String regex) {
        validateLoadedAutomaton(states);
        return new Automaton(states, alphabet, regex);
    }

    private static void validateLoadedAutomaton(HashSet<State> states) {
        int startCount = 0;
        int finalCount = 0;

        for (State state : states) {
            if (isStartType(state.type)) {
                startCount++;
            }
            if (isFinalType(state.type)) {
                finalCount++;
            }
        }

        if (startCount != 1) {
            throw new IllegalArgumentException("Loaded automaton must have exactly one start state.");
        }
        if (finalCount == 0) {
            throw new IllegalArgumentException("Loaded automaton must have at least one final state.");
        }
    }

    private void setRegex(String regex, boolean alphabetFlag)
            throws InvalidAlphabetException, InvalidRegexException {

        if (regex == null || regex.isEmpty()) {
            throw new InvalidRegexException(
                    "Regular expression cannot be empty. Use '/' for the empty string."
            );
        }

        StringBuilder offenses = new StringBuilder();
        int openBrackets = 0;
        char previous = '\0';

        if (alphabetFlag) {
            alphabet.clear();
        }

        for (int i = 0; i < regex.length(); i++) {
            char c = regex.charAt(i);

            if (!isAllowedRegexCharacter(c)) {
                offenses.append(c);
                continue;
            }

            if (c == '(') {
                openBrackets++;
            } else if (c == ')') {
                if (openBrackets == 0) {
                    throw new InvalidRegexException(
                            "Closing parenthesis found without matching opening parenthesis."
                    );
                }
                if (previous == '(' || previous == '+') {
                    throw new InvalidRegexException("Empty expression before ')' is not allowed.");
                }
                openBrackets--;
            } else if (c == '*') {
                if (previous == '\0' || previous == '(' || previous == '+' || previous == '*') {
                    throw new InvalidRegexException(
                            "Kleene star must follow a symbol, '/', or a parenthesized expression."
                    );
                }
            } else if (c == '+') {
                if (previous == '\0' || previous == '(' || previous == '+') {
                    throw new InvalidRegexException("'+' must be placed between two expressions.");
                }
            } else if (alphabetFlag && isSymbol(c)) {
                alphabet.add(c);
            }

            previous = c;
        }

        if (!offenses.isEmpty()) {
            throw new InvalidAlphabetException(offenses.toString());
        }

        if (openBrackets != 0) {
            throw new InvalidRegexException("Too many opening parentheses.");
        }

        if (previous == '+') {
            throw new InvalidRegexException("Regular expression cannot end with '+'.");
        }

        this.regex = regex;
    }

    private static boolean isAllowedRegexCharacter(char c) {
        return isSymbol(c) || c == '+' || c == '*' || c == '(' || c == ')' || c == '/';
    }

    private static boolean isSymbol(char c) {
        return (c >= 'a' && c <= 'z') || (c >= '0' && c <= '9');
    }

    private static boolean isStartType(StateTypes type) {
        return type == StateTypes.START || type == StateTypes.START_FINAL;
    }

    private static boolean isFinalType(StateTypes type) {
        return type == StateTypes.FINAL || type == StateTypes.START_FINAL;
    }

    /** Recursive descent parser for the supported regular expression syntax. */
    private class RegexParser {
        private final String input;
        private int pos = 0;

        RegexParser(String input) {
            this.input = input;
        }

        Fragment parse() {
            Fragment result = parseExpression();

            if (!isAtEnd()) {
                throw new InvalidRegexException(
                        "Unexpected character '" + peek() + "' at position " + pos + "."
                );
            }

            return result;
        }

        /** expression := term ('+' term)* */
        private Fragment parseExpression() {
            Fragment left = parseTerm();

            while (match('+')) {
                if (isAtEnd() || peek() == ')') {
                    throw new InvalidRegexException("Missing expression after '+'.");
                }

                Fragment right = parseTerm();
                left = unionFragments(left, right);
            }

            return left;
        }

        /** term := factor+ */
        private Fragment parseTerm() {
            if (isAtEnd() || peek() == ')' || peek() == '+') {
                throw new InvalidRegexException(
                        "Missing expression at position " + pos + ". Use '/' for the empty string."
                );
            }

            Fragment result = parseFactor();

            while (!isAtEnd() && peek() != ')' && peek() != '+') {
                result = concatFragments(result, parseFactor());
            }

            return result;
        }

        /** factor := base ('*')? */
        private Fragment parseFactor() {
            Fragment base = parseBase();

            if (match('*')) {
                base = starFragment(base);

                if (!isAtEnd() && peek() == '*') {
                    throw new InvalidRegexException(
                            "Kleene star after Kleene star cannot be interpreted."
                    );
                }
            }

            return base;
        }

        /** base := symbol | '/' | '(' expression ')' */
        private Fragment parseBase() {
            if (isAtEnd()) {
                throw new InvalidRegexException("Unexpected end of regular expression.");
            }

            char c = peek();

            if (isSymbol(c)) {
                pos++;
                alphabet.add(c);
                return symbolFragment(c);
            }

            if (c == '/') {
                pos++;
                return epsilonFragment();
            }

            if (c == '(') {
                pos++;

                if (!isAtEnd() && peek() == ')') {
                    throw new InvalidRegexException(
                            "Empty parentheses are not allowed. Use '/' for the empty string."
                    );
                }

                Fragment inner = parseExpression();
                expect(')');
                return inner;
            }

            throw new InvalidRegexException(
                    "Unexpected character '" + c + "' at position " + pos + "."
            );
        }

        private boolean match(char expected) {
            if (!isAtEnd() && input.charAt(pos) == expected) {
                pos++;
                return true;
            }

            return false;
        }

        private void expect(char expected) {
            if (!match(expected)) {
                throw new InvalidRegexException(
                        "Expected '" + expected + "' at position " + pos + "."
                );
            }
        }

        private char peek() {
            return input.charAt(pos);
        }

        private boolean isAtEnd() {
            return pos >= input.length();
        }
    }

    private Fragment symbolFragment(char c) {
        return getFragment(c);
    }

    private Fragment getFragment(char c) {
        State start = new State(StateTypes.MID);
        State end = new State(StateTypes.MID);

        start.addTransition(c, end);

        HashSet<State> fragmentStates = new HashSet<>();
        fragmentStates.add(start);
        fragmentStates.add(end);

        return new Fragment(start, end, fragmentStates);
    }

    private Fragment epsilonFragment() {
        return getFragment(EPSILON);
    }

    private Fragment concatFragments(Fragment first, Fragment second) {
        // Merge first.end with second.start. This avoids unnecessary epsilon transitions
        // and keeps simple expressions such as "ab" deterministic.
        for (Map.Entry<Character, HashSet<State>> entry : second.start.transitions.entrySet()) {
            for (State target : entry.getValue()) {
                first.end.addTransition(entry.getKey(), target);
            }
        }

        HashSet<State> fragmentStates = new HashSet<>(first.states);
        fragmentStates.addAll(second.states);
        fragmentStates.remove(second.start);

        return new Fragment(first.start, second.end, fragmentStates);
    }

    private Fragment unionFragments(Fragment first, Fragment second) {
        State start = new State(StateTypes.MID);
        State end = new State(StateTypes.MID);

        start.addTransition(EPSILON, first.start);
        start.addTransition(EPSILON, second.start);

        first.end.addTransition(EPSILON, end);
        second.end.addTransition(EPSILON, end);

        HashSet<State> fragmentStates = new HashSet<>();
        fragmentStates.add(start);
        fragmentStates.add(end);
        fragmentStates.addAll(first.states);
        fragmentStates.addAll(second.states);

        return new Fragment(start, end, fragmentStates);
    }

    private Fragment starFragment(Fragment inner) {
        State start = new State(StateTypes.MID);
        State end = new State(StateTypes.MID);

        start.addTransition(EPSILON, inner.start);
        start.addTransition(EPSILON, end);

        return getFragment(inner, start, end);
    }

    private Fragment plusFragment(Fragment inner) {
        State start = new State(StateTypes.MID);
        State end = new State(StateTypes.MID);

        start.addTransition(EPSILON, inner.start);

        return getFragment(inner, start, end);
    }

    private Fragment getFragment(Fragment inner, State start, State end) {
        inner.end.addTransition(EPSILON, inner.start);
        inner.end.addTransition(EPSILON, end);

        HashSet<State> fragmentStates = new HashSet<>();
        fragmentStates.add(start);
        fragmentStates.add(end);
        fragmentStates.addAll(inner.states);

        return new Fragment(start, end, fragmentStates);
    }

    private void markStartAndFinal(State start, State end) {
        for (State state : states) {
            state.type = StateTypes.MID;
        }

        if (start == end) {
            start.type = StateTypes.START_FINAL;
        } else {
            start.type = StateTypes.START;
            end.type = StateTypes.FINAL;
        }

        deterministic = null;
    }

    /**
     * Returns the regular expression description of the automaton.
     *
     * @return regular expression text
     */
    public String getRegex() {
        return regex;
    }

    /**
     * Returns all states of the automaton.
     *
     * @return unmodifiable set of states
     */
    public Set<State> getStates() {
        return Collections.unmodifiableSet(states);
    }

    /**
     * Returns the input alphabet of the automaton. Epsilon is not part of the alphabet.
     *
     * @return unmodifiable set of input symbols
     */
    public Set<Character> getAlphabet() {
        return Collections.unmodifiableSet(alphabet);
    }

    /**
     * Finds the start state of the automaton.
     *
     * @return start state
     */
    public State getStartState() {
        for (State state : states) {
            if (isStartType(state.type)) {
                return state;
            }
        }

        throw new IllegalStateException("Automaton has no start state.");
    }

    /**
     * Finds all accepting states of the automaton.
     *
     * @return set of final states
     */
    public HashSet<State> getFinalStates() {
        HashSet<State> result = new HashSet<>();

        for (State state : states) {
            if (isFinalType(state.type)) {
                result.add(state);
            }
        }

        return result;
    }

    private HashSet<State> epsilonClosure(Collection<State> inputStates) {
        HashSet<State> closure = new HashSet<>(inputStates);
        ArrayDeque<State> stack = new ArrayDeque<>(inputStates);

        while (!stack.isEmpty()) {
            State current = stack.pop();

            for (State next : current.transitions.getOrDefault(EPSILON, new HashSet<>())) {
                if (closure.add(next)) {
                    stack.push(next);
                }
            }
        }

        return closure;
    }

    private HashSet<State> move(Collection<State> inputStates, char symbol) {
        HashSet<State> result = new HashSet<>();

        for (State state : inputStates) {
            result.addAll(state.transitions.getOrDefault(symbol, new HashSet<>()));
        }

        return result;
    }

    private Fragment copyAsFragment() {
        HashMap<State, State> copy = new HashMap<>();

        for (State oldState : states) {
            copy.put(oldState, new State(StateTypes.MID));
        }

        for (State oldState : states) {
            State newState = copy.get(oldState);

            for (Map.Entry<Character, HashSet<State>> entry : oldState.transitions.entrySet()) {
                for (State oldTarget : entry.getValue()) {
                    newState.addTransition(entry.getKey(), copy.get(oldTarget));
                }
            }
        }

        HashSet<State> newStates = new HashSet<>(copy.values());
        State newStart = copy.get(getStartState());
        State newEnd = new State(StateTypes.MID);

        newStates.add(newEnd);

        for (State oldFinal : getFinalStates()) {
            copy.get(oldFinal).addTransition(EPSILON, newEnd);
        }

        return new Fragment(newStart, newEnd, newStates);
    }

    @Override
    public boolean deterministic() {
        if (deterministic != null) {
            return deterministic;
        }

        for (State state : states) {
            if (!state.transitions.getOrDefault(EPSILON, new HashSet<>()).isEmpty()) {
                deterministic = false;
                return false;
            }

            for (Map.Entry<Character, HashSet<State>> entry : state.transitions.entrySet()) {
                if (entry.getKey() == EPSILON) {
                    continue;
                }

                if (entry.getValue().size() > 1) {
                    deterministic = false;
                    return false;
                }
            }
        }

        deterministic = true;
        return true;
    }

    @Override
    public boolean empty() {
        HashSet<State> visited = new HashSet<>();
        ArrayDeque<State> queue = new ArrayDeque<>();

        State start = getStartState();

        visited.add(start);
        queue.add(start);

        while (!queue.isEmpty()) {
            State current = queue.remove();

            if (isFinalType(current.type)) {
                return false;
            }

            for (HashSet<State> targets : current.transitions.values()) {
                for (State target : targets) {
                    if (visited.add(target)) {
                        queue.add(target);
                    }
                }
            }
        }

        return true;
    }

    @Override
    public boolean recognize(String word) {
        if (word == null) {
            return false;
        }

        HashSet<State> currentStates = epsilonClosure(Collections.singleton(getStartState()));

        for (int i = 0; i < word.length(); i++) {
            char c = word.charAt(i);

            if (!alphabet.contains(c)) {
                return false;
            }

            currentStates = epsilonClosure(move(currentStates, c));
        }

        for (State state : currentStates) {
            if (isFinalType(state.type)) {
                return true;
            }
        }

        return false;
    }

    @Override
    public Automaton concat(Automaton other) {
        Fragment first = this.copyAsFragment();
        Fragment second = other.copyAsFragment();
        Fragment result = concatFragments(first, second);

        HashSet<Character> resultAlphabet = new HashSet<>(this.alphabet);
        resultAlphabet.addAll(other.alphabet);

        return new Automaton(result, resultAlphabet, "(" + this.regex + ")(" + other.regex + ")");
    }

    @Override
    public Automaton union(Automaton other) {
        Fragment first = this.copyAsFragment();
        Fragment second = other.copyAsFragment();
        Fragment result = unionFragments(first, second);

        HashSet<Character> resultAlphabet = new HashSet<>(this.alphabet);
        resultAlphabet.addAll(other.alphabet);

        return new Automaton(result, resultAlphabet, "(" + this.regex + ")+(" + other.regex + ")");
    }

    @Override
    public Automaton un() {
        Fragment result = plusFragment(this.copyAsFragment());
        return new Automaton(result, this.alphabet, "(" + this.regex + ")+");
    }

    /**
     * Checks whether the language of the automaton is finite.
     * The language is infinite exactly when a productive reachable cycle consumes
     * at least one real alphabet symbol.
     *
     * @return {@code true} if the language contains a finite number of words
     */
    public boolean finite() {
        HashSet<State> reachable = reachableFromStart();
        HashSet<State> canReachFinal = statesThatCanReachFinal();
        HashSet<State> relevant = new HashSet<>(reachable);
        relevant.retainAll(canReachFinal);

        ArrayList<HashSet<State>> components = stronglyConnectedComponents(relevant);

        for (HashSet<State> component : components) {
            if (componentHasPositiveCycle(component)) {
                return false;
            }
        }

        return true;
    }

    private HashSet<State> reachableFromStart() {
        HashSet<State> visited = new HashSet<>();
        ArrayDeque<State> queue = new ArrayDeque<>();

        State start = getStartState();
        visited.add(start);
        queue.add(start);

        while (!queue.isEmpty()) {
            State current = queue.remove();

            for (HashSet<State> targets : current.transitions.values()) {
                for (State target : targets) {
                    if (visited.add(target)) {
                        queue.add(target);
                    }
                }
            }
        }

        return visited;
    }

    private HashSet<State> statesThatCanReachFinal() {
        HashMap<State, HashSet<State>> reverse = new HashMap<>();

        for (State state : states) {
            reverse.put(state, new HashSet<>());
        }

        for (State state : states) {
            for (HashSet<State> targets : state.transitions.values()) {
                for (State target : targets) {
                    reverse.get(target).add(state);
                }
            }
        }

        HashSet<State> visited = new HashSet<>();
        ArrayDeque<State> queue = new ArrayDeque<>();

        for (State finalState : getFinalStates()) {
            visited.add(finalState);
            queue.add(finalState);
        }

        while (!queue.isEmpty()) {
            State current = queue.remove();

            for (State previous : reverse.get(current)) {
                if (visited.add(previous)) {
                    queue.add(previous);
                }
            }
        }

        return visited;
    }

    private ArrayList<HashSet<State>> stronglyConnectedComponents(HashSet<State> allowedStates) {
        ArrayList<HashSet<State>> result = new ArrayList<>();
        HashMap<State, Integer> index = new HashMap<>();
        HashMap<State, Integer> lowlink = new HashMap<>();
        ArrayDeque<State> stack = new ArrayDeque<>();
        HashSet<State> onStack = new HashSet<>();
        int[] nextIndex = new int[] {0};

        for (State state : allowedStates) {
            if (!index.containsKey(state)) {
                tarjan(state, allowedStates, index, lowlink, stack, onStack, nextIndex, result);
            }
        }

        return result;
    }

    private void tarjan(
            State state,
            HashSet<State> allowedStates,
            HashMap<State, Integer> index,
            HashMap<State, Integer> lowlink,
            ArrayDeque<State> stack,
            HashSet<State> onStack,
            int[] nextIndex,
            ArrayList<HashSet<State>> result) {

        index.put(state, nextIndex[0]);
        lowlink.put(state, nextIndex[0]);
        nextIndex[0]++;
        stack.push(state);
        onStack.add(state);

        for (HashSet<State> targets : state.transitions.values()) {
            for (State target : targets) {
                if (!allowedStates.contains(target)) {
                    continue;
                }

                if (!index.containsKey(target)) {
                    tarjan(target, allowedStates, index, lowlink, stack, onStack, nextIndex, result);
                    lowlink.put(state, Math.min(lowlink.get(state), lowlink.get(target)));
                } else if (onStack.contains(target)) {
                    lowlink.put(state, Math.min(lowlink.get(state), index.get(target)));
                }
            }
        }

        if (lowlink.get(state).equals(index.get(state))) {
            HashSet<State> component = new HashSet<>();
            State current;

            do {
                current = stack.pop();
                onStack.remove(current);
                component.add(current);
            } while (current != state);

            result.add(component);
        }
    }

    private boolean componentHasPositiveCycle(HashSet<State> component) {
        boolean hasCycleShape = component.size() > 1;
        boolean hasPositiveInternalEdge = false;

        for (State state : component) {
            for (Map.Entry<Character, HashSet<State>> entry : state.transitions.entrySet()) {
                for (State target : entry.getValue()) {
                    if (!component.contains(target)) {
                        continue;
                    }

                    if (state == target) {
                        hasCycleShape = true;
                    }
                    if (entry.getKey() != EPSILON) {
                        hasPositiveInternalEdge = true;
                    }
                }
            }
        }

        return hasCycleShape && hasPositiveInternalEdge;
    }

    /**
     * Creates a deterministic automaton equivalent to this automaton by subset construction.
     *
     * @return deterministic automaton accepting the same language
     */
    public Automaton determinize() {
        HashSet<Character> sortedAlphabetSet = new HashSet<>(alphabet);
        HashMap<String, State> dfaStatesByKey = new LinkedHashMap<>();
        HashMap<String, HashSet<State>> nfaSetsByKey = new LinkedHashMap<>();
        ArrayDeque<String> queue = new ArrayDeque<>();

        HashSet<State> startSet = epsilonClosure(Collections.singleton(getStartState()));
        String startKey = stateSetKey(startSet);
        State dfaStart = new State(containsFinalState(startSet) ? StateTypes.START_FINAL : StateTypes.START);

        dfaStatesByKey.put(startKey, dfaStart);
        nfaSetsByKey.put(startKey, startSet);
        queue.add(startKey);

        while (!queue.isEmpty()) {
            String currentKey = queue.remove();
            State currentDfaState = dfaStatesByKey.get(currentKey);
            HashSet<State> currentNfaSet = nfaSetsByKey.get(currentKey);

            for (Character symbol : sortedAlphabetSet) {
                HashSet<State> targetNfaSet = epsilonClosure(move(currentNfaSet, symbol));

                if (targetNfaSet.isEmpty()) {
                    continue;
                }

                String targetKey = stateSetKey(targetNfaSet);
                State targetDfaState = dfaStatesByKey.get(targetKey);

                if (targetDfaState == null) {
                    targetDfaState = new State(containsFinalState(targetNfaSet) ? StateTypes.FINAL : StateTypes.MID);
                    dfaStatesByKey.put(targetKey, targetDfaState);
                    nfaSetsByKey.put(targetKey, targetNfaSet);
                    queue.add(targetKey);
                }

                currentDfaState.addTransition(symbol, targetDfaState);
            }
        }

        HashSet<State> resultStates = new HashSet<>(dfaStatesByKey.values());
        return Automaton.fromLoadedData(resultStates, new HashSet<>(alphabet), "det(" + regex + ")");
    }

    private boolean containsFinalState(Collection<State> collection) {
        for (State state : collection) {
            if (isFinalType(state.type)) {
                return true;
            }
        }

        return false;
    }

    private String stateSetKey(HashSet<State> set) {
        ArrayList<State> sortedStates = new ArrayList<>(set);
        sortedStates.sort(Comparator.comparingInt(s -> s.id));

        StringBuilder result = new StringBuilder();

        for (State state : sortedStates) {
            if (!result.isEmpty()) {
                result.append(',');
            }
            result.append(state.id);
        }

        return result.toString();
    }

    @Override
    public String print() {
        ArrayList<State> sortedStates = getSortedStates();
        ArrayList<Character> symbols = getSortedPrintedSymbols(sortedStates);

        ArrayList<String> headers = new ArrayList<>();
        headers.add("State");
        headers.add("Type");

        for (Character c : symbols) {
            headers.add(c == EPSILON ? "/" : c.toString());
        }

        ArrayList<ArrayList<String>> rows = new ArrayList<>();

        for (State state : sortedStates) {
            ArrayList<String> row = new ArrayList<>();

            row.add(state.toString());
            row.add(state.type.toString());

            for (Character c : symbols) {
                row.add(formatTargets(state.transitions.get(c)));
            }

            rows.add(row);
        }

        int[] widths = calculateColumnWidths(headers, rows);

        StringBuilder out = new StringBuilder();

        out.append("Regex: ").append(regex).append('\n');
        out.append("Alphabet: ").append(formatAlphabet()).append('\n');
        out.append("Deterministic: ").append(deterministic()).append('\n');
        out.append("Empty language: ").append(empty()).append('\n');
        out.append("Finite language: ").append(finite()).append('\n');
        out.append('\n');

        appendSeparator(out, widths);
        appendRow(out, headers, widths);
        appendSeparator(out, widths);

        for (ArrayList<String> row : rows) {
            appendRow(out, row, widths);
        }

        appendSeparator(out, widths);

        return out.toString();
    }

    private ArrayList<State> getSortedStates() {
        ArrayList<State> sortedStates = new ArrayList<>(states);
        sortedStates.sort(Comparator.comparingInt(s -> s.id));
        return sortedStates;
    }

    private ArrayList<Character> getSortedPrintedSymbols(ArrayList<State> sortedStates) {
        ArrayList<Character> symbols = new ArrayList<>();

        boolean hasEpsilon = false;
        for (State state : sortedStates) {
            if (state.transitions.containsKey(EPSILON)) {
                hasEpsilon = true;
                break;
            }
        }

        if (hasEpsilon) {
            symbols.add(EPSILON);
        }

        ArrayList<Character> sortedAlphabet = new ArrayList<>(alphabet);
        sortedAlphabet.sort(Character::compareTo);
        symbols.addAll(sortedAlphabet);

        for (State state : sortedStates) {
            for (Character c : state.transitions.keySet()) {
                if (!symbols.contains(c)) {
                    symbols.add(c);
                }
            }
        }

        return symbols;
    }

    private String formatTargets(HashSet<State> targets) {
        if (targets == null || targets.isEmpty()) {
            return "-";
        }

        ArrayList<State> sortedTargets = new ArrayList<>(targets);
        sortedTargets.sort(Comparator.comparingInt(s -> s.id));

        StringBuilder result = new StringBuilder();
        result.append("{");

        for (int i = 0; i < sortedTargets.size(); i++) {
            if (i > 0) {
                result.append(", ");
            }

            result.append(sortedTargets.get(i));
        }

        result.append("}");
        return result.toString();
    }

    private String formatAlphabet() {
        if (alphabet.isEmpty()) {
            return "{}";
        }

        ArrayList<Character> sortedAlphabet = new ArrayList<>(alphabet);
        sortedAlphabet.sort(Character::compareTo);

        StringBuilder result = new StringBuilder();
        result.append("{");

        for (int i = 0; i < sortedAlphabet.size(); i++) {
            if (i > 0) {
                result.append(", ");
            }

            result.append(sortedAlphabet.get(i));
        }

        result.append("}");
        return result.toString();
    }

    private int[] calculateColumnWidths(ArrayList<String> headers, ArrayList<ArrayList<String>> rows) {
        int[] widths = new int[headers.size()];

        for (int i = 0; i < headers.size(); i++) {
            widths[i] = headers.get(i).length();
        }

        for (ArrayList<String> row : rows) {
            for (int i = 0; i < row.size(); i++) {
                widths[i] = Math.max(widths[i], row.get(i).length());
            }
        }

        return widths;
    }

    private void appendSeparator(StringBuilder out, int[] widths) {
        out.append("+");

        for (int width : widths) {
            repeat(out, '-', width + 2);
            out.append("+");
        }

        out.append('\n');
    }

    private void appendRow(StringBuilder out, ArrayList<String> values, int[] widths) {
        out.append("|");

        for (int i = 0; i < values.size(); i++) {
            out.append(" ");
            out.append(padRight(values.get(i), widths[i]));
            out.append(" |");
        }

        out.append('\n');
    }

    private String padRight(String text, int width) {
        StringBuilder result = new StringBuilder(text);
        while (result.length() < width) {
            result.append(' ');
        }
        return result.toString();
    }

    private void repeat(StringBuilder out, char c, int count) {
        out.append(String.valueOf(c).repeat(Math.max(0, count)));
    }
}

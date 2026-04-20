import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;


public class Automaton implements AutomatonMethods {
    static class State {
        StateTypes type;
        //for now Hashmap
        HashMap<Character, HashSet<State>> transitions = new HashMap<>();

        public State(StateTypes type) {
            this.type = type;
        }

        void add_transition(Character c, State next) {
            if (transitions.containsKey(c)) {
                transitions.get(c).add(next);
            } else {
                transitions.put(c, new HashSet<>());
                transitions.get(c).add(next);
            }
        }
    }

    private final HashSet<State> states;
    private final HashSet<Character> alphabet = new HashSet<>();
    private String regex = "";
    private Boolean deterministic = null;

    public Automaton(HashSet<State> states, HashSet<Character> alphabet, String regex) {
        this.states = states;
        this.alphabet.addAll(alphabet);
//        Not needed since this function for now won't be used by the user and user errors can't happen
//        for (Character c: alphabet) if (!regex.contains(c.toString())) ...
        setRegex(regex, false);
    }

    public Automaton(String regex) throws InvalidAlphabetException, InvalidRegexException {
        setRegex(regex, true);
        ArrayList<Object> used_reg = split_regex(regex);
        states = construct_states(used_reg);
    }

    private ArrayList<Object> split_regex(String reg) {
        char[] use = reg.toCharArray();
        ArrayList<Object> ret = new ArrayList<>();

        for (int i = 0; i < use.length; i++) {
            Object element;

            if (use[i] == '(') {
                StringBuilder str = new StringBuilder();
                int parenthesis = 0;
                while (true) {
                    i++;
                    if (use[i] == '(') {
                        str.append(use[i]);
                        parenthesis++;
                    } else if (use[i] == ')') {
                        parenthesis--;
                        if (parenthesis == -1)
                            break;
                        str.append(use[i]);
                    } else {
                        str.append(use[i]);
                    }
                }
                element = split_regex(str.toString());
            } else {
                if (use[i + 1] == '*')
                    element = Character.toUpperCase(use[i]);
                else
                    element = use[i];
            }
            ret.add(element);
        }

        return ret;
    }

    private State getState(State start, State end, State cur, HashSet<State> ret, Character c) {
        if (c.equals('+')) {
            cur.add_transition(c, end);
            cur = start;
        } else {
            if (Character.isUpperCase(c)) {
                cur.add_transition(c, cur);
            } else {
                State temp = new State(StateTypes.MID);
                cur.add_transition(c, temp);
                cur = temp;
                ret.add(cur);
            }
        }
        return cur;
    }

    private HashSet<State> construct_states(ArrayList<Object> reg, State start, State end) {
        HashSet<State> ret = new HashSet<>();
        State cur = start;

        for (Object element : reg) {
            if (element instanceof Character) {
                cur = getState(start, end, cur, ret, (Character) element);
            } else {
                if (reg.getLast().equals(element))
                    ret.addAll(construct_states((ArrayList<Object>) element, cur, end));
                else {
                    State temp = new State(StateTypes.MID);
                    ret.addAll(construct_states((ArrayList<Object>) element, cur, temp));
                    cur = temp;
                }
            }
        }

        return ret;
    }

    private HashSet<State> construct_states(ArrayList<Object> reg) {
        State start = new State(StateTypes.START);
        State end = new State(StateTypes.FINAL);
        State cur = start;
        HashSet<State> ret = new HashSet<>();
        ret.add(start);
        ret.add(end);

        for (Object element : reg) {
            if (element instanceof Character) {
                cur = getState(start, end, cur, ret, (Character) element);
            } else {
                if (reg.getLast().equals(element))
                    ret.addAll(construct_states((ArrayList<Object>) element, cur, end));
                else {
                    State temp = new State(StateTypes.MID);
                    ret.addAll(construct_states((ArrayList<Object>) element, cur, temp));
                    cur = temp;
                    ret.add(cur);
                }
            }
        }

        return ret;
    }

    private void setRegex(String regex, boolean alphabet_flag) throws InvalidAlphabetException, InvalidRegexException {
        StringBuilder offenses = new StringBuilder();
        int open_br = 0;
        boolean flag = false;

        if (regex.charAt(0) == '*' || regex.charAt(0) == '+' || regex.charAt(regex.length() - 1) == '+')
            throw new InvalidRegexException("* can't be at the beginning of the regular expression. + can't" +
                    "be at the end or beginning of the regular expression.");

        for (char c : regex.toCharArray()) {
            if (c == '*') {
                if (flag)
                    throw new InvalidRegexException("Kleene star after Kleene star can't be interpreted");
                flag = true;
            }

            //All allow
            if (!"abcdefghijklmnopqrstuvwxyz+()*/1234567890".contains(Character.toString(c)))
                offenses.append(c);

            if (c == '(')
                open_br++;
            else if (c == ')') {
                if (open_br == 0)
                    throw new InvalidRegexException(
                            "Closing parenthesis found, but no opening parenthesis found for it before that.");
                open_br--;
            } else if (alphabet_flag && c != '+' && c != '*')
                alphabet.add(c);
        }
        if (open_br != 0) throw new InvalidRegexException("Too many open parenthesis.");
        if (offenses.isEmpty()) throw new InvalidAlphabetException(offenses.toString());
        this.regex = regex;
    }

    public String getRegex() {
        return regex;
    }

    public boolean deterministic() {
        if (deterministic == null) {
            for (State s : states) {
                for (HashSet<State> a : s.transitions.values()) {
                    deterministic = a.size() == 1;
                }
            }
        }
        return deterministic;
    }

    @Override
    public String print() {
        int columnSize = 3;
        for (State s : states) {
            for (HashSet<State> a : s.transitions.values()) {
                if (3 + a.size() > columnSize)
            }
        }

        return "";
    }
}
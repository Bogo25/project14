//Author: Bogomil Iliev
/**
 * Describes the role of a state in a finite automaton.
 */
public enum StateTypes {
    /** State that is accepting, but not the initial state. */
    FINAL,

    /** Initial state that is not accepting. */
    START,

    /** State that is both initial and accepting. */
    START_FINAL,

    /** Ordinary state that is neither initial nor accepting. */
    MID
}

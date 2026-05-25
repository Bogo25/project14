//Author: Bogomil Iliev
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;
import java.util.Scanner;

/**
 * Command line interface for working with nondeterministic finite automatons.
 */
public class Application {
    /**
     * Utility class; instances are not needed because all command handling is static.
     */
    private Application() {
    }

    private static final Scanner SCANNER = new Scanner(System.in);
    private static final AutomatonRepository REPOSITORY = new AutomatonRepository();

    /**
     * Starts the command line application. Files passed as command line arguments
     * are opened before the interactive prompt is shown.
     *
     * @param args optional file names to open at startup
     */
    public static void main(String[] args) {
        openFilesFromArguments(args);
        runCommandLoop();
    }

    private static void openFilesFromArguments(String[] args) {
        for (String arg : args) {
            try {
                Automaton automaton = AutomatonFileManager.open(Paths.get(arg));
                String id = REPOSITORY.addAutomaton(automaton);
                System.out.println("Successfully opened " + arg + " as " + id);
            } catch (Exception e) {
                System.out.println("Could not open " + arg + ": " + e.getMessage());
            }
        }
    }

    private static void runCommandLoop() {
        while (true) {
            System.out.print("NFA> ");

            if (!SCANNER.hasNextLine()) {
                System.out.println();
                return;
            }

            String line = SCANNER.nextLine();

            try {
                List<String> input = CommandTokenizer.tokenize(line);

                if (input.isEmpty()) {
                    continue;
                }

                String command = input.getFirst().toLowerCase();

                if (command.equals("exit")) {
                    System.out.println("Exiting the program...");
                    return;
                }

                execute(input);
            } catch (Exception e) {
                System.out.println("Error: " + e.getMessage());
            }
        }
    }

    private static void execute(List<String> input) throws IOException {
        String command = input.getFirst().toLowerCase();

        switch (command) {
            case "help":
                requireArgumentCount(input, 1, "help");
                System.out.println(getHelp());
                break;

            case "reg":
                commandReg(input);
                break;

            case "save":
                commandSave(input);
                break;

            case "open":
                commandOpen(input);
                break;

            case "close":
                commandClose(input);
                break;

            case "list":
                requireArgumentCount(input, 1, "list");
                REPOSITORY.list();
                break;

            case "print":
                commandPrint(input);
                break;

            case "empty":
                commandEmpty(input);
                break;

            case "deterministic":
                commandDeterministic(input);
                break;

            case "recognize":
                commandRecognize(input);
                break;

            case "concat":
                commandConcat(input);
                break;

            case "union":
                commandUnion(input);
                break;

            case "un":
                commandUn(input);
                break;

            case "finite":
                commandFinite(input);
                break;

            case "determinize":
                commandDeterminize(input);
                break;

            default:
                System.out.println("Invalid command. Type 'help' for supported commands.");
        }
    }

    private static void commandReg(List<String> input) {
        requireArgumentCount(input, 2, "reg REGEX");

        Automaton automaton = new Automaton(input.get(1));
        String id = REPOSITORY.addAutomaton(automaton);
        System.out.println("Created automaton " + id);
    }

    private static void commandSave(List<String> input) throws IOException {
        requireArgumentCount(input, 3, "save ID FILE");

        String id = input.get(1);
        Path path = Paths.get(input.get(2));
        Automaton automaton = REPOSITORY.getAutomaton(id);

        AutomatonFileManager.save(automaton, path);
        System.out.println("Successfully saved " + id + " to " + path);
    }

    private static void commandOpen(List<String> input) throws IOException {
        requireMinimumArgumentCount(input, 2, "open FILE [FILE ...]");

        for (int i = 1; i < input.size(); i++) {
            Path path = Paths.get(input.get(i));
            Automaton automaton = AutomatonFileManager.open(path);
            String id = REPOSITORY.addAutomaton(automaton);
            System.out.println("Successfully opened " + path + " as " + id);
        }
    }

    private static void commandClose(List<String> input) {
        requireMinimumArgumentCount(input, 2, "close ID [ID ...] | close *");

        if (input.size() == 2 && input.get(1).equals("*")) {
            REPOSITORY.close();
            System.out.println("Closed all automatons.");
            return;
        }

        for (int i = 1; i < input.size(); i++) {
            REPOSITORY.close(input.get(i));
            System.out.println("Closed " + input.get(i));
        }
    }

    private static void commandPrint(List<String> input) {
        requireArgumentCount(input, 2, "print ID");

        Automaton automaton = REPOSITORY.getAutomaton(input.get(1));
        System.out.print(automaton.print());
    }

    private static void commandEmpty(List<String> input) {
        requireArgumentCount(input, 2, "empty ID");

        Automaton automaton = REPOSITORY.getAutomaton(input.get(1));
        System.out.println(automaton.empty());
    }

    private static void commandDeterministic(List<String> input) {
        requireArgumentCount(input, 2, "deterministic ID");

        Automaton automaton = REPOSITORY.getAutomaton(input.get(1));
        System.out.println(automaton.deterministic());
    }

    private static void commandRecognize(List<String> input) {
        requireArgumentCount(input, 3, "recognize ID WORD");

        Automaton automaton = REPOSITORY.getAutomaton(input.get(1));
        String word = input.get(2);

        if (word.equals("/")) {
            word = "";
        }

        System.out.println(automaton.recognize(word));
    }

    private static void commandConcat(List<String> input) {
        requireMinimumArgumentCount(input, 3, "concat ID ID [ID ...]");

        Automaton result = REPOSITORY.getAutomaton(input.get(1));

        for (int i = 2; i < input.size(); i++) {
            result = result.concat(REPOSITORY.getAutomaton(input.get(i)));
        }

        String id = REPOSITORY.addAutomaton(result);
        System.out.println("Created automaton " + id);
    }

    private static void commandUnion(List<String> input) {
        requireMinimumArgumentCount(input, 3, "union ID ID [ID ...]");

        Automaton result = REPOSITORY.getAutomaton(input.get(1));

        for (int i = 2; i < input.size(); i++) {
            result = result.union(REPOSITORY.getAutomaton(input.get(i)));
        }

        String id = REPOSITORY.addAutomaton(result);
        System.out.println("Created automaton " + id);
    }

    private static void commandUn(List<String> input) {
        requireArgumentCount(input, 2, "un ID");

        Automaton result = REPOSITORY.getAutomaton(input.get(1)).un();
        String id = REPOSITORY.addAutomaton(result);
        System.out.println("Created automaton " + id);
    }

    private static void commandFinite(List<String> input) {
        requireArgumentCount(input, 2, "finite ID");

        Automaton automaton = REPOSITORY.getAutomaton(input.get(1));
        System.out.println(automaton.finite());
    }

    private static void commandDeterminize(List<String> input) {
        requireArgumentCount(input, 2, "determinize ID");

        Automaton result = REPOSITORY.getAutomaton(input.get(1)).determinize();
        String id = REPOSITORY.addAutomaton(result);
        System.out.println("Created deterministic automaton " + id);
    }

    private static void requireArgumentCount(List<String> input, int count, String usage) {
        if (input.size() != count) {
            throw new IllegalArgumentException("Usage: " + usage);
        }
    }

    private static void requireMinimumArgumentCount(List<String> input, int count, String usage) {
        if (input.size() < count) {
            throw new IllegalArgumentException("Usage: " + usage);
        }
    }

    /**
     * Loads the help text from the {@code src/help} file. If the file is missing,
     * a built-in fallback help text is returned.
     *
     * @return help text displayed by the {@code help} command
     */
    public static String getHelp() {
        Path helpPath = Paths.get(System.getProperty("user.dir"), "src", "help");

        if (Files.exists(helpPath)) {
            try {
                return new String(Files.readAllBytes(helpPath));
            } catch (IOException ignored) {
                // Fallback below.
            }
        }

        return """
                Usage: nfa [FILE ...]
                
                Options:
                    help                    Print this help text and exit
                    reg REGEX               Creates an automaton based on the regular expression
                    save ID FILE            Save the automaton in the file
                    open FILE [FILE ...]    Load the listed automatons
                    close ID [ID ...]       Remove automatons from memory
                    close *                 Remove all automatons from memory
                    list                    List all automatons in memory
                    print ID                Print all transitions of said automaton
                    empty ID                Check if the automaton's language is empty
                    deterministic ID        Check if the automaton is deterministic
                    recognize ID WORD       Check if the automaton recognizes WORD. Use / for empty word
                    concat ID ID [ID ...]   Create concatenation of all listed automatons
                    union ID ID [ID ...]    Create union of all listed automatons
                    un ID                   Create positive closure of said automaton
                    finite ID               Check if the automaton's language is finite
                    determinize ID          Create deterministic automaton equivalent to ID
                    exit                    Exit the program
                """;
    }
}

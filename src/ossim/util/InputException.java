package ossim.util;

/** Thrown when user input is blank, malformed, out of range or inconsistent. Message is shown to the user. */
public class InputException extends RuntimeException {
    public InputException(String message) {
        super(message);
    }
}

package lvl0fixpipeline.app.parser;

/**
 * EXCEPTION — Thrown when parsing fails
 * E.g. an unknown function, a syntax error, unclosed parentheses, etc.
 */
public class ParseException extends Exception {
    public ParseException(String message) {
        super(message);
    }
}
package lvl0fixpipeline.app.parser;

/**
 * EXCEPTION — Vyhazuje se při chybě při parsingbě výrazu
 * Např. neznámá funkce, chyba v syntaxi, neuzavřené závorky atd.
 */
public class ParseException extends Exception {
    public ParseException(String message) {
        super(message);
    }
}
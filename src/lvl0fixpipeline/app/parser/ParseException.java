package lvl0fixpipeline.app.parser;

/**
 * EXCEPTION — Vyhazuje se při chybě parsingového procesu
 * Např. neznámá funkce, chyba v syntaxi, neuzavřené závorky atd.
 */
public class ParseException extends Exception {
    public ParseException(String message) {
        super(message);
    }
}
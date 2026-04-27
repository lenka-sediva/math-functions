package lvl0fixpipeline.app.parser;

/**
 * TOKEN — Atomická jednotka výrazu
 * 
 * Příklady:
 * - Token(TokenType.NUMBER, 3.14)
 * - Token(TokenType.VARIABLE_X, "x")
 * - Token(TokenType.OPERATOR, "+")
 * - Token(TokenType.FUNCTION, "sin")
 */
public class Token {
    public final TokenType type;
    public final String value;
    public final double number;

    /**
     * Konstruktor pro non-numeric tokeny (operátor, funkce, proměnná)
     */
    public Token(TokenType type, String value) {
        this.type = type;
        this.value = value;
        this.number = 0;
    }

    /**
     * Konstruktor pro numerické tokeny
     */
    public Token(double number) {
        this.type = TokenType.NUMBER;
        this.value = String.valueOf(number);
        this.number = number;
    }

    @Override
    public String toString() {
        return "Token{" + type + ", '" + value + "'}";
    }
}
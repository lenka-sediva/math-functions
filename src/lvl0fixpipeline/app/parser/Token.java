package lvl0fixpipeline.app.parser;

/**
 * TOKEN — An atomic unit of an expression
 * 
 * Examples:
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
     * Constructor for non-numeric tokens (operator, function, variable)
     */
    public Token(TokenType type, String value) {
        this.type = type;
        this.value = value;
        this.number = 0;
    }

    /**
     * Constructor for numeric tokens
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
package lvl0fixpipeline.app.parser;

public class Token {
    public final TokenType type;
    public final String value;
    public final double number;

    public Token(TokenType type, String value) {
        this.type = type;
        this.value = value;
        this.number = 0;
    }

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
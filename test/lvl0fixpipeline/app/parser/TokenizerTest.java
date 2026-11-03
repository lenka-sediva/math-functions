package lvl0fixpipeline.app.parser;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class TokenizerTest {
    private final Tokenizer tokenizer = new Tokenizer();

    /**
     * Compares token types and values, e.g. "OPERATOR:+"
     */
    private void assertTokens(String expr, String... expected) throws ParseException {
        List<String> actual = tokenizer.tokenize(expr).stream()
                .map(t -> t.type + ":" + t.value)
                .toList();
        assertEquals(List.of(expected), actual);
    }

    @Test
    void integerAndDecimalNumbers() throws ParseException {
        List<Token> tokens = tokenizer.tokenize("42 3.5 .25");
        assertEquals(3, tokens.size());
        assertEquals(42.0, tokens.get(0).number);
        assertEquals(3.5, tokens.get(1).number);
        assertEquals(0.25, tokens.get(2).number);
    }

    @Test
    void scientificNotation() throws ParseException {
        List<Token> tokens = tokenizer.tokenize("1e-3 2.5e+2 4e2");
        assertEquals(1e-3, tokens.get(0).number);
        assertEquals(250.0, tokens.get(1).number);
        assertEquals(400.0, tokens.get(2).number);
    }

    @Test
    void variablesAndConstants() throws ParseException {
        List<Token> tokens = tokenizer.tokenize("x y t pi e");
        assertEquals(TokenType.VARIABLE_X, tokens.get(0).type);
        assertEquals(TokenType.VARIABLE_Y, tokens.get(1).type);
        assertEquals(TokenType.VARIABLE_T, tokens.get(2).type);
        assertEquals(Math.PI, tokens.get(3).number);
        assertEquals(Math.E, tokens.get(4).number);
    }

    @Test
    void inputIsCaseInsensitive() throws ParseException {
        assertTokens("SIN(X)", "FUNCTION:sin", "LEFT_PAREN:(", "VARIABLE_X:x", "RIGHT_PAREN:)");
    }

    @Test
    void whitespaceIsIgnored() throws ParseException {
        assertTokens("  x \t+\n y ", "VARIABLE_X:x", "OPERATOR:+", "VARIABLE_Y:y");
    }

    @Test
    void doubleStarIsPower() throws ParseException {
        assertTokens("x**2", "VARIABLE_X:x", "OPERATOR:^", "NUMBER:2.0");
    }

    @Test
    void functionWithTwoArguments() throws ParseException {
        assertTokens("max(x,y)",
                "FUNCTION:max", "LEFT_PAREN:(", "VARIABLE_X:x", "COMMA:,", "VARIABLE_Y:y", "RIGHT_PAREN:)");
    }

    @Test
    void minusAtStartIsUnary() throws ParseException {
        assertTokens("-x", "OPERATOR:u-", "VARIABLE_X:x");
    }

    @Test
    void minusAfterOperatorIsUnary() throws ParseException {
        assertTokens("2*-x", "NUMBER:2.0", "OPERATOR:*", "OPERATOR:u-", "VARIABLE_X:x");
    }

    @Test
    void minusAfterLeftParenIsUnary() throws ParseException {
        assertTokens("(-x)", "LEFT_PAREN:(", "OPERATOR:u-", "VARIABLE_X:x", "RIGHT_PAREN:)");
    }

    @Test
    void minusAfterCommaIsUnary() throws ParseException {
        assertTokens("max(1,-x)",
                "FUNCTION:max", "LEFT_PAREN:(", "NUMBER:1.0", "COMMA:,",
                "OPERATOR:u-", "VARIABLE_X:x", "RIGHT_PAREN:)");
    }

    @Test
    void minusBetweenOperandsIsBinary() throws ParseException {
        assertTokens("x-1", "VARIABLE_X:x", "OPERATOR:-", "NUMBER:1.0");
        assertTokens("(x)-1", "LEFT_PAREN:(", "VARIABLE_X:x", "RIGHT_PAREN:)", "OPERATOR:-", "NUMBER:1.0");
    }

    @Test
    void blankInputGivesNoTokens() throws ParseException {
        assertTrue(tokenizer.tokenize("   ").isEmpty());
    }

    @ParameterizedTest
    @ValueSource(strings = {"foo(x)", "z", "sinx", "x & y", "1.2.3", "2e"})
    void invalidInputThrows(String expr) {
        assertThrows(ParseException.class, () -> tokenizer.tokenize(expr));
    }
}

package lvl0fixpipeline.app.parser;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.ValueSource;

import static org.junit.jupiter.api.Assertions.*;

class MathParserTest {
    private static final double EPS = 1e-9;

    private static double eval(String expr) throws ParseException {
        return new MathParser(expr).evaluate(0, 0);
    }

    // --- Operators and precedence ---

    @ParameterizedTest(name = "{0} = {1}")
    @CsvSource(delimiter = ';', value = {
            "1 + 2;        3",
            "7 - 10;       -3",
            "2 + 3 * 4;    14",
            "(2 + 3) * 4;  20",
            "10 / 4;       2.5",
            "10 - 4 - 3;   3",     // left-associative
            "100 / 10 / 5; 2",     // left-associative
            "2 ^ 3 ^ 2;    512",   // right-associative: 2^(3^2)
            "2 ** 3;       8",
            "7 % 3;        1",
            "2 * 3 ^ 2;    18",
            "((1));        1",
    })
    void binaryOperators(String expr, double expected) throws ParseException {
        assertEquals(expected, eval(expr), EPS);
    }

    @ParameterizedTest(name = "{0} = {1}")
    @CsvSource(delimiter = ';', value = {
            "-3;           -3",
            "--3;          3",
            "-2 ^ 2;       -4",    // -(2^2), as in math
            "2 ^ -1;       0.5",
            "2 * -3;       -6",
            "-2 * 3;       -6",
            "5 - -2;       7",
            "-(1 + 2);     -3",
            "-sqrt(4);     -2",
            "max(1, -2);   1",     // unary minus after a comma
            "min(-1, -2);  -2",
    })
    void unaryMinus(String expr, double expected) throws ParseException {
        assertEquals(expected, eval(expr), EPS);
    }

    // --- Functions and constants ---

    @ParameterizedTest(name = "{0} = {1}")
    @CsvSource(delimiter = ';', value = {
            "sin(0);         0",
            "cos(0);         1",
            "sin(pi / 2);    1",
            "sqrt(16);       4",
            "cbrt(27);       3",
            "exp(0);         1",
            "log(e);         1",
            "log10(1000);    3",
            "log2(8);        3",
            "abs(-5);        5",
            "floor(2.7);     2",
            "ceil(2.1);      3",
            "round(2.5);     3",
            "sign(-3);       -1",
            "tanh(0);        0",
            "pow(2, 10);     1024",
            "min(3, 7);      3",
            "max(3, 7);      7",
            "atan2(1, 1);    0.7853981633974483",
            "sqrt(pow(3, 2) + pow(4, 2)); 5",
    })
    void functions(String expr, double expected) throws ParseException {
        assertEquals(expected, eval(expr), EPS);
    }

    // --- Variables ---

    @Test
    void evaluatesVariables() throws ParseException {
        MathParser parser = new MathParser("x^2 + 2*y - t");
        assertEquals(3 * 3 + 2 * 4 - 1, parser.evaluate(3, 4, 1), EPS);
        assertEquals(1 + 2 * 2, parser.evaluate(1, 2), EPS); // t defaults to 0
    }

    @Test
    void compiledOnceEvaluatedManyTimes() throws ParseException {
        MathParser parser = new MathParser("sin(x) * cos(y)");
        for (double x = -3; x <= 3; x += 0.5) {
            for (double y = -3; y <= 3; y += 0.5) {
                assertEquals(Math.sin(x) * Math.cos(y), parser.evaluate(x, y), EPS);
            }
        }
    }

    @Test
    void recompileReplacesExpression() throws ParseException {
        MathParser parser = new MathParser("x");
        parser.compile("x + 1");
        assertEquals("x + 1", parser.getExpression());
        assertEquals(3, parser.evaluate(2, 0), EPS);
    }

    @Test
    void detectsTimeVariable() throws ParseException {
        assertTrue(new MathParser("sin(x + t)").containsTimeVariable());
        assertFalse(new MathParser("sin(x + y)").containsTimeVariable());
        assertFalse(new MathParser("tan(x)").containsTimeVariable()); // the "t" in a function name doesn't count
    }

    // --- Errors ---

    @ParameterizedTest
    @ValueSource(strings = {"", "   ", "()", "(x + 1", "x + 1)", "foo(x)", "x $ 2", "1, 2"})
    void invalidExpressionFailsToCompile(String expr) {
        assertThrows(ParseException.class, () -> new MathParser(expr));
    }

    @ParameterizedTest
    @ValueSource(strings = {"x +", "* 2", "2 3", "sin()", "pow(2)", "-"})
    void malformedExpressionFailsToEvaluate(String expr) {
        assertThrows(ParseException.class, () -> new MathParser(expr).evaluate(1, 1));
    }

    // --- evaluateSafe ---

    @Test
    void evaluateSafeReturnsValue() throws ParseException {
        assertEquals(6, new MathParser("x * y").evaluateSafe(2, 3, 0), EPS);
    }

    @ParameterizedTest
    @ValueSource(strings = {"1 / 0", "-1 / 0", "0 / 0", "sqrt(-1)", "log(0)", "x +"})
    void evaluateSafeReturnsNaNForInvalidResults(String expr) throws ParseException {
        assertTrue(Double.isNaN(new MathParser(expr).evaluateSafe(1, 1, 0)));
    }
}

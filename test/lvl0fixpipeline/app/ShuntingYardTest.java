package lvl0fixpipeline.app;

import lvl0fixpipeline.app.parser.ParseException;
import lvl0fixpipeline.app.parser.Token;
import lvl0fixpipeline.app.parser.Tokenizer;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

import java.util.stream.Collectors;

import static org.junit.jupiter.api.Assertions.assertEquals;

class ShuntingYardTest {
    private final Tokenizer tokenizer = new Tokenizer();
    private final ShuntingYard shuntingYard = new ShuntingYard();

    /**
     * Converts an infix expression to postfix, joined with spaces (numbers without ".0")
     */
    private String postfix(String expr) throws ParseException {
        return shuntingYard.toPostfix(tokenizer.tokenize(expr)).stream()
                .map(t -> t.value.replaceAll("\\.0$", ""))
                .collect(Collectors.joining(" "));
    }

    @ParameterizedTest(name = "{0} -> {1}")
    @CsvSource(delimiter = ';', value = {
            "2 + 3 * 4;        2 3 4 * +",
            "(2 + 3) * 4;      2 3 + 4 *",
            "1 - 2 - 3;        1 2 - 3 -",
            "2 ^ 3 ^ 2;        2 3 2 ^ ^",
            "-2 ^ 2;           2 2 ^ u-",
            "2 ^ -3;           2 3 u- ^",
            "-x * y;           x u- y *",
            "sin(x) + 1;       x sin 1 +",
            "max(x, y * 2);    x y 2 * max",
            "sqrt(x^2 + y^2);  x 2 ^ y 2 ^ + sqrt",
    })
    void convertsInfixToPostfix(String infix, String expected) throws ParseException {
        assertEquals(expected, postfix(infix));
    }
}

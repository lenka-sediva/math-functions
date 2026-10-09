package lvl0fixpipeline.app.parser;

import lvl0fixpipeline.app.ShuntingYard;

import java.util.List;

/**
 * HIGH-LEVEL PARSER — Compiles an expression once, evaluates it efficiently many times
 *
 * Architecture:
 * 1. compile() — Tokenizes and converts to postfix (RPN) once
 * 2. evaluate() — Evaluates the postfix expression efficiently
 *
 * Used for f(x, y, t) where x and y change at every grid point,
 * but the expression stays the same — so parsing is done only once
 */
public class MathParser {
    private final Tokenizer tokenizer = new Tokenizer(); // turns the string into a list of tokens
    private final ShuntingYard shuntingYard = new ShuntingYard(); // converts infix => postfix
    private List<Token> postfix; // postfix token list
    private String expression; // original expression (for the UI and debugging)

    /**
     * Constructor — parses the expression immediately
     */
    public MathParser(String expression) throws ParseException {
        compile(expression);
    }

    /**
     * Compiles the expression — tokenization + conversion to postfix
     */
    public void compile(String expression) throws ParseException {
        this.expression = expression;
        List<Token> tokens = tokenizer.tokenize(expression); // tokenization — turn the string into a list of tokens
        this.postfix = shuntingYard.toPostfix(tokens); // convert to postfix
        // validation
        if (this.postfix.isEmpty()) {
            throw new ParseException("Empty expression");
        }
    }

    /**
     * Evaluates f(x, y) for t=0
     */
    public double evaluate(double x, double y) throws ParseException {
        return evaluate(x, y, 0.0);
    }

    /**
     * Evaluates f(x, y, t) — animated version
     */
    public double evaluate(double x, double y, double t) throws ParseException {
        return shuntingYard.evaluate(postfix, x, y, t);
    }

    /**
     * SAFE evaluation — returns NaN instead of throwing an exception
     * Used for sampling the function (so math errors don't cause a crash)
     */
    public double evaluateSafe(double x, double y, double t) {
        try {
            double result = evaluate(x, y, t);
            // check that the result is a valid number
            return Double.isFinite(result) ? result : Double.NaN;
        } catch (ParseException e) {
            return Double.NaN;
        }
    }

    /**
     * Returns the original expression (for the UI)
     */
    public String getExpression() {
        return expression;
    }

    /**
     * Checks whether the expression contains the variable t (animation)
     * @return true if the expression contains t, false otherwise
     */
    public boolean containsTimeVariable() {
        for (Token token : postfix) {
            if (token.type == TokenType.VARIABLE_T) {
                return true;
            }
        }
        return false;
    }
}

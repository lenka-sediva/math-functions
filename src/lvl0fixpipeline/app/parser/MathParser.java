package lvl0fixpipeline.app.parser;

import lvl0fixpipeline.app.ShuntingYard;

import java.util.List;

/**
 * Vysokoúrovňový parser: zkompiluje výraz jednou, vyhodnocuje mnohokrát
 */
public class MathParser {
    private final Tokenizer tokenizer = new Tokenizer();
    private final ShuntingYard shuntingYard = new ShuntingYard();
    private List<Token> postfix;
    private String expression;

    public MathParser(String expression) throws ParseException {
        compile(expression);
    }

    public void compile(String expression) throws ParseException {
        this.expression = expression;
        List<Token> tokens = tokenizer.tokenize(expression);
        this.postfix = shuntingYard.toPostfix(tokens);
        if (this.postfix.isEmpty()) {
            throw new ParseException("Empty expression");
        }
    }

    // Vyhodnotí f(x, y) pro t=0.
    public double evaluate(double x, double y) throws ParseException {
        return evaluate(x, y, 0.0);
    }

    // Vyhodnotí f(x, y, t)
    public double evaluate(double x, double y, double t) throws ParseException {
        return shuntingYard.evaluate(postfix, x, y, t);
    }

    // Bezpečné vyhodnocení: místo výjimky vrací NaN pro matematické chyby (např. log(-1), sqrt(-1), atd.)
    public double evaluateSafe(double x, double y, double t) {
        try {
            double result = evaluate(x, y, t);
            return Double.isFinite(result) ? result : Double.NaN;
        } catch (ParseException e) {
            return Double.NaN;
        }
    }

    public String getExpression() {
        return expression;
    }
}

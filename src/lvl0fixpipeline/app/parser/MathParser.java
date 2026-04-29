package lvl0fixpipeline.app.parser;

import lvl0fixpipeline.app.ShuntingYard;

import java.util.List;

/**
 * HIGH-LEVEL PARSER — Zkompiluje výraz jednou, vyhodnocuje ho mnohokrát efektivně
 * 
 * Architektura:
 * 1. compile() — Tokenizuje a konvertuje na postfix (RPN) jednou
 * 2. evaluate() — Efektivně vyhodnocuje postfixový výraz
 * 
 * Používá se pro f(x, y, t) kde se x a y mění v každém pixelu,
 * ale výraz je stejný — takže parsing se dělá jen jednou
 */
public class MathParser {
    private final Tokenizer tokenizer = new Tokenizer(); // přesměruje string na seznam tokenů
    private final ShuntingYard shuntingYard = new ShuntingYard(); // konvert infix => postfix
    private List<Token> postfix; // postfixový seznam tokenů
    private String expression; // původní výraz (pro UI a debug)

    /**
     * Konstruktor — okamžitě parsuje výraz
     */
    public MathParser(String expression) throws ParseException {
        compile(expression);
    }

    /**
     * Kompiluje výraz — tokenizace + konverze na postfix
     */
    public void compile(String expression) throws ParseException {
        this.expression = expression;
        List<Token> tokens = tokenizer.tokenize(expression); // tokenizace — převod stringu na seznam tokenů
        this.postfix = shuntingYard.toPostfix(tokens); // konvert na postfix
        // validace
        if (this.postfix.isEmpty()) {
            throw new ParseException("Empty expression");
        }
    }

    /**
     * Vyhodnotí f(x, y) pro t=0
     */
    public double evaluate(double x, double y) throws ParseException {
        return evaluate(x, y, 0.0);
    }

    /**
     * Vyhodnotí f(x, y, t) — animovaná verze
     */
    public double evaluate(double x, double y, double t) throws ParseException {
        return shuntingYard.evaluate(postfix, x, y, t);
    }

    /**
     * BEZPEČNÉ vyhodnocení — vrací NaN místo vyhazování výjimky
     * Používá se pro vzorkování funkce (aby nedošlo k pádu na matematické chyby)
     */
    public double evaluateSafe(double x, double y, double t) {
        try {
            double result = evaluate(x, y, t);
            // zkontroluje, že výsledek je validní číslo
            return Double.isFinite(result) ? result : Double.NaN;
        } catch (ParseException e) {
            return Double.NaN;
        }
    }

    /**
     * Vrátí původní výraz (pro UI)
     */
    public String getExpression() {
        return expression;
    }
}

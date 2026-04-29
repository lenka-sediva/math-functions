package lvl0fixpipeline.app.parser;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

/**
 * TOKENIZER — Převádí string výrazu na seznam tokenů
 * 
 * Rozpoznává:
 * - Čísla (včetně vědeckého zápisu: 1e-3)
 * - Proměnné (x, y, t)
 * - Funkce (sin, cos, sqrt, ...)
 * - Operátory (+, -, *, /, ^, %)
 * - Závorky a čárky
 * - Speciální konstanty (pi, e)
 */
public class Tokenizer {
    // seznam všech dostupných funkcí
    private static final Set<String> FUNCTIONS = Set.of(
            "sin", "cos", "tan", "asin", "acos", "atan", "atan2",
            "sqrt", "cbrt", "exp", "log", "log10", "log2",
            "abs", "floor", "ceil", "round", "sign",
            "sinh", "cosh", "tanh", "pow", "min", "max"
    );

    /**
     * Tokenizuje vstupní string na seznam tokenů
     * @param expr matematický výraz
     * @return seznam tokenů
     */
    public List<Token> tokenize(String expr) throws ParseException {
        List<Token> tokens = new ArrayList<>();
        int i = 0;
        expr = expr.trim().toLowerCase();

        while (i < expr.length()) {
            char c = expr.charAt(i);

            // Přeskočí mezery, tabulátory a další bílé znaky
            if (Character.isWhitespace(c)) {
                i++;
                continue;
            }

            // Číslo
            if (Character.isDigit(c) || (c == '.' && i + 1 < expr.length() && Character.isDigit(expr.charAt(i + 1)))) {
                StringBuilder num = new StringBuilder();
                
                // Načte číslo a desetinnou čárku
                while (i < expr.length() && (Character.isDigit(expr.charAt(i)) || expr.charAt(i) == '.')) {
                    num.append(expr.charAt(i++));
                }
                
                // Vědecký zápis (scientific notation): 1e-3, 2.5e+10
                if (i < expr.length() && (expr.charAt(i) == 'e')) {
                    num.append(expr.charAt(i++));
                    if (i < expr.length() && (expr.charAt(i) == '+' || expr.charAt(i) == '-')) {
                        num.append(expr.charAt(i++));
                    }
                    while (i < expr.length() && Character.isDigit(expr.charAt(i))) {
                        num.append(expr.charAt(i++));
                    }
                }
                
                try {
                    tokens.add(new Token(Double.parseDouble(num.toString())));
                } catch (NumberFormatException e) {
                    throw new ParseException("Invalid number: " + num);
                }
                continue;
            }

            // Identifikátory: funkce, proměnné, konstanty
            if (Character.isLetter(c)) {
                StringBuilder ident = new StringBuilder();
                // Načte identifikátor (abeceda, cifry, underscore)
                while (i < expr.length() && (Character.isLetterOrDigit(expr.charAt(i)) || expr.charAt(i) == '_')) {
                    ident.append(expr.charAt(i++));
                }
                String id = ident.toString();
                
                switch (id) {
                    // Proměnné
                    case "x" -> tokens.add(new Token(TokenType.VARIABLE_X, "x"));
                    case "y" -> tokens.add(new Token(TokenType.VARIABLE_Y, "y"));
                    case "t" -> tokens.add(new Token(TokenType.VARIABLE_T, "t"));
                    
                    // Speciální konstanty
                    case "pi" -> tokens.add(new Token(Math.PI));
                    case "e" -> tokens.add(new Token(Math.E));
                    
                    // Funkce
                    default -> {
                        if (FUNCTIONS.contains(id)) {
                            tokens.add(new Token(TokenType.FUNCTION, id));
                        } else {
                            throw new ParseException("Unknown identifier: '" + id + "'");
                        }
                    }
                }
                continue;
            }

            // Operátory a závorky
            switch (c) {
                case '+' -> {
                    tokens.add(new Token(TokenType.OPERATOR, "+"));
                    i++;
                }
                
                case '-' -> {
                    // Detekce unárního mínu:
                    // Je-li na začátku výrazu, po operátoru nebo levé závorce → unární
                    if (tokens.isEmpty() ||
                            tokens.get(tokens.size() - 1).type == TokenType.OPERATOR ||
                            tokens.get(tokens.size() - 1).type == TokenType.LEFT_PAREN) {
                        tokens.add(new Token(TokenType.OPERATOR, "u-")); // unární minus
                    } else {
                        tokens.add(new Token(TokenType.OPERATOR, "-")); // binární minus
                    }
                    i++;
                }
                
                case '*' -> {
                    // Kontrola pro ** (alternativa k ^)
                    if (i + 1 < expr.length() && expr.charAt(i + 1) == '*') {
                        tokens.add(new Token(TokenType.OPERATOR, "^")); // ** → ^
                        i += 2;
                    } else {
                        tokens.add(new Token(TokenType.OPERATOR, "*"));
                        i++;
                    }
                }
                
                case '/' -> {
                    tokens.add(new Token(TokenType.OPERATOR, "/"));
                    i++;
                }
                
                case '^' -> {
                    tokens.add(new Token(TokenType.OPERATOR, "^"));
                    i++;
                }
                
                case '%' -> {
                    tokens.add(new Token(TokenType.OPERATOR, "%"));
                    i++;
                }
                
                case '(' -> {
                    tokens.add(new Token(TokenType.LEFT_PAREN, "("));
                    i++;
                }
                
                case ')' -> {
                    tokens.add(new Token(TokenType.RIGHT_PAREN, ")"));
                    i++;
                }
                
                case ',' -> {
                    tokens.add(new Token(TokenType.COMMA, ","));
                    i++;
                }
                
                default -> throw new ParseException("Unexpected character: '" + c + "'");
            }
        }
        
        return tokens;
    }
}
package lvl0fixpipeline.app.parser;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

/**
 * TOKENIZER — Turns an expression string into a list of tokens
 *
 * Recognizes:
 * - Numbers (including scientific notation: 1e-3)
 * - Variables (x, y, t)
 * - Functions (sin, cos, sqrt, ...)
 * - Operators (+, -, *, /, ^, %)
 * - Parentheses and commas
 * - Special constants (pi, e)
 */
public class Tokenizer {
    // all available functions
    private static final Set<String> FUNCTIONS = Set.of(
            "sin", "cos", "tan", "asin", "acos", "atan", "atan2",
            "sqrt", "cbrt", "exp", "log", "log10", "log2",
            "abs", "floor", "ceil", "round", "sign",
            "sinh", "cosh", "tanh", "pow", "min", "max"
    );

    /**
     * Tokenizes the input string into a list of tokens
     * @param expr math expression
     * @return list of tokens
     */
    public List<Token> tokenize(String expr) throws ParseException {
        List<Token> tokens = new ArrayList<>();
        int i = 0;
        expr = expr.trim().toLowerCase();

        while (i < expr.length()) {
            char c = expr.charAt(i);

            // Skip spaces, tabs and other whitespace
            if (Character.isWhitespace(c)) {
                i++;
                continue;
            }

            // Number
            if (Character.isDigit(c) || (c == '.' && i + 1 < expr.length() && Character.isDigit(expr.charAt(i + 1)))) {
                StringBuilder num = new StringBuilder();
                
                // Read the digits and the decimal point
                while (i < expr.length() && (Character.isDigit(expr.charAt(i)) || expr.charAt(i) == '.')) {
                    num.append(expr.charAt(i++));
                }
                
                // Scientific notation: 1e-3, 2.5e+10
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

            // Identifiers: functions, variables, constants
            if (Character.isLetter(c)) {
                StringBuilder ident = new StringBuilder();
                // Read the identifier (letters, digits, underscore)
                while (i < expr.length() && (Character.isLetterOrDigit(expr.charAt(i)) || expr.charAt(i) == '_')) {
                    ident.append(expr.charAt(i++));
                }
                String id = ident.toString();
                
                switch (id) {
                    // Variables
                    case "x" -> tokens.add(new Token(TokenType.VARIABLE_X, "x"));
                    case "y" -> tokens.add(new Token(TokenType.VARIABLE_Y, "y"));
                    case "t" -> tokens.add(new Token(TokenType.VARIABLE_T, "t"));
                    
                    // Special constants
                    case "pi" -> tokens.add(new Token(Math.PI));
                    case "e" -> tokens.add(new Token(Math.E));
                    
                    // Functions
                    default -> {
                        if (FUNCTIONS.contains(id)) {
                            tokens.add(new Token(TokenType.FUNCTION, id));
                        } else {
                            throw new ParseException("Unknown function: '" + id + "'");
                        }
                    }
                }
                continue;
            }

            // Operators and parentheses
            switch (c) {
                case '+' -> {
                    tokens.add(new Token(TokenType.OPERATOR, "+"));
                    i++;
                }
                
                case '-' -> {
                    // Unary minus detection:
                    // at the start of the expression, after an operator or a left parenthesis → unary
                    if (tokens.isEmpty() ||
                            tokens.getLast().type == TokenType.OPERATOR ||
                            tokens.getLast().type == TokenType.LEFT_PAREN) {
                        tokens.add(new Token(TokenType.OPERATOR, "u-")); // unary minus
                    } else {
                        tokens.add(new Token(TokenType.OPERATOR, "-")); // binary minus
                    }
                    i++;
                }
                
                case '*' -> {
                    // Check for ** (alternative to ^)
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
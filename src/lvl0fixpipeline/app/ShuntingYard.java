package lvl0fixpipeline.app;

import lvl0fixpipeline.app.parser.ParseException;
import lvl0fixpipeline.app.parser.Token;
import lvl0fixpipeline.app.parser.TokenType;

import java.util.*;

/**
 * SHUNTING YARD ALGORITMUS — Převod infixu na postfix a vyhodnocení
 * 
 * Postup:
 * 1. toPostfix() — parsuje infix seznam tokenů na postfix (RPN)
 * 2. evaluate() — vyhodnocuje postfixový výraz se zadanými x, y, t
 * 
 * Příklad: "2 + 3 * 4" → postfix: "2 3 4 * +" → výsledek: 14
 */
public class ShuntingYard {
    /**
     * Priorita (precedence) operátorů: vyšší číslo = vyšší priorita
     * Pořadí vyhodnocování:
     *  1. Unární mínus
     *  2. Umocňování (^)
     *  3. Násobení, dělení, modulo (*, /, %)
     *  4. Sčítání, odčítání (+, -)
     */
    private static int precedence(String op) {
        return switch (op) {
            case "u-" -> 4; // unární mínus — nejvyšší priorita
            case "^"  -> 3; // umocňování
            case "*", "/", "%" -> 2; // násobení, dělení
            case "+", "-" -> 1; // sčítání, odčítání
            default -> 0;
        };
    }

    /**
     * Vrací true, pokud je operátor PRÁVĚ asociativní
     * (vyhodnocuje se zprava doleva, např. a^b^c = a^(b^c))
     */
    private static boolean isRightAssoc(String op) {
        return op.equals("^") || op.equals("u-");
    }

    /**
     * Převádí infix seznam tokenů na postfix (RPN) pomocí Shunting-yard
     * @param tokens seznam tokenů v infixovém tvaru
     * @return seznam tokenů v postfixovém (RPN) tvaru
     */
    public List<Token> toPostfix(List<Token> tokens) throws ParseException {
        List<Token> output = new ArrayList<>();
        Deque<Token> opStack = new ArrayDeque<>();

        for (Token token : tokens) {
            switch (token.type) {
                // Čísla a proměnné — přímo do výstupu
                case NUMBER, VARIABLE_X, VARIABLE_Y, VARIABLE_T -> output.add(token);

                // Funkce — na stack
                case FUNCTION -> opStack.push(token);

                // Čárka (separator v multi-arg funkcích)
                case COMMA -> {
                    // Pop až do levé závorky
                    while (!opStack.isEmpty() && opStack.peek().type != TokenType.LEFT_PAREN) {
                        output.add(opStack.pop());
                    }
                    if (opStack.isEmpty()) {
                        throw new ParseException("Mismatched parentheses or misplaced comma");
                    }
                }

                // Operátor
                case OPERATOR -> {
                    String op = token.value;
                    // Popuj ze stacku ty operátory, co mají větší/rovnou prioritu
                    // (a nejsou right-associative)
                    while (!opStack.isEmpty()) {
                        Token top = opStack.peek();
                        if (top.type == TokenType.OPERATOR &&
                                (precedence(top.value) > precedence(op) ||
                                        (precedence(top.value) == precedence(op) && !isRightAssoc(op)))) {
                            output.add(opStack.pop());
                        } else {
                            break;
                        }
                    }
                    opStack.push(token);
                }

                // Levá závorka — na stack
                case LEFT_PAREN -> opStack.push(token);

                // Pravá závorka — popuj až do levé
                case RIGHT_PAREN -> {
                    while (!opStack.isEmpty() && opStack.peek().type != TokenType.LEFT_PAREN) {
                        output.add(opStack.pop());
                    }
                    if (opStack.isEmpty()) {
                        throw new ParseException("Mismatched parentheses: missing '('");
                    }
                    opStack.pop(); // zahodí levou závorku

                    // Pokud je na stacku funkce — popuj ji
                    if (!opStack.isEmpty() && opStack.peek().type == TokenType.FUNCTION) {
                        output.add(opStack.pop());
                    }
                }
            }
        }

        // Popuj všechny zbývající operátory
        while (!opStack.isEmpty()) {
            Token top = opStack.pop();
            if (top.type == TokenType.LEFT_PAREN || top.type == TokenType.RIGHT_PAREN) {
                throw new ParseException("Mismatched parentheses");
            }
            output.add(top);
        }

        return output;
    }

    /**
     * Vyhodnotí seznam tokenů v postfixovém tvaru pro dané hodnoty x, y, t
     *
     * @param postfix seznam tokenů v RPN (postfixovém) tvaru
     * @param x hodnota proměnné x
     * @param y hodnota proměnné y
     * @param t hodnota proměnné t (čas pro animaci)
     * @return vyhodnocený výsledek
     */
    public double evaluate(List<Token> postfix, double x, double y, double t) throws ParseException {
        Deque<Double> stack = new ArrayDeque<>();

        for (Token token : postfix) {
            switch (token.type) {
                // Čísla a proměnné — push na stack
                case NUMBER -> stack.push(token.number);
                case VARIABLE_X -> stack.push(x);
                case VARIABLE_Y -> stack.push(y);
                case VARIABLE_T -> stack.push(t);

                // Operátor
                case OPERATOR -> {
                    if (token.value.equals("u-")) {
                        // Unární mínus — pop jeden operand
                        if (stack.isEmpty()) throw new ParseException("Invalid expression: unary minus with no operand");
                        stack.push(-stack.pop());
                    } else {
                        // Binární operátor — pop dva operandy
                        if (stack.size() < 2) throw new ParseException("Invalid expression: not enough operands for '" + token.value + "'");
                        double b = stack.pop();
                        double a = stack.pop();
                        stack.push(applyOperator(token.value, a, b));
                    }
                }

                // Funkce
                case FUNCTION -> {
                    String fn = token.value;
                    // Funkce se 2 argumenty
                    if (fn.equals("pow") || fn.equals("atan2") || fn.equals("min") || fn.equals("max")) {
                        if (stack.size() < 2) throw new ParseException("Function '" + fn + "' requires 2 arguments");
                        double b = stack.pop();
                        double a = stack.pop();
                        stack.push(applyFunction2(fn, a, b));
                    } else {
                        // Funkce s 1 argumentem
                        if (stack.isEmpty()) throw new ParseException("Function '" + fn + "' requires 1 argument");
                        double a = stack.pop();
                        stack.push(applyFunction1(fn, a));
                    }
                }

                default -> throw new ParseException("Unexpected token in postfix: " + token);
            }
        }

        // Na konci by měl zůstat jen jeden výsledek
        if (stack.size() != 1) {
            throw new ParseException("Invalid expression: " + stack.size() + " values left on stack");
        }
        return stack.pop();
    }

    /**
     * Aplikuje binární operátor
     */
    private double applyOperator(String op, double a, double b) throws ParseException {
        return switch (op) {
            case "+" -> a + b;
            case "-" -> a - b;
            case "*" -> a * b;
            case "/" -> a / b;  // Java zachází s NaN/Infinity správně
            case "^" -> Math.pow(a, b);
            case "%" -> a % b;
            default -> throw new ParseException("Unknown operator: " + op);
        };
    }

    /**
     * Aplikuje funkci s 1 argumentem
     */
    private double applyFunction1(String fn, double a) throws ParseException {
        return switch (fn) {
            // Trigonometrické
            case "sin"   -> Math.sin(a);
            case "cos"   -> Math.cos(a);
            case "tan"   -> Math.tan(a);
            case "asin"  -> Math.asin(a);
            case "acos"  -> Math.acos(a);
            case "atan"  -> Math.atan(a);
            
            // Hyperbolické
            case "sinh"  -> Math.sinh(a);
            case "cosh"  -> Math.cosh(a);
            case "tanh"  -> Math.tanh(a);
            
            // Mocniny
            case "sqrt"  -> Math.sqrt(a);
            case "cbrt"  -> Math.cbrt(a);
            
            // Exponenciální a logaritmické
            case "exp"   -> Math.exp(a);
            case "log"   -> Math.log(a);      // Přirozený logaritmus
            case "log10" -> Math.log10(a);
            case "log2"  -> Math.log(a) / Math.log(2);
            
            // Ostatní
            case "abs"   -> Math.abs(a);
            case "floor" -> Math.floor(a);
            case "ceil"  -> Math.ceil(a);
            case "round" -> (double) Math.round(a);
            case "sign"  -> Math.signum(a);
            
            default -> throw new ParseException("Unknown function: " + fn);
        };
    }

    /**
     * Aplikuje funkci se 2 argumenty
     */
    private double applyFunction2(String fn, double a, double b) throws ParseException {
        return switch (fn) {
            case "pow"   -> Math.pow(a, b);
            case "atan2" -> Math.atan2(a, b);
            case "min"   -> Math.min(a, b);
            case "max"   -> Math.max(a, b);
            default -> throw new ParseException("Unknown 2-arg function: " + fn);
        };
    }
}
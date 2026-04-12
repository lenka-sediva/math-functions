package lvl0fixpipeline.p00demo;

import lvl0fixpipeline.p00demo.parser.ParseException;
import lvl0fixpipeline.p00demo.parser.Token;
import lvl0fixpipeline.p00demo.parser.TokenType;

import java.util.*;

/**
 * Převádí infixový seznam tokenů na postfix pomocí algoritmu Shunting-yard
 * Poté vyhodnocuje postfixový výraz pro dané hodnoty x, y
 */
public class ShuntingYard {

    // Priorita (precendence) operátorů: vyšší číslo = vyšší priorita
    private static int precedence(String op) {
        return switch (op) {
            case "u-" -> 4; // unární minus má nejvyšší prioritu
            case "^"  -> 3; // umocňování
            case "*", "/", "%" -> 2; // násobení, dělení, modulo
            case "+", "-" -> 1; // sčítání, odčítání
            default -> 0;
        };
    }

    // Vrací true, pokud je operátor pravě asociativní
    // (vyhodnocuje se zprava doleva, např. a^b^c = a^(b^c))
    private static boolean isRightAssoc(String op) {
        return op.equals("^") || op.equals("u-");
    }

    /**
     * Převádí infixový seznam tokenů na postfix pomocí algoritmu Shunting-yard
     */
    public List<Token> toPostfix(List<Token> tokens) throws ParseException {
        List<Token> output = new ArrayList<>();
        Deque<Token> opStack = new ArrayDeque<>();

        for (Token token : tokens) {
            switch (token.type) {
                case NUMBER, VARIABLE_X, VARIABLE_Y, VARIABLE_T -> output.add(token);

                case FUNCTION -> opStack.push(token);

                case COMMA -> {
                    // pop až do levé závorky (nepřidává se do výstupu)
                    while (!opStack.isEmpty() && opStack.peek().type != TokenType.LEFT_PAREN) {
                        output.add(opStack.pop());
                    }
                    if (opStack.isEmpty()) {
                        throw new ParseException("Mismatched parentheses or misplaced comma");
                    }
                }

                case OPERATOR -> {
                    String op = token.value;
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

                case LEFT_PAREN -> opStack.push(token);

                case RIGHT_PAREN -> {
                    while (!opStack.isEmpty() && opStack.peek().type != TokenType.LEFT_PAREN) {
                        output.add(opStack.pop());
                    }
                    if (opStack.isEmpty()) {
                        throw new ParseException("Mismatched parentheses: missing '('");
                    }
                    opStack.pop(); // zahodí levou závorku
                    if (!opStack.isEmpty() && opStack.peek().type == TokenType.FUNCTION) {
                        output.add(opStack.pop());
                    }
                }
            }
        }

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
     * Vyhodnotí seznam tokenů postfixu pro dané hodnoty x, y, t
     *
     */
    public double evaluate(List<Token> postfix, double x, double y, double t) throws ParseException {
        Deque<Double> stack = new ArrayDeque<>();

        for (Token token : postfix) {
            switch (token.type) {
                case NUMBER -> stack.push(token.number);
                case VARIABLE_X -> stack.push(x);
                case VARIABLE_Y -> stack.push(y);
                case VARIABLE_T -> stack.push(t);

                case OPERATOR -> {
                    if (token.value.equals("u-")) {
                        if (stack.isEmpty()) throw new ParseException("Invalid expression: unary minus with no operand");
                        stack.push(-stack.pop());
                    } else {
                        if (stack.size() < 2) throw new ParseException("Invalid expression: not enough operands for '" + token.value + "'");
                        double b = stack.pop();
                        double a = stack.pop();
                        stack.push(applyOperator(token.value, a, b));
                    }
                }

                case FUNCTION -> {
                    String fn = token.value;
                    // Funkce se 2 argumenty
                    if (fn.equals("pow") || fn.equals("atan2") || fn.equals("min") || fn.equals("max")) {
                        if (stack.size() < 2) throw new ParseException("Function '" + fn + "' requires 2 arguments");
                        double b = stack.pop();
                        double a = stack.pop();
                        stack.push(applyFunction2(fn, a, b));
                    } else {
                        if (stack.isEmpty()) throw new ParseException("Function '" + fn + "' requires 1 argument");
                        double a = stack.pop();
                        stack.push(applyFunction1(fn, a));
                    }
                }

                default -> throw new ParseException("Unexpected token in postfix: " + token);
            }
        }

        if (stack.size() != 1) {
            throw new ParseException("Invalid expression: " + stack.size() + " values left on stack");
        }
        return stack.pop();
    }

    private double applyOperator(String op, double a, double b) throws ParseException {
        return switch (op) {
            case "+" -> a + b;
            case "-" -> a - b;
            case "*" -> a * b;
            case "/" -> a / b;  // NaN/Infinity zpracovává Java
            case "^" -> Math.pow(a, b);
            case "%" -> a % b;
            default -> throw new ParseException("Unknown operator: " + op);
        };
    }

    // Funkce se 1 argumentem
    private double applyFunction1(String fn, double a) throws ParseException {
        return switch (fn) {
            case "sin"   -> Math.sin(a);
            case "cos"   -> Math.cos(a);
            case "tan"   -> Math.tan(a);
            case "asin"  -> Math.asin(a);
            case "acos"  -> Math.acos(a);
            case "atan"  -> Math.atan(a);
            case "sinh"  -> Math.sinh(a);
            case "cosh"  -> Math.cosh(a);
            case "tanh"  -> Math.tanh(a);
            case "sqrt"  -> Math.sqrt(a);
            case "cbrt"  -> Math.cbrt(a);
            case "exp"   -> Math.exp(a);
            case "log"   -> Math.log(a);
            case "log10" -> Math.log10(a);
            case "log2"  -> Math.log(a) / Math.log(2);
            case "abs"   -> Math.abs(a);
            case "floor" -> Math.floor(a);
            case "ceil"  -> Math.ceil(a);
            case "round" -> (double) Math.round(a);
            case "sign"  -> Math.signum(a);
            default -> throw new ParseException("Unknown function: " + fn);
        };
    }

    // Funkce se 2 argumenty
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
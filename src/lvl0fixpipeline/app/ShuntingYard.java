package lvl0fixpipeline.app;

import lvl0fixpipeline.app.parser.ParseException;
import lvl0fixpipeline.app.parser.Token;
import lvl0fixpipeline.app.parser.TokenType;

import java.util.*;

/**
 * SHUNTING YARD ALGORITHM — Converts infix to postfix and evaluates it
 *
 * Steps:
 * 1. toPostfix() — converts an infix token list to postfix (RPN)
 * 2. evaluate() — evaluates the postfix expression for the given x, y, t
 *
 * Example: "2 + 3 * 4" → postfix: "2 3 4 * +" → result: 14
 */
public class ShuntingYard {
    /**
     * Operator precedence: a higher number = a higher priority
     * Evaluation order:
     *  1. Exponentiation (^)
     *  2. Unary minus (so -2^2 = -(2^2) = -4, as in math)
     *  3. Multiplication, division, modulo (*, /, %)
     *  4. Addition, subtraction (+, -)
     */
    private static int precedence(String op) {
        return switch (op) {
            case "^"  -> 4; // exponentiation — highest priority
            case "u-" -> 3; // unary minus
            case "*", "/", "%" -> 2; // multiplication, division
            case "+", "-" -> 1; // addition, subtraction
            default -> 0;
        };
    }

    /**
     * Returns true if the operator is RIGHT-associative
     * (evaluated right to left, e.g. a^b^c = a^(b^c))
     */
    private static boolean isRightAssoc(String op) {
        return op.equals("^") || op.equals("u-");
    }

    /**
     * Converts an infix token list to postfix (RPN) using Shunting-yard
     * @param tokens list of tokens in infix form
     * @return list of tokens in postfix (RPN) form
     */
    public List<Token> toPostfix(List<Token> tokens) throws ParseException {
        List<Token> output = new ArrayList<>();
        Deque<Token> opStack = new ArrayDeque<>();

        for (Token token : tokens) {
            switch (token.type) {
                // Numbers and variables — straight to the output
                case NUMBER, VARIABLE_X, VARIABLE_Y, VARIABLE_T -> output.add(token);

                // Functions — onto the stack
                case FUNCTION -> opStack.push(token);

                // Comma (separator in multi-argument functions)
                case COMMA -> {
                    // Pop up to the left parenthesis
                    while (!opStack.isEmpty() && opStack.peek().type != TokenType.LEFT_PAREN) {
                        output.add(opStack.pop());
                    }
                    if (opStack.isEmpty()) {
                        throw new ParseException("Mismatched parentheses or misplaced comma");
                    }
                }

                // Operator
                case OPERATOR -> {
                    String op = token.value;
                    // Unary minus is a prefix operator: it has no left operand yet,
                    // so it must not pop anything (otherwise 2^-3 would break)
                    if (op.equals("u-")) {
                        opStack.push(token);
                        continue;
                    }
                    // Pop the operators with higher/equal priority off the stack
                    // (unless right-associative)
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

                // Left parenthesis — onto the stack
                case LEFT_PAREN -> opStack.push(token);

                // Right parenthesis — pop up to the left one
                case RIGHT_PAREN -> {
                    while (!opStack.isEmpty() && opStack.peek().type != TokenType.LEFT_PAREN) {
                        output.add(opStack.pop());
                    }
                    if (opStack.isEmpty()) {
                        throw new ParseException("Mismatched parentheses: missing '('");
                    }
                    opStack.pop(); // discard the left parenthesis

                    // If a function is on the stack — pop it
                    if (!opStack.isEmpty() && opStack.peek().type == TokenType.FUNCTION) {
                        output.add(opStack.pop());
                    }
                }
            }
        }

        // Pop all remaining operators
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
     * Evaluates a postfix token list for the given values of x, y, t
     *
     * @param postfix list of tokens in RPN (postfix) form
     * @param x value of the variable x
     * @param y value of the variable y
     * @param t value of the variable t (time for the animation)
     * @return the evaluated result
     */
    public double evaluate(List<Token> postfix, double x, double y, double t) throws ParseException {
        Deque<Double> stack = new ArrayDeque<>();

        for (Token token : postfix) {
            switch (token.type) {
                // Numbers and variables — push onto the stack
                case NUMBER -> stack.push(token.number);
                case VARIABLE_X -> stack.push(x);
                case VARIABLE_Y -> stack.push(y);
                case VARIABLE_T -> stack.push(t);

                // Operator
                case OPERATOR -> {
                    if (token.value.equals("u-")) {
                        // Unary minus — pop one operand
                        if (stack.isEmpty()) throw new ParseException("Invalid expression: unary minus with no operand");
                        stack.push(-stack.pop());
                    } else {
                        // Binary operator — pop two operands
                        if (stack.size() < 2) throw new ParseException("Invalid expression: not enough operands for '" + token.value + "'");
                        double b = stack.pop();
                        double a = stack.pop();
                        stack.push(applyOperator(token.value, a, b));
                    }
                }

                // Functions
                case FUNCTION -> {
                    String fn = token.value;
                    // Functions with 2 arguments
                    if (fn.equals("pow") || fn.equals("atan2") || fn.equals("min") || fn.equals("max")) {
                        if (stack.size() < 2) throw new ParseException("Function '" + fn + "' requires 2 arguments");
                        double b = stack.pop();
                        double a = stack.pop();
                        stack.push(applyFunction2(fn, a, b));
                    } else {
                        // Functions with 1 argument
                        if (stack.isEmpty()) throw new ParseException("Function '" + fn + "' requires 1 argument");
                        double a = stack.pop();
                        stack.push(applyFunction1(fn, a));
                    }
                }

                default -> throw new ParseException("Unexpected token in postfix: " + token);
            }
        }

        // Exactly one result should remain at the end
        if (stack.size() != 1) {
            throw new ParseException("Invalid expression: " + stack.size() + " values left on stack");
        }
        return stack.pop();
    }

    /**
     * Applies a binary operator
     */
    private double applyOperator(String op, double a, double b) throws ParseException {
        return switch (op) {
            case "+" -> a + b;
            case "-" -> a - b;
            case "*" -> a * b;
            case "/" -> a / b;  // Java handles NaN/Infinity correctly
            case "^" -> Math.pow(a, b);
            case "%" -> a % b;
            default -> throw new ParseException("Unknown operator: " + op);
        };
    }

    /**
     * Applies a function with 1 argument
     */
    private double applyFunction1(String fn, double a) throws ParseException {
        return switch (fn) {
            // Trigonometric
            case "sin"   -> Math.sin(a);
            case "cos"   -> Math.cos(a);
            case "tan"   -> Math.tan(a);
            case "asin"  -> Math.asin(a);
            case "acos"  -> Math.acos(a);
            case "atan"  -> Math.atan(a);
            
            // Hyperbolic
            case "sinh"  -> Math.sinh(a);
            case "cosh"  -> Math.cosh(a);
            case "tanh"  -> Math.tanh(a);
            
            // Roots
            case "sqrt"  -> Math.sqrt(a);
            case "cbrt"  -> Math.cbrt(a);
            
            // Exponential and logarithmic
            case "exp"   -> Math.exp(a);
            case "log"   -> Math.log(a);      // Natural logarithm
            case "log10" -> Math.log10(a);
            case "log2"  -> Math.log(a) / Math.log(2);
            
            // Other
            case "abs"   -> Math.abs(a);
            case "floor" -> Math.floor(a);
            case "ceil"  -> Math.ceil(a);
            case "round" -> (double) Math.round(a);
            case "sign"  -> Math.signum(a);
            
            default -> throw new ParseException("Unknown function: " + fn);
        };
    }

    /**
     * Applies a function with 2 arguments
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
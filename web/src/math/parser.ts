import {
  FUNCTION_ARITY,
  type BinaryOp,
  type ConstName,
  type FnName,
  type Node,
  type VarName,
} from './ast';
import { compile } from './codegen';
import { ParseError, tokenize, type Token } from './tokenizer';

/**
 * Parser: tokens -> Shunting-yard -> postfix (RPN) -> AST.
 *
 * This is the same algorithm as the Java version (ShuntingYard.java), with one change:
 * instead of evaluating the RPN directly, it builds a tree. The tree can then be compiled
 * to JavaScript and GLSL and differentiated symbolically.
 */

type StackItem =
  | { kind: 'op'; op: BinaryOp | 'u-'; token: Token }
  | { kind: 'fn'; fn: FnName; token: Token }
  | { kind: 'lparen'; token: Token };

type RpnItem =
  | { kind: 'value'; node: Node; token: Token }
  | { kind: 'op'; op: BinaryOp | 'u-'; token: Token }
  | { kind: 'fn'; fn: FnName; argc: number; token: Token };

const PRECEDENCE: Record<BinaryOp | 'u-', number> = {
  '^': 4, // exponentiation binds tightest
  'u-': 3, // unary minus: -2^2 = -(2^2) = -4, like in math
  '*': 2,
  '/': 2,
  '%': 2,
  '+': 1,
  '-': 1,
};

const RIGHT_ASSOC = new Set(['^', 'u-']);

export function parse(src: string, allowedVars: readonly VarName[]): Node {
  if (!src.trim()) throw new ParseError('Type a formula', 0, 0);
  const tokens = tokenize(src, allowedVars);
  const rpn = toPostfix(tokens, src.length);
  return buildTree(rpn, src.length);
}

/** Parses a constant expression such as "2*pi" or "-3.5" (used for ranges). */
export function parseConstant(src: string): number {
  // A constant expression has no variables, so the compiled function ignores its inputs.
  return compile(parse(src, []))(0, 0, 0, 0, 0);
}

function toPostfix(tokens: Token[], srcLength: number): RpnItem[] {
  const output: RpnItem[] = [];
  const stack: StackItem[] = [];
  // For every open function call: how many arguments we have seen so far.
  const argCounts: number[] = [];
  let prev: Token | undefined;

  for (const token of tokens) {
    switch (token.type) {
      case 'number':
        output.push({ kind: 'value', node: { kind: 'num', value: token.value! }, token });
        break;
      case 'variable':
        output.push({ kind: 'value', node: { kind: 'var', name: token.text as VarName }, token });
        break;
      case 'constant':
        output.push({ kind: 'value', node: { kind: 'const', name: token.text as ConstName }, token });
        break;
      case 'function':
        stack.push({ kind: 'fn', fn: token.text as FnName, token });
        break;
      case 'lparen':
        stack.push({ kind: 'lparen', token });
        if (prev?.type === 'function') argCounts.push(1);
        break;
      case 'comma': {
        popUntilLParen(stack, output, token);
        const fnItem = stack[stack.length - 2];
        if (fnItem?.kind !== 'fn' || argCounts.length === 0) {
          throw new ParseError('A comma can only separate function arguments', token.pos);
        }
        if (prev?.type === 'lparen' || prev?.type === 'comma') {
          throw new ParseError('Missing function argument', token.pos);
        }
        argCounts[argCounts.length - 1]++;
        break;
      }
      case 'rparen': {
        if (prev?.type === 'lparen' && stack[stack.length - 2]?.kind !== 'fn') {
          throw new ParseError('Empty parentheses', token.pos);
        }
        popUntilLParen(stack, output, token);
        stack.pop(); // the "("
        const top = stack[stack.length - 1];
        if (top?.kind === 'fn') {
          stack.pop();
          const argc = argCounts.pop()!;
          if (prev?.type === 'lparen' || prev?.type === 'comma') {
            throw new ParseError(`Missing argument in ${top.fn}(...)`, token.pos);
          }
          const expected = FUNCTION_ARITY[top.fn];
          if (argc !== expected) {
            throw new ParseError(
              `${top.fn} takes ${expected} argument${expected === 1 ? '' : 's'}, got ${argc}`,
              top.token.pos,
              top.fn.length,
            );
          }
          output.push({ kind: 'fn', fn: top.fn, argc, token: top.token });
        }
        break;
      }
      case 'operator': {
        const isUnary =
          !prev || prev.type === 'operator' || prev.type === 'lparen' || prev.type === 'comma';
        if (isUnary) {
          if (token.text === '+') break; // unary plus does nothing
          if (token.text !== '-') {
            throw new ParseError(`"${token.text}" needs a value on its left`, token.pos);
          }
          // Prefix operators never pop anything, which keeps 2^-x = 2^(-x) correct.
          stack.push({ kind: 'op', op: 'u-', token });
          break;
        }
        const op = token.text as BinaryOp;
        while (true) {
          const top = stack[stack.length - 1];
          if (top?.kind !== 'op') break;
          const pTop = PRECEDENCE[top.op];
          const pCur = PRECEDENCE[op];
          const shouldPop = RIGHT_ASSOC.has(op) ? pTop > pCur : pTop >= pCur;
          if (!shouldPop) break;
          output.push(stack.pop() as RpnItem);
        }
        stack.push({ kind: 'op', op, token });
        break;
      }
    }
    prev = token;
  }

  if (prev?.type === 'operator') {
    throw new ParseError(`"${prev.text}" needs a value on its right`, prev.pos);
  }

  while (stack.length) {
    const top = stack.pop()!;
    if (top.kind === 'lparen' || top.kind === 'fn') {
      throw new ParseError('Missing closing parenthesis ")"', srcLength, 0);
    }
    output.push(top);
  }
  return output;
}

function popUntilLParen(stack: StackItem[], output: RpnItem[], at: Token) {
  while (stack.length && stack[stack.length - 1].kind !== 'lparen') {
    const item = stack.pop()!;
    if (item.kind === 'fn') throw new ParseError('Unexpected function', item.token.pos);
    output.push(item as RpnItem);
  }
  if (!stack.length) {
    throw new ParseError(at.type === 'comma' ? 'A comma can only separate function arguments' : 'Unmatched ")"', at.pos);
  }
}

function buildTree(rpn: RpnItem[], srcLength: number): Node {
  const stack: Node[] = [];
  for (const item of rpn) {
    switch (item.kind) {
      case 'value':
        stack.push(item.node);
        break;
      case 'op': {
        if (item.op === 'u-') {
          const arg = stack.pop();
          if (!arg) throw new ParseError('"-" needs a value on its right', item.token.pos);
          stack.push({ kind: 'neg', arg });
        } else {
          const right = stack.pop();
          const left = stack.pop();
          if (!left || !right) throw new ParseError(`"${item.op}" needs values on both sides`, item.token.pos);
          stack.push({ kind: 'bin', op: item.op, left, right });
        }
        break;
      }
      case 'fn': {
        if (stack.length < item.argc) throw new ParseError(`Missing argument in ${item.fn}(...)`, item.token.pos);
        const args = stack.splice(stack.length - item.argc, item.argc);
        stack.push({ kind: 'call', fn: item.fn, args });
        break;
      }
    }
  }
  if (stack.length !== 1) throw new ParseError('Incomplete formula', srcLength, 0);
  return stack[0];
}

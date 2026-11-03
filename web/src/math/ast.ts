/**
 * Abstract syntax tree for math expressions.
 *
 * The parser turns a formula string into this tree once. Everything else works on the tree:
 * evaluation (compiled to JavaScript), GPU evaluation (compiled to GLSL), symbolic derivatives,
 * simplification and pretty-printing.
 */

export type VarName = 'x' | 'y' | 'u' | 'v' | 't';
export type ConstName = 'pi' | 'e';
export type BinaryOp = '+' | '-' | '*' | '/' | '^' | '%';

export type FnName =
  | 'sin' | 'cos' | 'tan' | 'asin' | 'acos' | 'atan' | 'atan2'
  | 'sinh' | 'cosh' | 'tanh'
  | 'sqrt' | 'cbrt' | 'exp' | 'log' | 'log10' | 'log2'
  | 'abs' | 'floor' | 'ceil' | 'round' | 'sign' | 'trunc'
  | 'pow' | 'min' | 'max';

export type Node =
  | { kind: 'num'; value: number }
  | { kind: 'const'; name: ConstName }
  | { kind: 'var'; name: VarName }
  | { kind: 'neg'; arg: Node }
  | { kind: 'bin'; op: BinaryOp; left: Node; right: Node }
  | { kind: 'call'; fn: FnName; args: Node[] };

/** Number of arguments of every built-in function. */
export const FUNCTION_ARITY: Record<FnName, number> = {
  sin: 1, cos: 1, tan: 1, asin: 1, acos: 1, atan: 1, atan2: 2,
  sinh: 1, cosh: 1, tanh: 1,
  sqrt: 1, cbrt: 1, exp: 1, log: 1, log10: 1, log2: 1,
  abs: 1, floor: 1, ceil: 1, round: 1, sign: 1, trunc: 1,
  pow: 2, min: 2, max: 2,
};

export const CONSTANTS: Record<ConstName, number> = { pi: Math.PI, e: Math.E };

export const isFunctionName = (s: string): s is FnName => Object.hasOwn(FUNCTION_ARITY, s);
export const isConstName = (s: string): s is ConstName => Object.hasOwn(CONSTANTS, s);
export const isVarName = (s: string): s is VarName => ['x', 'y', 'u', 'v', 't'].includes(s);

// Small constructors keep the derivative rules readable.
export const num = (value: number): Node => ({ kind: 'num', value });
export const variable = (name: VarName): Node => ({ kind: 'var', name });
export const neg = (arg: Node): Node => ({ kind: 'neg', arg });
export const bin = (op: BinaryOp, left: Node, right: Node): Node => ({ kind: 'bin', op, left, right });
export const call = (fn: FnName, ...args: Node[]): Node => ({ kind: 'call', fn, args });
export const add = (a: Node, b: Node) => bin('+', a, b);
export const sub = (a: Node, b: Node) => bin('-', a, b);
export const mul = (a: Node, b: Node) => bin('*', a, b);
export const div = (a: Node, b: Node) => bin('/', a, b);
export const pow = (a: Node, b: Node) => bin('^', a, b);

/** True if the expression contains the given variable anywhere. */
export function dependsOn(node: Node, name: VarName): boolean {
  switch (node.kind) {
    case 'num':
    case 'const':
      return false;
    case 'var':
      return node.name === name;
    case 'neg':
      return dependsOn(node.arg, name);
    case 'bin':
      return dependsOn(node.left, name) || dependsOn(node.right, name);
    case 'call':
      return node.args.some((a) => dependsOn(a, name));
  }
}

/** All variables used by the expression. */
export function variablesOf(node: Node, out = new Set<VarName>()): Set<VarName> {
  switch (node.kind) {
    case 'var':
      out.add(node.name);
      break;
    case 'neg':
      variablesOf(node.arg, out);
      break;
    case 'bin':
      variablesOf(node.left, out);
      variablesOf(node.right, out);
      break;
    case 'call':
      node.args.forEach((a) => variablesOf(a, out));
      break;
  }
  return out;
}

/** Structural equality, used by the simplifier. */
export function equals(a: Node, b: Node): boolean {
  if (a.kind !== b.kind) return false;
  switch (a.kind) {
    case 'num':
      return a.value === (b as typeof a).value;
    case 'const':
    case 'var':
      return a.name === (b as typeof a).name;
    case 'neg':
      return equals(a.arg, (b as typeof a).arg);
    case 'bin': {
      const o = b as typeof a;
      return a.op === o.op && equals(a.left, o.left) && equals(a.right, o.right);
    }
    case 'call': {
      const o = b as typeof a;
      return a.fn === o.fn && a.args.length === o.args.length && a.args.every((x, i) => equals(x, o.args[i]));
    }
  }
}

/** Number of nodes; handy to keep an eye on derivative blow-up. */
export function size(node: Node): number {
  switch (node.kind) {
    case 'neg':
      return 1 + size(node.arg);
    case 'bin':
      return 1 + size(node.left) + size(node.right);
    case 'call':
      return 1 + node.args.reduce((s, a) => s + size(a), 0);
    default:
      return 1;
  }
}

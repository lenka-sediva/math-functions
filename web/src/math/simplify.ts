import { equals, num, type Node } from './ast';
import { JS_FUNCTIONS } from './codegen';

/**
 * Algebraic simplification. Symbolic derivatives produce lots of noise like `1*cos(x) + 0`;
 * this pass folds constants and removes neutral elements so the result is readable
 * and cheaper to evaluate on the GPU.
 *
 * Rules are applied bottom-up and repeated until nothing changes.
 */
export function simplify(node: Node): Node {
  let cur = node;
  for (let i = 0; i < 8; i++) {
    const next = step(cur);
    if (equals(next, cur)) return next;
    cur = next;
  }
  return cur;
}

const isNum = (n: Node): n is Extract<Node, { kind: 'num' }> => n.kind === 'num';
/** Not a type guard on purpose: "is not the number 0" must not narrow away all numbers. */
const isVal = (n: Node, v: number): boolean => n.kind === 'num' && n.value === v;

function fold(value: number, fallback: Node): Node {
  return Number.isFinite(value) ? num(value) : fallback;
}

function step(node: Node): Node {
  switch (node.kind) {
    case 'num':
    case 'const':
    case 'var':
      return node;

    case 'neg': {
      const a = step(node.arg);
      if (isNum(a)) return num(-a.value);
      if (a.kind === 'neg') return a.arg; // --a = a
      return { kind: 'neg', arg: a };
    }

    case 'call': {
      const args = node.args.map(step);
      const out: Node = { kind: 'call', fn: node.fn, args };
      if (args.every((a) => isNum(a))) {
        return fold(JS_FUNCTIONS[node.fn](...args.map((a) => (a as { value: number }).value)), out);
      }
      return out;
    }

    case 'bin': {
      const l = step(node.left);
      const r = step(node.right);
      const out: Node = { kind: 'bin', op: node.op, left: l, right: r };

      if (isNum(l) && isNum(r)) {
        const a = l.value;
        const b = r.value;
        const value = { '+': a + b, '-': a - b, '*': a * b, '/': a / b, '^': Math.pow(a, b), '%': a % b }[node.op];
        // Keep fractions like 1/3 symbolic only if they would print badly; folding is fine otherwise.
        return fold(value, out);
      }

      switch (node.op) {
        case '+':
          if (isVal(l, 0)) return r;
          if (isVal(r, 0)) return l;
          if (r.kind === 'neg') return { kind: 'bin', op: '-', left: l, right: r.arg }; // a + -b = a - b
          if (isNum(r) && r.value < 0) return { kind: 'bin', op: '-', left: l, right: num(-r.value) };
          if (l.kind === 'neg') return { kind: 'bin', op: '-', left: r, right: l.arg }; // -a + b = b - a
          if (equals(l, r)) return { kind: 'bin', op: '*', left: num(2), right: l };
          return out;

        case '-':
          if (isVal(r, 0)) return l;
          if (isVal(l, 0)) return { kind: 'neg', arg: r };
          if (r.kind === 'neg') return { kind: 'bin', op: '+', left: l, right: r.arg }; // a - -b = a + b
          if (isNum(r) && r.value < 0) return { kind: 'bin', op: '+', left: l, right: num(-r.value) };
          if (equals(l, r)) return num(0);
          return out;

        case '*':
          if (isVal(l, 0) || isVal(r, 0)) return num(0);
          if (isVal(l, 1)) return r;
          if (isVal(r, 1)) return l;
          if (isVal(l, -1)) return { kind: 'neg', arg: r };
          if (isVal(r, -1)) return { kind: 'neg', arg: l };
          if (isNum(r) && !isNum(l)) return { kind: 'bin', op: '*', left: r, right: l }; // numbers first
          if (l.kind === 'neg' && r.kind === 'neg') return { kind: 'bin', op: '*', left: l.arg, right: r.arg };
          if (l.kind === 'neg') return { kind: 'neg', arg: { kind: 'bin', op: '*', left: l.arg, right: r } };
          if (r.kind === 'neg') return { kind: 'neg', arg: { kind: 'bin', op: '*', left: l, right: r.arg } };
          // 2*(3*x) = 6*x
          if (isNum(l) && r.kind === 'bin' && r.op === '*' && isNum(r.left)) {
            return { kind: 'bin', op: '*', left: num(l.value * r.left.value), right: r.right };
          }
          // x*x = x^2
          if (equals(l, r)) return { kind: 'bin', op: '^', left: l, right: num(2) };
          return out;

        case '/':
          if (isVal(l, 0)) return num(0);
          if (isVal(r, 1)) return l;
          if (isVal(r, -1)) return { kind: 'neg', arg: l };
          if (l.kind === 'neg') return { kind: 'neg', arg: { kind: 'bin', op: '/', left: l.arg, right: r } };
          if (equals(l, r)) return num(1);
          return out;

        case '^':
          if (isVal(r, 1)) return l;
          if (isVal(r, 0)) return num(1);
          if (isVal(l, 1)) return num(1);
          // (a^2)^3 = a^6 when both exponents are numbers
          if (isNum(r) && l.kind === 'bin' && l.op === '^' && isNum(l.right) && Number.isInteger(l.right.value) && Number.isInteger(r.value)) {
            return { kind: 'bin', op: '^', left: l.left, right: num(l.right.value * r.value) };
          }
          return out;

        case '%':
          return out;
      }
    }
  }
}

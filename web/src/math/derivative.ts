import { add, call, dependsOn, div, mul, neg, num, pow, sub, type Node, type VarName } from './ast';
import { simplify } from './simplify';

/**
 * Symbolic differentiation: returns the exact derivative ∂node/∂name as a new AST.
 *
 * The rules are the ones from a calculus textbook (sum, product, quotient, chain rule).
 * Non-smooth functions get their derivative almost everywhere (abs -> sign, floor -> 0),
 * which is what you want for drawing normals.
 */
export function derivative(node: Node, name: VarName): Node {
  return simplify(d(node, name));
}

function d(node: Node, x: VarName): Node {
  if (!dependsOn(node, x)) return num(0);

  switch (node.kind) {
    case 'num':
    case 'const':
      return num(0);

    case 'var':
      return num(node.name === x ? 1 : 0);

    case 'neg':
      return neg(d(node.arg, x));

    case 'bin':
      return binaryRule(node, x);

    case 'call':
      return callRule(node, x);
  }
}

function binaryRule(node: Extract<Node, { kind: 'bin' }>, x: VarName): Node {
  const { left: a, right: b } = node;
  const da = () => d(a, x);
  const db = () => d(b, x);
  switch (node.op) {
    case '+':
      return add(da(), db());
    case '-':
      return sub(da(), db());
    case '*':
      return add(mul(da(), b), mul(a, db())); // product rule
    case '/':
      return div(sub(mul(da(), b), mul(a, db())), pow(b, num(2))); // quotient rule
    case '%':
      // a % b = a - b*trunc(a/b), and trunc is piecewise constant
      return sub(da(), mul(db(), call('trunc', div(a, b))));
    case '^':
      return powerRule(a, b, x);
  }
}

function powerRule(a: Node, b: Node, x: VarName): Node {
  const aVar = dependsOn(a, x);
  const bVar = dependsOn(b, x);
  if (aVar && !bVar) {
    // d(a^n) = n * a^(n-1) * a'
    return mul(mul(b, pow(a, sub(b, num(1)))), d(a, x));
  }
  if (!aVar && bVar) {
    // d(c^b) = c^b * ln(c) * b'
    return mul(mul(pow(a, b), call('log', a)), d(b, x));
  }
  // general case: d(a^b) = a^b * (b' * ln(a) + b * a' / a)
  return mul(pow(a, b), add(mul(d(b, x), call('log', a)), div(mul(b, d(a, x)), a)));
}

function callRule(node: Extract<Node, { kind: 'call' }>, x: VarName): Node {
  const [a, b] = node.args;
  const da = () => d(a, x);

  switch (node.fn) {
    case 'sin':
      return mul(call('cos', a), da());
    case 'cos':
      return neg(mul(call('sin', a), da()));
    case 'tan':
      return div(da(), pow(call('cos', a), num(2)));
    case 'asin':
      return div(da(), call('sqrt', sub(num(1), pow(a, num(2)))));
    case 'acos':
      return neg(div(da(), call('sqrt', sub(num(1), pow(a, num(2))))));
    case 'atan':
      return div(da(), add(num(1), pow(a, num(2))));
    case 'atan2': {
      // atan2(y, x): (x*y' - y*x') / (x^2 + y^2)
      const [yy, xx] = node.args;
      return div(sub(mul(xx, d(yy, x)), mul(yy, d(xx, x))), add(pow(xx, num(2)), pow(yy, num(2))));
    }
    case 'sinh':
      return mul(call('cosh', a), da());
    case 'cosh':
      return mul(call('sinh', a), da());
    case 'tanh':
      return mul(sub(num(1), pow(call('tanh', a), num(2))), da());
    case 'sqrt':
      return div(da(), mul(num(2), call('sqrt', a)));
    case 'cbrt':
      return div(da(), mul(num(3), pow(call('cbrt', a), num(2))));
    case 'exp':
      return mul(call('exp', a), da());
    case 'log':
      return div(da(), a);
    case 'log10':
      return div(da(), mul(a, call('log', num(10))));
    case 'log2':
      return div(da(), mul(a, call('log', num(2))));
    case 'abs':
      return mul(call('sign', a), da());
    case 'floor':
    case 'ceil':
    case 'round':
    case 'sign':
    case 'trunc':
      return num(0);
    case 'pow':
      return powerRule(a, b, x);
    case 'min':
    case 'max': {
      // min(a,b) = (a + b - |a - b|) / 2, max(a,b) = (a + b + |a - b|) / 2
      const sum = add(da(), d(b, x));
      const diff = mul(call('sign', sub(a, b)), sub(da(), d(b, x)));
      return div(node.fn === 'min' ? sub(sum, diff) : add(sum, diff), num(2));
    }
  }
}

import { type BinaryOp, type FnName, type Node } from './ast';

/**
 * Three back-ends for the same AST:
 *  - toText: readable formula (shown in the UI, e.g. for derivatives)
 *  - compile / toJS: a fast JavaScript function for the CPU (picking, STL export, critical points)
 *  - toGLSL: shader code, so the GPU evaluates the surface for every vertex
 */

export type CompiledFn = (x: number, y: number, u: number, v: number, t: number) => number;

/** Reference implementations. Java's Math has the same semantics, so results match the desktop app. */
export const JS_FUNCTIONS: Record<FnName, (...a: number[]) => number> = {
  sin: Math.sin, cos: Math.cos, tan: Math.tan,
  asin: Math.asin, acos: Math.acos, atan: Math.atan, atan2: Math.atan2,
  sinh: Math.sinh, cosh: Math.cosh, tanh: Math.tanh,
  sqrt: Math.sqrt, cbrt: Math.cbrt, exp: Math.exp,
  log: Math.log, log10: Math.log10, log2: Math.log2,
  abs: Math.abs, floor: Math.floor, ceil: Math.ceil, round: Math.round,
  sign: Math.sign, trunc: Math.trunc,
  pow: Math.pow, min: Math.min, max: Math.max,
};

// ---------------------------------------------------------------------------
// Text

const PREC: Record<BinaryOp, number> = { '+': 1, '-': 1, '*': 2, '/': 2, '%': 2, '^': 4 };
const NEG_PREC = 3;
const ATOM_PREC = 5;

function precOf(node: Node): number {
  if (node.kind === 'bin') return PREC[node.op];
  if (node.kind === 'neg') return NEG_PREC;
  if (node.kind === 'num' && node.value < 0) return NEG_PREC;
  return ATOM_PREC;
}

export function formatNumber(v: number): string {
  if (Number.isInteger(v) && Math.abs(v) < 1e15) return String(v);
  return String(Number(v.toPrecision(6)));
}

export function toText(node: Node): string {
  switch (node.kind) {
    case 'num':
      return formatNumber(node.value);
    case 'const':
    case 'var':
      return node.name;
    case 'neg': {
      // -(a*b) = (-a)*b, so only sums need parentheses.
      const inner = toText(node.arg);
      return precOf(node.arg) < 2 || node.arg.kind === 'neg' ? `-(${inner})` : `-${inner}`;
    }
    case 'call':
      return `${node.fn}(${node.args.map(toText).join(', ')})`;
    case 'bin': {
      const p = PREC[node.op];
      const lp = precOf(node.left);
      const rp = precOf(node.right);
      const rightAssoc = node.op === '^';
      const leftNeedsParens = lp < p || (rightAssoc && lp === p);
      const rightIsNegative = node.right.kind === 'neg' || (node.right.kind === 'num' && node.right.value < 0);
      const rightNeedsParens =
        rightIsNegative || rp < p || (!rightAssoc && rp === p && (node.op !== '+' && node.op !== '*'));
      const l = leftNeedsParens ? `(${toText(node.left)})` : toText(node.left);
      const r = rightNeedsParens ? `(${toText(node.right)})` : toText(node.right);
      return node.op === '+' || node.op === '-' ? `${l} ${node.op} ${r}` : `${l}${node.op}${r}`;
    }
  }
}

// ---------------------------------------------------------------------------
// JavaScript

export function toJS(node: Node): string {
  switch (node.kind) {
    case 'num':
      return node.value < 0 ? `(${node.value})` : String(node.value);
    case 'const':
      return node.name === 'pi' ? 'Math.PI' : 'Math.E';
    case 'var':
      return node.name;
    case 'neg':
      return `(-${toJS(node.arg)})`;
    case 'call':
      return `Math.${node.fn}(${node.args.map(toJS).join(',')})`;
    case 'bin': {
      const l = toJS(node.left);
      const r = toJS(node.right);
      return node.op === '^' ? `Math.pow(${l},${r})` : `(${l}${node.op}${r})`;
    }
  }
}

/**
 * Compiles the AST to a native JavaScript function. Generated once per formula, then called
 * hundreds of thousands of times (e.g. when exporting STL), so it is much faster than walking the tree.
 */
export function compile(node: Node): CompiledFn {
  // The source contains only numbers, variable names, operators and Math.* calls from our own whitelist.
  return new Function('x', 'y', 'u', 'v', 't', `"use strict"; return ${toJS(node)};`) as CompiledFn;
}

// ---------------------------------------------------------------------------
// GLSL (WebGL2 / GLSL ES 3.00)

/**
 * Helper functions that make GLSL behave like Java/JavaScript Math where the built-ins differ:
 * GLSL pow() is undefined for negative bases, mod() uses floor instead of truncation,
 * and atan(0, 0) is undefined.
 */
export const GLSL_HELPERS = /* glsl */ `
float f_sq(float a) { return a * a; }
float f_cube(float a) { return a * a * a; }
float f_cbrt(float a) { return sign(a) * pow(abs(a), 1.0 / 3.0); }
float f_mod(float a, float b) { return a - b * trunc(a / b); }
float f_atan2(float y, float x) { return (x == 0.0 && y == 0.0) ? 0.0 : atan(y, x); }
float f_round(float a) { return floor(a + 0.5); }
float f_pow(float a, float b) {
  if (b == 0.0) return 1.0;
  if (a >= 0.0) return pow(a, b);
  if (b == floor(b)) {
    float r = pow(-a, b);
    return mod(b, 2.0) == 1.0 ? -r : r;
  }
  return sqrt(-1.0 - abs(a)); // NaN, like Math.pow(-2, 0.5)
}
`;

export function glslFloat(v: number): string {
  if (!Number.isFinite(v)) return '0.0';
  let s = Number.isInteger(v) && Math.abs(v) < 1e9 ? v.toFixed(1) : v.toPrecision(9);
  if (!/[.e]/.test(s)) s += '.0';
  return v < 0 ? `(${s})` : s;
}

const GLSL_CALL: Partial<Record<FnName, string>> = {
  cbrt: 'f_cbrt',
  atan2: 'f_atan2',
  round: 'f_round',
  pow: 'f_pow',
};

export function toGLSL(node: Node): string {
  switch (node.kind) {
    case 'num':
      return glslFloat(node.value);
    case 'const':
      return node.name === 'pi' ? '3.14159265' : '2.71828183';
    case 'var':
      return node.name;
    case 'neg':
      return `(-${toGLSL(node.arg)})`;
    case 'call': {
      const args = node.args.map(toGLSL);
      if (node.fn === 'log10') return `(log(${args[0]}) * 0.434294482)`;
      return `${GLSL_CALL[node.fn] ?? node.fn}(${args.join(', ')})`;
    }
    case 'bin': {
      const l = toGLSL(node.left);
      const r = toGLSL(node.right);
      switch (node.op) {
        case '^':
          if (node.right.kind === 'num') {
            if (node.right.value === 2) return `f_sq(${l})`;
            if (node.right.value === 3) return `f_cube(${l})`;
            if (node.right.value === 0.5) return `sqrt(${l})`;
          }
          return `f_pow(${l}, ${r})`;
        case '%':
          return `f_mod(${l}, ${r})`;
        default:
          return `(${l} ${node.op} ${r})`;
      }
    }
  }
}

// ---------------------------------------------------------------------------
// HTML (typeset caption)

const escapeHtml = (s: string) => s.replace(/[&<>"]/g, (c) => ({ '&': '&amp;', '<': '&lt;', '>': '&gt;', '"': '&quot;' })[c]!);

/**
 * Typesets a formula the way a textbook would: italic variables, a centred dot for
 * multiplication, superscript exponents and a radical sign for sqrt.
 */
export function toHTML(node: Node): string {
  switch (node.kind) {
    case 'num':
      return node.value < 0 ? `−${formatNumber(-node.value)}` : formatNumber(node.value);
    case 'const':
      return node.name === 'pi' ? 'π' : '<i>e</i>';
    case 'var':
      return `<i>${node.name}</i>`;
    case 'neg': {
      const inner = toHTML(node.arg);
      return precOf(node.arg) < 2 || node.arg.kind === 'neg' ? `−(${inner})` : `−${inner}`;
    }
    case 'call': {
      const args = node.args.map(toHTML).join(', ');
      if (node.fn === 'sqrt') return `√(${args})`;
      if (node.fn === 'abs') return `|${args}|`;
      return `${escapeHtml(node.fn)}(${args})`;
    }
    case 'bin': {
      const p = PREC[node.op];
      const lp = precOf(node.left);
      const rp = precOf(node.right);
      if (node.op === '^') {
        const base = lp <= p ? `(${toHTML(node.left)})` : toHTML(node.left);
        return `${base}<sup>${toHTML(node.right)}</sup>`;
      }
      const rightIsNegative = node.right.kind === 'neg' || (node.right.kind === 'num' && node.right.value < 0);
      const l = lp < p ? `(${toHTML(node.left)})` : toHTML(node.left);
      const r = rightIsNegative || rp < p || (rp === p && node.op !== '+' && node.op !== '*') ? `(${toHTML(node.right)})` : toHTML(node.right);
      const sym = { '+': ' + ', '-': ' − ', '*': '·', '/': ' / ', '%': ' mod ' }[node.op];
      return `${l}${sym}${r}`;
    }
  }
}

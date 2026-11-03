import { describe, expect, it } from 'vitest';
import { compile, toText } from './codegen';
import { parse, parseConstant } from './parser';
import { ParseError } from './tokenizer';

const XYT = ['x', 'y', 't'] as const;
const evalAt = (src: string, x = 0, y = 0, t = 0) => compile(parse(src, XYT))(x, y, 0, 0, t);

describe('parser: precedence and associativity', () => {
  it.each([
    ['2 + 3 * 4', 14],
    ['(2 + 3) * 4', 20],
    ['2 ^ 3 ^ 2', 512], // right-associative: 2^(3^2)
    ['-2 ^ 2', -4], // unary minus binds weaker than ^
    ['2 ^ -1', 0.5], // unary minus right after ^
    ['--3', 3],
    ['+5 - -5', 10],
    ['10 - 4 - 3', 3], // left-associative
    ['24 / 4 / 2', 3],
    ['7 % 4', 3],
    ['-7 % 4', -3], // truncated remainder, same as Java
    ['2pi', 2 * Math.PI], // implicit multiplication
    ['3(1 + 1)', 6],
    ['(1 + 1)(2 + 2)', 8],
    ['1.5e2', 150],
    ['.5 + .25', 0.75],
  ])('%s = %d', (src, expected) => {
    expect(evalAt(src)).toBeCloseTo(expected, 12);
  });

  it('evaluates variables and functions', () => {
    expect(evalAt('sin(x) * cos(y)', 1, 2)).toBeCloseTo(Math.sin(1) * Math.cos(2), 12);
    expect(evalAt('atan2(y, x)', -1, 1)).toBeCloseTo(Math.atan2(1, -1), 12);
    expect(evalAt('max(x, y) + min(x, y)', 3, 5)).toBe(8);
    expect(evalAt('pow(x, 3) - x^3', 1.7)).toBeCloseTo(0, 12);
    expect(evalAt('2x y', 3, 4)).toBe(24);
    expect(evalAt('x + t', 1, 0, 2)).toBe(3);
  });

  it('parses constant expressions for ranges', () => {
    expect(parseConstant('2*pi')).toBeCloseTo(2 * Math.PI, 12);
    expect(parseConstant('-3.5')).toBe(-3.5);
  });
});

describe('parser: errors point at the problem', () => {
  const errorOf = (src: string, vars: readonly ('x' | 'y' | 'u' | 'v' | 't')[] = XYT) => {
    try {
      parse(src, vars);
    } catch (e) {
      expect(e).toBeInstanceOf(ParseError);
      return e as ParseError;
    }
    throw new Error(`expected "${src}" to fail`);
  };

  it.each([
    ['', 'Type a formula'],
    ['sin(x', 'Missing closing parenthesis'],
    ['x + ', 'needs a value on its right'],
    ['* x', 'needs a value on its left'],
    ['x)', 'Unmatched'],
    ['foo(x)', 'Unknown name "foo"'],
    ['xy', 'Did you mean x*y'],
    ['sin x', 'with parentheses'],
    ['atan2(x)', 'takes 2 arguments'],
    ['sin(x, y)', 'takes 1 argument'],
    ['2 3', 'Missing operator'],
    ['()', 'Empty parentheses'],
    ['x # y', 'Unexpected character'],
    ['max(x,)', 'Missing'],
  ])('"%s" -> %s', (src, message) => {
    expect(errorOf(src).message).toContain(message);
  });

  it('rejects variables from the other mode', () => {
    const e = errorOf('u + x');
    expect(e.message).toContain('"u" can\'t be used here');
    expect(e.pos).toBe(0);
  });

  it('reports the position of the bad token', () => {
    expect(errorOf('x + foo').pos).toBe(4);
  });
});

describe('toText', () => {
  it.each([
    ['x - (y - 1)', 'x - (y - 1)'],
    ['(x^2)^3', '(x^2)^3'],
    ['x^(y^2)', 'x^y^2'],
    ['-(x + 1)', '-(x + 1)'],
    ['2^(-x)', '2^(-x)'],
    ['x / (y * 2)', 'x/(y*2)'],
    ['sin(x) * cos(y)', 'sin(x)*cos(y)'],
  ])('%s', (src, expected) => {
    expect(toText(parse(src, XYT))).toBe(expected);
  });

  it('round-trips: printing and re-parsing gives the same values', () => {
    for (const src of ['x - (y - 1) * 2', '-x^2 + 2^-y', 'atan2(y, x) / (1 + x*x)', '(x + 1)(y - 1) % 3']) {
      const printed = toText(parse(src, XYT));
      expect(evalAt(printed, 1.3, -0.7)).toBeCloseTo(evalAt(src, 1.3, -0.7), 12);
    }
  });
});

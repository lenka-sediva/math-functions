import { describe, expect, it } from 'vitest';
import { size } from './ast';
import { compile, toText } from './codegen';
import { derivative } from './derivative';
import { parse } from './parser';
import { simplify } from './simplify';

const XYT = ['x', 'y', 't'] as const;

/** Central difference, the method the Java version used for normals. */
function numeric(src: string, wrt: 'x' | 'y', x: number, y: number) {
  const f = compile(parse(src, XYT));
  const h = 1e-6;
  return wrt === 'x'
    ? (f(x + h, y, 0, 0, 0.3) - f(x - h, y, 0, 0, 0.3)) / (2 * h)
    : (f(x, y + h, 0, 0, 0.3) - f(x, y - h, 0, 0, 0.3)) / (2 * h);
}

const FORMULAS = [
  'sin(x) * cos(y)',
  'x*x - y*y',
  'sin(sqrt(x*x+y*y)) / (sqrt(x*x+y*y)+0.01)',
  '(1-(x*x+y*y)/4)*exp(-(x*x+y*y)/4)',
  'sin(x+t)*cos(y+t)',
  'sin(x)+cos(y)+sin(x*y)',
  'sin(atan2(y,x)*3+sqrt(x*x+y*y)-t*2)',
  'tan(x/3) + atan(y) + asin(x/9) - acos(y/9)',
  'sinh(x/3) * cosh(y/4) + tanh(x*y)',
  'log(x*x + 1) + log10(y*y + 2) + log2(x*x + y*y + 3)',
  'cbrt(x*y + 5) + pow(x, 3) + 2^x + (x*x + 1)^y',
  'abs(x - 0.3) + max(x, y) - min(x, 2y)',
  '-x^2 / (1 + y^2)',
  'exp(sin(x*y)) ^ 2',
];

describe('symbolic derivative matches numerical differentiation', () => {
  const points = [
    [0.7, 1.3],
    [-1.1, 0.4],
    [2.2, -1.9],
  ];
  for (const src of FORMULAS) {
    it(src, () => {
      const f = parse(src, XYT);
      const fx = compile(derivative(f, 'x'));
      const fy = compile(derivative(f, 'y'));
      for (const [x, y] of points) {
        const nx = numeric(src, 'x', x, y);
        const ny = numeric(src, 'y', x, y);
        expect(fx(x, y, 0, 0, 0.3)).toBeCloseTo(nx, 4);
        expect(fy(x, y, 0, 0, 0.3)).toBeCloseTo(ny, 4);
      }
    });
  }
});

describe('derivatives are simplified', () => {
  it.each([
    ['x^2', 'x', '2*x'],
    ['x*x - y*y', 'x', '2*x'],
    ['x*x - y*y', 'y', '-2*y'],
    ['sin(x) * cos(y)', 'x', 'cos(x)*cos(y)'],
    ['3*x + 5', 'x', '3'],
    ['exp(2*x)', 'x', '2*exp(2*x)'],
    ['y^3', 'x', '0'],
  ])('%s, d/d%s', (src, wrt, expected) => {
    expect(toText(derivative(parse(src, XYT), wrt as 'x' | 'y'))).toBe(expected);
  });

  it('keeps the tree small for the presets', () => {
    for (const src of FORMULAS) {
      const f = parse(src, XYT);
      expect(size(derivative(f, 'x'))).toBeLessThan(20 * size(f));
    }
  });

  it('simplify does not change values', () => {
    for (const src of FORMULAS) {
      const f = parse(src, XYT);
      const a = compile(f)(0.9, -0.6, 0, 0, 0.3);
      const b = compile(simplify(f))(0.9, -0.6, 0, 0, 0.3);
      expect(b).toBeCloseTo(a, 10);
    }
  });
});

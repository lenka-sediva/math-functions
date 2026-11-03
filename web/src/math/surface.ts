import { variablesOf, type Node, type VarName } from './ast';
import { compile, toGLSL, type CompiledFn } from './codegen';
import { derivative } from './derivative';
import { parse, parseConstant } from './parser';
import { ParseError } from './tokenizer';

/**
 * A surface is described the same way in both modes: a point P(a, b, t) and its two
 * partial derivatives ∂P/∂a and ∂P/∂b. The normal is their cross product.
 *
 *  - explicit  z = f(x, y):      P = (x, y, f),  ∂P/∂x = (1, 0, f_x),  ∂P/∂y = (0, 1, f_y)
 *  - parametric (x, y, z)(u, v): P = (X, Y, Z),  ∂P/∂u, ∂P/∂v from symbolic derivatives
 *
 * This keeps the renderer, STL export and point inspector mode-agnostic.
 */

export type Mode = 'explicit' | 'parametric';
export type Vec3 = [number, number, number];
type Triple<T> = [T, T, T];

export interface SurfaceInput {
  mode: Mode;
  f: string;
  px: string;
  py: string;
  pz: string;
  /** Range of the first parameter (x or u) as text, so "2*pi" works. */
  aRange: [string, string];
  /** Range of the second parameter (y or v). */
  bRange: [string, string];
}

export type InputField = 'f' | 'px' | 'py' | 'pz' | 'a0' | 'a1' | 'b0' | 'b1';

export interface FieldError {
  field: InputField;
  message: string;
  pos: number;
  length: number;
}

export interface ExplicitParts {
  f: Node;
  fx: Node;
  fy: Node;
  fxx: CompiledFn;
  fxy: CompiledFn;
  fyy: CompiledFn;
}

export interface Surface {
  mode: Mode;
  aRange: [number, number];
  bRange: [number, number];
  P: Triple<Node>;
  Pa: Triple<Node>;
  Pb: Triple<Node>;
  usesTime: boolean;
  /** GLSL source defining surfP, surfPa and surfPb. */
  glsl: string;
  /** Evaluates the point and both tangents on the CPU. Returns false if the point is undefined. */
  evaluate(a: number, b: number, t: number, out: SurfaceSample): boolean;
  /** Only for z = f(x, y): the formula and its derivatives. */
  explicit?: ExplicitParts;
}

export interface SurfaceSample {
  p: Vec3;
  pa: Vec3;
  pb: Vec3;
}

export const newSample = (): SurfaceSample => ({ p: [0, 0, 0], pa: [0, 0, 0], pb: [0, 0, 0] });

export const PARAMS: Record<Mode, [VarName, VarName]> = { explicit: ['x', 'y'], parametric: ['u', 'v'] };

export function buildSurface(input: SurfaceInput): { surface: Surface; errors: [] } | { surface: null; errors: FieldError[] } {
  const errors: FieldError[] = [];
  const [pa, pb] = PARAMS[input.mode];
  const allowed: VarName[] = [pa, pb, 't'];

  const parseField = (field: InputField, src: string): Node | null => {
    try {
      return parse(src, allowed);
    } catch (e) {
      errors.push(toFieldError(field, e));
      return null;
    }
  };
  const parseRange = (field: InputField, src: string): number | null => {
    try {
      const v = parseConstant(src);
      if (!Number.isFinite(v)) throw new ParseError('The range must be a finite number', 0, src.length);
      return v;
    } catch (e) {
      errors.push(toFieldError(field, e));
      return null;
    }
  };

  let P: Triple<Node | null>;
  if (input.mode === 'explicit') {
    P = [{ kind: 'var', name: 'x' }, { kind: 'var', name: 'y' }, parseField('f', input.f)];
  } else {
    P = [parseField('px', input.px), parseField('py', input.py), parseField('pz', input.pz)];
  }

  const a0 = parseRange('a0', input.aRange[0]);
  const a1 = parseRange('a1', input.aRange[1]);
  const b0 = parseRange('b0', input.bRange[0]);
  const b1 = parseRange('b1', input.bRange[1]);
  if (a0 !== null && a1 !== null && a0 >= a1) errors.push({ field: 'a1', message: `The end of the ${pa} range must be greater than its start`, pos: 0, length: 0 });
  if (b0 !== null && b1 !== null && b0 >= b1) errors.push({ field: 'b1', message: `The end of the ${pb} range must be greater than its start`, pos: 0, length: 0 });

  if (errors.length || P.some((n) => n === null)) return { surface: null, errors };
  const Pn = P as Triple<Node>;

  const Pa = Pn.map((n) => derivative(n, pa)) as Triple<Node>;
  const Pb = Pn.map((n) => derivative(n, pb)) as Triple<Node>;
  const usesTime = Pn.some((n) => variablesOf(n).has('t'));

  const jsP = Pn.map(compile) as Triple<CompiledFn>;
  const jsPa = Pa.map(compile) as Triple<CompiledFn>;
  const jsPb = Pb.map(compile) as Triple<CompiledFn>;
  const isExplicit = input.mode === 'explicit';

  const evaluate = (a: number, b: number, t: number, out: SurfaceSample): boolean => {
    const x = isExplicit ? a : 0;
    const y = isExplicit ? b : 0;
    const u = isExplicit ? 0 : a;
    const v = isExplicit ? 0 : b;
    let ok = true;
    for (let i = 0; i < 3; i++) {
      out.p[i] = jsP[i](x, y, u, v, t);
      out.pa[i] = jsPa[i](x, y, u, v, t);
      out.pb[i] = jsPb[i](x, y, u, v, t);
      if (!Number.isFinite(out.p[i])) ok = false;
    }
    return ok;
  };

  let explicit: ExplicitParts | undefined;
  if (isExplicit) {
    const f = Pn[2];
    const fx = Pa[2];
    const fy = Pb[2];
    explicit = {
      f,
      fx,
      fy,
      fxx: compile(derivative(fx, 'x')),
      fxy: compile(derivative(fx, 'y')),
      fyy: compile(derivative(fy, 'y')),
    };
  }

  const surface: Surface = {
    mode: input.mode,
    aRange: [a0!, a1!],
    bRange: [b0!, b1!],
    P: Pn,
    Pa,
    Pb,
    usesTime,
    glsl: surfaceGLSL(input.mode, Pn, Pa, Pb),
    evaluate,
    explicit,
  };
  return { surface, errors: [] };
}

function toFieldError(field: InputField, e: unknown): FieldError {
  if (e instanceof ParseError) return { field, message: e.message, pos: e.pos, length: e.length };
  return { field, message: String((e as Error)?.message ?? e), pos: 0, length: 0 };
}

/** Generates the GLSL functions that the vertex shader calls for every vertex. */
function surfaceGLSL(mode: Mode, P: Triple<Node>, Pa: Triple<Node>, Pb: Triple<Node>): string {
  const head =
    mode === 'explicit'
      ? 'float x = a; float y = b; float u = 0.0; float v = 0.0;'
      : 'float x = 0.0; float y = 0.0; float u = a; float v = b;';
  const fn = (name: string, c: Triple<Node>) =>
    `vec3 ${name}(float a, float b, float t) {\n  ${head}\n  return vec3(${c.map(toGLSL).join(',\n    ')});\n}`;
  return [fn('surfP', P), fn('surfPa', Pa), fn('surfPb', Pb)].join('\n\n');
}

// ---------------------------------------------------------------------------
// Measuring the surface: bounding box, outlier clipping, colour range

export interface Frame {
  /** Box that contains the visible surface (math coordinates). */
  boxMin: Vec3;
  boxMax: Vec3;
  /** Vertices outside these bounds are hidden (cuts off spikes like 1/x or tan). */
  clipMin: Vec3;
  clipMax: Vec3;
  /** False if the formula has no real values anywhere in the range. */
  hasValues: boolean;
}

/**
 * Samples the surface on a coarse grid (and over a few moments in time when animated)
 * to find a stable box for the camera, colours and clipping.
 *
 * Outliers are detected with percentiles, so a single pole like 1/x doesn't squash the whole plot.
 */
export function measure(surface: Surface, n = 64): Frame {
  const times = surface.usesTime ? [0, 1, 2, 3, 4, 5].map((k) => (k * Math.PI) / 3) : [0];
  const axes: number[][] = [[], [], []];
  const s = newSample();
  const [a0, a1] = surface.aRange;
  const [b0, b1] = surface.bRange;
  for (const t of times) {
    for (let i = 0; i <= n; i++) {
      for (let j = 0; j <= n; j++) {
        surface.evaluate(a0 + ((a1 - a0) * i) / n, b0 + ((b1 - b0) * j) / n, t, s);
        if (s.p.every(Number.isFinite)) for (let k = 0; k < 3; k++) axes[k].push(s.p[k]);
      }
    }
  }

  if (axes[0].length === 0) {
    return { boxMin: [-1, -1, -1], boxMax: [1, 1, 1], clipMin: [-1, -1, -1], clipMax: [1, 1, 1], hasValues: false };
  }

  const boxMin: Vec3 = [0, 0, 0];
  const boxMax: Vec3 = [0, 0, 0];
  const clipMin: Vec3 = [0, 0, 0];
  const clipMax: Vec3 = [0, 0, 0];
  for (let k = 0; k < 3; k++) {
    const values = Float64Array.from(axes[k]).sort();
    const lo = quantile(values, 0.01);
    const hi = quantile(values, 0.99);
    const span = Math.max(hi - lo, 1e-9);
    clipMin[k] = lo - span;
    clipMax[k] = hi + span;
    // Actual extent of everything that survives clipping.
    let mn = Infinity;
    let mx = -Infinity;
    for (const v of values) {
      if (v >= clipMin[k] && v <= clipMax[k]) {
        if (v < mn) mn = v;
        if (v > mx) mx = v;
      }
    }
    if (mx - mn < 1e-9) {
      // A flat surface (e.g. z = 1): give it a little height so the box isn't degenerate.
      mn -= 1;
      mx += 1;
      clipMin[k] = Math.min(clipMin[k], mn);
      clipMax[k] = Math.max(clipMax[k], mx);
    }
    boxMin[k] = mn;
    boxMax[k] = mx;
  }

  if (surface.mode === 'explicit') {
    // x and y are exactly the domain; never clip them.
    for (const k of [0, 1] as const) {
      const r = k === 0 ? surface.aRange : surface.bRange;
      boxMin[k] = r[0];
      boxMax[k] = r[1];
      clipMin[k] = -1e30; // effectively unbounded, but still a finite value for GPU uniforms
      clipMax[k] = 1e30;
    }
  }
  return { boxMin, boxMax, clipMin, clipMax, hasValues: true };
}

function quantile(sorted: Float64Array, q: number): number {
  const idx = Math.min(sorted.length - 1, Math.max(0, Math.round(q * (sorted.length - 1))));
  return sorted[idx];
}

const scratch = newSample();

/**
 * CPU twin of the shader test: is there a pole between this grid point and a neighbour?
 * A jump bigger than maxJump that goes against the local slope means the function
 * shot off to infinity and came back from the other side (tan, 1/x).
 */
export function poleNearby(surface: Surface, a: number, b: number, t: number, s: SurfaceSample, h: number, k: number, maxJump: number): boolean {
  const z = s.p[2];
  const za = s.pa[2];
  const zb = s.pb[2];
  const against = (na: number, nb: number, slopeStep: number) => {
    surface.evaluate(na, nb, t, scratch);
    const d = scratch.p[2] - z;
    return Math.abs(d) > maxJump && d * slopeStep < 0;
  };
  return against(a + h, b, za * h) || against(a - h, b, -za * h) || against(a, b + k, zb * k) || against(a, b - k, -zb * k);
}

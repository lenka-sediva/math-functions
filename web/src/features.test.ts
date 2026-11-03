import { describe, expect, it } from 'vitest';
import { exportSTL } from './export/stl';
import { classify, findCriticalPoints } from './math/critical';
import { buildSurface, measure, newSample, poleNearby, type SurfaceInput } from './math/surface';
import { PRESETS } from './presets';
import { DEFAULT_STATE, cloneState, decodeState, encodeState } from './state';

const explicit = (f: string, r: [string, string] = ['-2', '2']): SurfaceInput => ({
  mode: 'explicit', f, px: '', py: '', pz: '', aRange: r, bRange: r,
});

const build = (input: SurfaceInput) => {
  const res = buildSurface(input);
  if (!res.surface) throw new Error(res.errors.map((e) => e.message).join('; '));
  return res.surface;
};

describe('surfaces', () => {
  it('every preset builds and generates GLSL', () => {
    for (const p of PRESETS) {
      const s = build({ mode: p.mode, f: p.f ?? '', px: p.px ?? '', py: p.py ?? '', pz: p.pz ?? '', aRange: p.a, bRange: p.b });
      expect(s.glsl).toContain('vec3 surfP(');
      expect(s.glsl).not.toMatch(/NaN|Infinity|undefined/);
      expect(measure(s, 16).hasValues).toBe(true);
    }
  });

  it('reports errors per field', () => {
    const res = buildSurface({ ...explicit('sin(x'), aRange: ['3', '1'] });
    expect(res.errors.map((e) => e.field).sort()).toEqual(['a1', 'f']);
  });

  it('clips poles like 1/x instead of letting them squash the plot', () => {
    const frame = measure(build(explicit('1/x')));
    expect(frame.boxMax[2]).toBeLessThan(1000);
    expect(frame.boxMin[2]).toBeGreaterThan(-1000);
  });

  it('detects poles between grid points but not steep smooth slopes', () => {
    const tan = build(explicit('tan(x)', ['-3', '3']));
    const s = newSample();
    const span = 20;
    tan.evaluate(Math.PI / 2 - 0.03, 0, 0, s); // just left of the pole, next vertex is past it
    expect(poleNearby(tan, Math.PI / 2 - 0.03, 0, 0, s, 0.06, 0.06, 0.5 * span)).toBe(true);
    tan.evaluate(0.3, 0, 0, s);
    expect(poleNearby(tan, 0.3, 0, 0, s, 0.06, 0.06, 0.5 * span)).toBe(false);

    // A coarse grid over a steep saddle has big jumps, but they follow the slope.
    const saddle = build(explicit('x*x - y*y', ['-6', '6']));
    for (const [a, b] of [[-6, -6], [6, 0], [-4.8, 3.6]]) {
      saddle.evaluate(a, b, 0, s);
      expect(poleNearby(saddle, a, b, 0, s, 1.2, 1.2, 0.5 * 72)).toBe(false);
    }
  });

  it('notices formulas without real values', () => {
    expect(measure(build(explicit('sqrt(-1 - x*x)'))).hasValues).toBe(false);
  });
});

describe('critical points', () => {
  it('classifies with the Hessian determinant', () => {
    expect(classify(-2, 0, -2)).toBe('maximum');
    expect(classify(2, 0, 2)).toBe('minimum');
    expect(classify(2, 0, -2)).toBe('saddle');
    expect(classify(0, 0, 0)).toBe('degenerate');
  });

  it('finds the saddle of x² - y²', () => {
    const pts = findCriticalPoints(build(explicit('x*x - y*y')), 0);
    expect(pts).toHaveLength(1);
    expect(pts[0].kind).toBe('saddle');
    expect(Math.abs(pts[0].x) + Math.abs(pts[0].y)).toBeLessThan(1e-9);
  });

  it('finds the four critical points of sin(x)cos(y) in (-2, 2)²', () => {
    // Maximum at (π/2, 0), minimum at (-π/2, 0), saddles at (0, ±π/2).
    const pts = findCriticalPoints(build(explicit('sin(x) * cos(y)', ['-2', '2'])), 0);
    const kinds = pts.map((p) => p.kind).sort();
    expect(kinds).toEqual(['maximum', 'minimum', 'saddle', 'saddle']);
  });

  it('works for the monkey saddle (degenerate)', () => {
    const pts = findCriticalPoints(build(explicit('x^3 - 3x*y^2')), 0);
    // Newton converges slowly at a degenerate point; it may or may not be reported, but never misclassified.
    for (const p of pts) expect(p.kind).toBe('degenerate');
  });
});

describe('STL export', () => {
  /** In a closed (watertight) mesh, every edge is shared by exactly two triangles. */
  function edgeCounts(buf: ArrayBuffer) {
    const view = new DataView(buf);
    const n = view.getUint32(80, true);
    const key = (o: number) => [0, 4, 8].map((k) => view.getFloat32(o + k, true).toFixed(4)).join(',');
    const edges = new Map<string, number>();
    for (let i = 0; i < n; i++) {
      const o = 84 + i * 50 + 12;
      const v = [key(o), key(o + 12), key(o + 24)];
      for (let k = 0; k < 3; k++) {
        const e = [v[k], v[(k + 1) % 3]].sort().join('|');
        edges.set(e, (edges.get(e) ?? 0) + 1);
      }
    }
    return { n, counts: [...edges.values()] };
  }

  it('writes a valid binary file', () => {
    const s = build(explicit('x*y'));
    const r = exportSTL(s, { resolution: 10, t: 0, scale: [1, 1, 1], solid: false, sizeMm: 100 }, () => true);
    expect(r.triangles).toBe(200);
    expect(r.data.byteLength).toBe(84 + 50 * 200);
  });

  it('closes explicit surfaces into a watertight solid', () => {
    const s = build(explicit('sin(x) * cos(y)'));
    const r = exportSTL(s, { resolution: 12, t: 0, scale: [1, 1, 1], solid: true, sizeMm: 100 }, () => true);
    const { counts } = edgeCounts(r.data);
    expect(counts.every((c) => c === 2)).toBe(true);
  });

  it('scales the largest side to the requested size', () => {
    const s = build(explicit('0.1*x'));
    const r = exportSTL(s, { resolution: 4, t: 0, scale: [1, 1, 1], solid: false, sizeMm: 50 }, () => true);
    const view = new DataView(r.data);
    let mx = -Infinity;
    let mn = Infinity;
    for (let i = 0; i < r.triangles; i++) {
      for (let k = 0; k < 3; k++) {
        const x = view.getFloat32(84 + i * 50 + 12 + k * 12, true);
        mx = Math.max(mx, x);
        mn = Math.min(mn, x);
      }
    }
    expect(mx - mn).toBeCloseTo(50, 3);
  });

  it('falls back to an open surface when points are undefined', () => {
    const s = build(explicit('sqrt(x)'));
    const r = exportSTL(s, { resolution: 8, t: 0, scale: [1, 1, 1], solid: true, sizeMm: 100 }, (_a, _b, _t, s) => s.p.every(Number.isFinite));
    expect(r.solidSkipped).toBe(true);
    expect(r.triangles).toBeGreaterThan(0);
  });
});

describe('shareable links', () => {
  it('round-trips the full state', () => {
    const s = cloneState(DEFAULT_STATE);
    s.f = 'sin(x)*cos(y) + 1/(1+x^2)';
    s.xr = ['-2pi', '2pi'];
    s.res = 220;
    s.playing = false;
    s.show.wireframe = true;
    s.show.floor = false;
    s.orthographic = true;
    s.camera = [0.5, 0.25, 1.2];
    expect(decodeState(encodeState(s))).toEqual(s);
  });

  it('round-trips parametric surfaces', () => {
    const s = cloneState(DEFAULT_STATE);
    s.mode = 'parametric';
    s.px = 'v*cos(u)';
    s.ur = ['-pi', 'pi'];
    const back = decodeState(encodeState(s));
    expect(back.mode).toBe('parametric');
    expect(back.px).toBe('v*cos(u)');
    expect(back.ur).toEqual(['-pi', 'pi']);
  });

  it('keeps default links short', () => {
    expect(encodeState(DEFAULT_STATE)).toBe(new URLSearchParams({ f: DEFAULT_STATE.f }).toString());
  });

  it('ignores garbage', () => {
    const s = decodeState('res=abc&cam=1,2&speed=-5&x=1');
    expect(s.res).toBe(DEFAULT_STATE.res);
    expect(s.camera).toBeNull();
    expect(s.xr).toEqual(DEFAULT_STATE.xr);
  });
});

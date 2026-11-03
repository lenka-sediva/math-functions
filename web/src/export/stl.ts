import { newSample, type Surface, type SurfaceSample, type Vec3 } from '../math/surface';

export interface StlOptions {
  resolution: number;
  t: number;
  /** Display scale (z can be squashed on screen); the export matches what you see. */
  scale: Vec3;
  /** For z = f(x, y): add side walls and a bottom so the model is a printable solid. */
  solid: boolean;
  /** Largest dimension of the model in millimetres. */
  sizeMm: number;
}

export interface StlResult {
  data: ArrayBuffer;
  triangles: number;
  /** True if the solid option was requested but skipped because of undefined points. */
  solidSkipped: boolean;
  /** Triangles dropped because a vertex was undefined. */
  dropped: number;
}

type Tri = [Vec3, Vec3, Vec3];

/**
 * Builds a binary STL (the format slicers for 3D printers read):
 * 80-byte header, triangle count, then 50 bytes per triangle (normal + 3 vertices + 2 spare bytes).
 */
export function exportSTL(surface: Surface, opts: StlOptions, isVisible: (a: number, b: number, t: number, s: SurfaceSample, resolution: number) => boolean): StlResult {
  const n = Math.max(2, Math.round(opts.resolution));
  const [a0, a1] = surface.aRange;
  const [b0, b1] = surface.bRange;
  const grid: (Vec3 | null)[] = [];
  const s = newSample();
  for (let j = 0; j <= n; j++) {
    for (let i = 0; i <= n; i++) {
      const a = a0 + ((a1 - a0) * i) / n;
      const b = b0 + ((b1 - b0) * j) / n;
      const ok = surface.evaluate(a, b, opts.t, s) && isVisible(a, b, opts.t, s, n);
      grid.push(ok ? [s.p[0] * opts.scale[0], s.p[1] * opts.scale[1], s.p[2] * opts.scale[2]] : null);
    }
  }
  const at = (i: number, j: number) => grid[j * (n + 1) + i];

  const tris: Tri[] = [];
  let dropped = 0;
  for (let j = 0; j < n; j++) {
    for (let i = 0; i < n; i++) {
      for (const [p, q, r] of [
        [at(i, j), at(i + 1, j), at(i + 1, j + 1)],
        [at(i, j), at(i + 1, j + 1), at(i, j + 1)],
      ]) {
        if (p && q && r) tris.push([p, q, r]);
        else dropped++;
      }
    }
  }

  const wantSolid = opts.solid && surface.mode === 'explicit';
  const solidSkipped = wantSolid && dropped > 0;
  if (wantSolid && !solidSkipped) addBaseAndWalls(tris, grid as Vec3[], n);

  normalizeSize(tris, opts.sizeMm);
  return { data: writeBinary(tris), triangles: tris.length, solidSkipped, dropped };
}

/** Closes an explicit surface into a watertight solid: four walls down to a flat bottom. */
function addBaseAndWalls(tris: Tri[], grid: Vec3[], n: number) {
  const at = (i: number, j: number) => grid[j * (n + 1) + i];
  let zMin = Infinity;
  let zMax = -Infinity;
  for (const p of grid) {
    zMin = Math.min(zMin, p[2]);
    zMax = Math.max(zMax, p[2]);
  }
  const xySpan = Math.max(Math.abs(at(n, 0)[0] - at(0, 0)[0]), Math.abs(at(0, n)[1] - at(0, 0)[1]));
  const base = zMin - Math.max(0.08 * xySpan, 0.1 * (zMax - zMin));
  const down = (p: Vec3): Vec3 => [p[0], p[1], base];

  // Bottom: same grid, flat and facing down, so its edges match the walls exactly.
  for (let j = 0; j < n; j++) {
    for (let i = 0; i < n; i++) {
      const p = down(at(i, j));
      const q = down(at(i + 1, j));
      const r = down(at(i + 1, j + 1));
      const s = down(at(i, j + 1));
      tris.push([p, r, q], [p, s, r]);
    }
  }

  // Walls along the border, walked counter-clockwise when seen from above.
  const border: Vec3[] = [];
  for (let i = 0; i < n; i++) border.push(at(i, 0));
  for (let j = 0; j < n; j++) border.push(at(n, j));
  for (let i = n; i > 0; i--) border.push(at(i, n));
  for (let j = n; j > 0; j--) border.push(at(0, j));
  for (let k = 0; k < border.length; k++) {
    const p = border[k];
    const q = border[(k + 1) % border.length];
    tris.push([p, down(p), down(q)], [p, down(q), q]);
  }
}

/** Centres the model on the build plate and scales the largest side to sizeMm. */
function normalizeSize(tris: Tri[], sizeMm: number) {
  const mn: Vec3 = [Infinity, Infinity, Infinity];
  const mx: Vec3 = [-Infinity, -Infinity, -Infinity];
  for (const t of tris) for (const p of t) for (let k = 0; k < 3; k++) {
    mn[k] = Math.min(mn[k], p[k]);
    mx[k] = Math.max(mx[k], p[k]);
  }
  const largest = Math.max(mx[0] - mn[0], mx[1] - mn[1], mx[2] - mn[2]);
  if (!(largest > 0)) return;
  const k = sizeMm / largest;
  const cx = (mn[0] + mx[0]) / 2;
  const cy = (mn[1] + mx[1]) / 2;
  for (const t of tris) {
    for (let i = 0; i < 3; i++) {
      const p = t[i];
      t[i] = [(p[0] - cx) * k, (p[1] - cy) * k, (p[2] - mn[2]) * k];
    }
  }
}

function writeBinary(tris: Tri[]): ArrayBuffer {
  const buf = new ArrayBuffer(84 + tris.length * 50);
  const view = new DataView(buf);
  const header = 'Math Functions 3D - binary STL';
  for (let i = 0; i < header.length; i++) view.setUint8(i, header.charCodeAt(i));
  view.setUint32(80, tris.length, true);
  let o = 84;
  for (const [a, b, c] of tris) {
    const e1 = [b[0] - a[0], b[1] - a[1], b[2] - a[2]];
    const e2 = [c[0] - a[0], c[1] - a[1], c[2] - a[2]];
    let nx = e1[1] * e2[2] - e1[2] * e2[1];
    let ny = e1[2] * e2[0] - e1[0] * e2[2];
    let nz = e1[0] * e2[1] - e1[1] * e2[0];
    const l = Math.hypot(nx, ny, nz) || 1;
    nx /= l;
    ny /= l;
    nz /= l;
    for (const v of [nx, ny, nz, ...a, ...b, ...c]) {
      view.setFloat32(o, v, true);
      o += 4;
    }
    view.setUint16(o, 0, true);
    o += 2;
  }
  return buf;
}

import { compile } from './codegen';
import type { Surface } from './surface';

export type CriticalKind = 'maximum' | 'minimum' | 'saddle' | 'degenerate';

export interface CriticalPoint {
  x: number;
  y: number;
  z: number;
  kind: CriticalKind;
}

/**
 * Finds points where ∇f = 0 for z = f(x, y) and classifies them with the second-derivative test.
 *
 * Newton's method on the gradient: H · Δ = -∇f, where H is the Hessian built from the
 * symbolic second derivatives. Started from a grid of seeds; duplicates are merged.
 */
export function findCriticalPoints(surface: Surface, t: number, seeds = 24, limit = 60): CriticalPoint[] {
  const ex = surface.explicit;
  if (!ex) return [];
  const f = compile(ex.f);
  const fx = compile(ex.fx);
  const fy = compile(ex.fy);
  const { fxx, fxy, fyy } = ex;
  const [x0, x1] = surface.aRange;
  const [y0, y1] = surface.bRange;
  const span = Math.max(x1 - x0, y1 - y0);
  const found: CriticalPoint[] = [];

  for (let i = 0; i <= seeds; i++) {
    for (let j = 0; j <= seeds; j++) {
      let x = x0 + ((x1 - x0) * (i + 0.5)) / (seeds + 1);
      let y = y0 + ((y1 - y0) * (j + 0.5)) / (seeds + 1);
      let converged = false;
      for (let it = 0; it < 40; it++) {
        const gx = fx(x, y, 0, 0, t);
        const gy = fy(x, y, 0, 0, t);
        const a = fxx(x, y, 0, 0, t);
        const b = fxy(x, y, 0, 0, t);
        const d = fyy(x, y, 0, 0, t);
        const det = a * d - b * b;
        if (!Number.isFinite(det) || Math.abs(det) < 1e-14) break;
        // Solve the 2×2 system by Cramer's rule.
        const dx = (-gx * d + gy * b) / det;
        const dy = (-a * gy + b * gx) / det;
        x += dx;
        y += dy;
        if (!(x >= x0 && x <= x1 && y >= y0 && y <= y1)) break;
        if (Math.hypot(dx, dy) < 1e-10 * span) {
          converged = true;
          break;
        }
      }
      if (!converged) continue;
      const gx = fx(x, y, 0, 0, t);
      const gy = fy(x, y, 0, 0, t);
      const z = f(x, y, 0, 0, t);
      if (!Number.isFinite(z) || Math.hypot(gx, gy) > 1e-6 * (1 + Math.abs(z))) continue;
      if (found.some((p) => Math.hypot(p.x - x, p.y - y) < 1e-5 * span)) continue;
      found.push({ x, y, z, kind: classify(fxx(x, y, 0, 0, t), fxy(x, y, 0, 0, t), fyy(x, y, 0, 0, t)) });
      if (found.length >= limit) return sortPoints(found);
    }
  }
  return sortPoints(found);
}

/** Second-derivative test with the Hessian determinant D = f_xx f_yy - f_xy². */
export function classify(fxx: number, fxy: number, fyy: number): CriticalKind {
  const D = fxx * fyy - fxy * fxy;
  const eps = 1e-9 * (Math.abs(fxx) + Math.abs(fyy) + Math.abs(fxy) + 1e-12);
  if (D > eps) return fxx < 0 ? 'maximum' : 'minimum';
  if (D < -eps) return 'saddle';
  return 'degenerate';
}

const ORDER: Record<CriticalKind, number> = { maximum: 0, minimum: 1, saddle: 2, degenerate: 3 };

function sortPoints(points: CriticalPoint[]): CriticalPoint[] {
  return points.sort((p, q) => ORDER[p.kind] - ORDER[q.kind] || q.z - p.z);
}

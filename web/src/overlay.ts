import type { CriticalKind, CriticalPoint } from './math/critical';
import { newSample, type Vec3 } from './math/surface';
import { v3 } from './render/mat4';
import { COLORS, type Renderer } from './render/renderer';

/**
 * 2D annotations drawn on a transparent canvas above the WebGL view: axis labels, the
 * inspected point with its arrows, and critical-point markers. Canvas 2D gives crisp,
 * thick lines and text, which WebGL lines (always 1 px) can't.
 */

export const CRITICAL_STYLE: Record<CriticalKind, { color: string; label: string }> = {
  maximum: { color: '#e0603f', label: 'Maximum' },
  minimum: { color: '#2f6fb0', label: 'Minimum' },
  saddle: { color: '#f0b84a', label: 'Saddle point' },
  degenerate: { color: '#9aa3a6', label: 'Unclassified' },
};

export interface OverlayInput {
  renderer: Renderer;
  t: number;
  showAxes: boolean;
  critical: CriticalPoint[];
}

export function drawOverlay(ctx: CanvasRenderingContext2D, { renderer, t, showAxes, critical }: OverlayInput) {
  const canvas = ctx.canvas;
  const dpr = canvas.width / Math.max(1, canvas.getBoundingClientRect().width);
  ctx.setTransform(1, 0, 0, 1, 0, 0);
  ctx.clearRect(0, 0, canvas.width, canvas.height);
  ctx.setTransform(dpr, 0, 0, dpr, 0, 0);
  const surface = renderer.surface;
  if (!surface || !renderer.frame) return;

  if (showAxes) {
    const ends = renderer.axisEnds();
    ctx.font = 'italic 20px "STIX Two Text", Georgia, serif';
    ctx.textAlign = 'center';
    ctx.textBaseline = 'middle';
    for (const [name, p, color] of [
      ['x', ends.x, COLORS.axisX],
      ['y', ends.y, COLORS.axisY],
      ['z', ends.z, COLORS.axisZ],
    ] as const) {
      const s = renderer.project(p);
      if (!s) continue;
      ctx.strokeStyle = COLORS.paper;
      ctx.lineWidth = 5;
      ctx.lineJoin = 'round';
      ctx.strokeText(name, s.x, s.y);
      ctx.fillStyle = color;
      ctx.fillText(name, s.x, s.y);
    }
  }

  for (const c of critical) {
    const p: Vec3 = [c.x, c.y, c.z];
    if (!renderer.isVisible(p)) continue;
    const s = renderer.project(p);
    if (s) marker(ctx, c.kind, s.x, s.y, 7);
  }

  const ip = renderer.inspected;
  const sample = newSample();
  if (ip && surface.evaluate(ip.a, ip.b, t, sample) && renderer.isVisible(sample.p)) {
    const pw = renderer.toWorld(sample.p);
    const len = renderer.radius * 0.24;
    const arrowTo = (dirMath: Vec3, color: string, label: string) => {
      const dw = v3.normalize(v3.mul(dirMath, renderer.scale));
      if (v3.len(dw) === 0) return;
      const a = renderer.project(sample.p);
      const b = renderer.project(renderer.fromWorld(v3.add(pw, v3.scale(dw, len))));
      if (a && b) arrow(ctx, a.x, a.y, b.x, b.y, color, label);
    };

    if (surface.mode === 'explicit') {
      // Steepest ascent along the surface: move by ∇f in the plane, climb by |∇f|².
      const [, , fx] = sample.pa;
      const [, , fy] = sample.pb;
      if (Math.hypot(fx, fy) > 1e-9) arrowTo([fx, fy, fx * fx + fy * fy], COLORS.accent, 'uphill');
    } else {
      arrowTo(sample.pa as Vec3, COLORS.axisX, '∂P/∂u');
      arrowTo(sample.pb as Vec3, COLORS.axisY, '∂P/∂v');
    }

    const s = renderer.project(sample.p);
    if (s) {
      ctx.beginPath();
      ctx.arc(s.x, s.y, 5.5, 0, Math.PI * 2);
      ctx.fillStyle = COLORS.accent;
      ctx.fill();
      ctx.lineWidth = 2;
      ctx.strokeStyle = '#ffffff';
      ctx.stroke();
    }
  }
}

function arrow(ctx: CanvasRenderingContext2D, x0: number, y0: number, x1: number, y1: number, color: string, label: string) {
  const ang = Math.atan2(y1 - y0, x1 - x0);
  const len = Math.hypot(x1 - x0, y1 - y0);
  if (len < 4) return;
  const head = Math.min(12, len * 0.4);
  ctx.save();
  ctx.lineCap = 'round';
  ctx.lineJoin = 'round';
  for (const [stroke, width] of [[COLORS.paper, 6], [color, 2.5]] as const) {
    ctx.strokeStyle = stroke;
    ctx.lineWidth = width;
    ctx.beginPath();
    ctx.moveTo(x0, y0);
    ctx.lineTo(x1, y1);
    ctx.moveTo(x1 - head * Math.cos(ang - 0.42), y1 - head * Math.sin(ang - 0.42));
    ctx.lineTo(x1, y1);
    ctx.lineTo(x1 - head * Math.cos(ang + 0.42), y1 - head * Math.sin(ang + 0.42));
    ctx.stroke();
  }
  ctx.font = 'italic 16px "STIX Two Text", Georgia, serif';
  ctx.textAlign = Math.cos(ang) >= 0 ? 'left' : 'right';
  ctx.textBaseline = 'middle';
  const lx = x1 + Math.cos(ang) * 8;
  const ly = y1 + Math.sin(ang) * 8;
  ctx.strokeStyle = COLORS.paper;
  ctx.lineWidth = 4;
  ctx.strokeText(label, lx, ly);
  ctx.fillStyle = color;
  ctx.fillText(label, lx, ly);
  ctx.restore();
}

/** Shapes differ per kind so the markers don't rely on colour alone. */
export function marker(ctx: CanvasRenderingContext2D, kind: CriticalKind, x: number, y: number, r: number) {
  ctx.beginPath();
  switch (kind) {
    case 'maximum':
      ctx.moveTo(x, y - r);
      ctx.lineTo(x + r * 0.95, y + r * 0.7);
      ctx.lineTo(x - r * 0.95, y + r * 0.7);
      break;
    case 'minimum':
      ctx.moveTo(x, y + r);
      ctx.lineTo(x + r * 0.95, y - r * 0.7);
      ctx.lineTo(x - r * 0.95, y - r * 0.7);
      break;
    case 'saddle':
      ctx.moveTo(x, y - r);
      ctx.lineTo(x + r, y);
      ctx.lineTo(x, y + r);
      ctx.lineTo(x - r, y);
      break;
    case 'degenerate':
      ctx.arc(x, y, r * 0.75, 0, Math.PI * 2);
      break;
  }
  ctx.closePath();
  ctx.fillStyle = CRITICAL_STYLE[kind].color;
  ctx.fill();
  ctx.lineWidth = 1.5;
  ctx.strokeStyle = COLORS.ink;
  ctx.stroke();
}

/** The same marker as inline SVG, for the list in the panel. */
export function markerSVG(kind: CriticalKind): string {
  const shape = {
    maximum: '<polygon points="6,1 11,10 1,10"/>',
    minimum: '<polygon points="6,11 11,2 1,2"/>',
    saddle: '<polygon points="6,0.5 11.5,6 6,11.5 0.5,6"/>',
    degenerate: '<circle cx="6" cy="6" r="4.5"/>',
  }[kind];
  return `<svg viewBox="0 0 12 12" aria-hidden="true" fill="${CRITICAL_STYLE[kind].color}" stroke="${COLORS.ink}" stroke-width="1.2">${shape}</svg>`;
}

import type { Mode } from './math/surface';
import type { DisplayOptions } from './render/renderer';

/** Everything needed to reproduce a view. Stored in the URL so a link opens the same surface. */
export interface AppState {
  mode: Mode;
  f: string;
  px: string;
  py: string;
  pz: string;
  /** Ranges per mode, so switching modes doesn't lose them. */
  xr: [string, string];
  yr: [string, string];
  ur: [string, string];
  vr: [string, string];
  res: number;
  playing: boolean;
  speed: number;
  show: DisplayOptions;
  orthographic: boolean;
  trueScale: boolean;
  /** Camera azimuth, elevation (radians) and distance relative to the auto-fit distance. */
  camera: [number, number, number] | null;
}

export const DEFAULT_STATE: AppState = {
  mode: 'explicit',
  f: 'sin(x + t) * cos(y)',
  px: '(2 + 0.7cos(v)) * cos(u)',
  py: '(2 + 0.7cos(v)) * sin(u)',
  pz: '0.7sin(v)',
  xr: ['-6', '6'],
  yr: ['-6', '6'],
  ur: ['0', '2pi'],
  vr: ['0', '2pi'],
  res: 150,
  playing: true,
  speed: 1,
  show: { surface: true, wireframe: false, contours: true, floor: true, axes: true, grid: true, normals: false },
  orthographic: false,
  trueScale: false,
  camera: null,
};

const SHOW_KEYS: (keyof DisplayOptions)[] = ['surface', 'wireframe', 'contours', 'floor', 'axes', 'grid', 'normals'];

export function cloneState(s: AppState): AppState {
  return structuredClone(s);
}

/** Serializes the state to a query string. Only values that differ from the defaults are written. */
export function encodeState(s: AppState): string {
  const d = DEFAULT_STATE;
  const q = new URLSearchParams();
  if (s.mode === 'explicit') {
    q.set('f', s.f);
    if (!sameRange(s.xr, d.xr)) q.set('x', s.xr.join(','));
    if (!sameRange(s.yr, d.yr)) q.set('y', s.yr.join(','));
  } else {
    q.set('mode', 'parametric');
    q.set('px', s.px);
    q.set('py', s.py);
    q.set('pz', s.pz);
    if (!sameRange(s.ur, d.ur)) q.set('u', s.ur.join(','));
    if (!sameRange(s.vr, d.vr)) q.set('v', s.vr.join(','));
  }
  if (s.res !== d.res) q.set('res', String(s.res));
  if (s.playing !== d.playing) q.set('play', s.playing ? '1' : '0');
  if (s.speed !== d.speed) q.set('speed', String(s.speed));
  const shown = SHOW_KEYS.filter((k) => s.show[k]);
  const defaultShown = SHOW_KEYS.filter((k) => d.show[k]);
  if (shown.join() !== defaultShown.join()) q.set('show', shown.join(','));
  if (s.orthographic) q.set('proj', 'ortho');
  if (s.trueScale) q.set('scale', 'true');
  if (s.camera) q.set('cam', s.camera.map((v) => +v.toFixed(3)).join(','));
  return q.toString();
}

export function decodeState(query: string): AppState {
  const q = new URLSearchParams(query);
  const s = cloneState(DEFAULT_STATE);
  if (q.get('mode') === 'parametric') s.mode = 'parametric';
  const text = (key: string, fallback: string) => {
    const v = q.get(key);
    return v !== null && v.trim() !== '' ? v.slice(0, 500) : fallback;
  };
  s.f = text('f', s.f);
  s.px = text('px', s.px);
  s.py = text('py', s.py);
  s.pz = text('pz', s.pz);
  s.xr = range(q.get('x'), s.xr);
  s.yr = range(q.get('y'), s.yr);
  s.ur = range(q.get('u'), s.ur);
  s.vr = range(q.get('v'), s.vr);
  const res = Number(q.get('res'));
  if (Number.isFinite(res) && res >= 10) s.res = Math.min(400, Math.round(res));
  if (q.has('play')) s.playing = q.get('play') === '1';
  const speed = Number(q.get('speed'));
  if (q.has('speed') && Number.isFinite(speed)) s.speed = Math.min(5, Math.max(0.1, speed));
  if (q.has('show')) {
    const shown = new Set((q.get('show') ?? '').split(','));
    for (const k of SHOW_KEYS) s.show[k] = shown.has(k);
  }
  s.orthographic = q.get('proj') === 'ortho';
  s.trueScale = q.get('scale') === 'true';
  const cam = (q.get('cam') ?? '').split(',').map(Number);
  if (cam.length === 3 && cam.every(Number.isFinite) && cam[2] > 0) s.camera = cam as [number, number, number];
  return s;
}

function range(v: string | null, fallback: [string, string]): [string, string] {
  if (!v) return fallback;
  // Split on the comma that separates the two ends (ranges are constants, so they contain no commas).
  const parts = v.split(',');
  return parts.length === 2 && parts.every((p) => p.trim()) ? [parts[0].trim(), parts[1].trim()] : fallback;
}

const sameRange = (a: [string, string], b: [string, string]) => a[0] === b[0] && a[1] === b[1];

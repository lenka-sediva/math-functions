import './style.css';
import { size, type Node } from './math/ast';
import { findCriticalPoints, type CriticalPoint } from './math/critical';
import { formatNumber, toHTML, toText } from './math/codegen';
import { buildSurface, measure, newSample, PARAMS, type FieldError, type InputField, type Mode, type Surface, type SurfaceInput } from './math/surface';
import { exportSTL } from './export/stl';
import { CRITICAL_STYLE, drawOverlay, markerSVG } from './overlay';
import { PRESETS } from './presets';
import { ShaderError } from './render/gl';
import { Renderer } from './render/renderer';
import { decodeState, encodeState, type AppState } from './state';

const $ = <T extends HTMLElement = HTMLElement>(id: string) => document.getElementById(id) as T;
const $$ = <T extends HTMLElement = HTMLElement>(sel: string) => Array.from(document.querySelectorAll<T>(sel));

const glCanvas = $<HTMLCanvasElement>('gl');
const overlayCanvas = $<HTMLCanvasElement>('overlay');
const overlay = overlayCanvas.getContext('2d')!;
const stage = document.querySelector<HTMLElement>('.stage')!;

const inputs = {
  f: $<HTMLInputElement>('f'),
  px: $<HTMLInputElement>('px'),
  py: $<HTMLInputElement>('py'),
  pz: $<HTMLInputElement>('pz'),
  a0: $<HTMLInputElement>('a0'),
  a1: $<HTMLInputElement>('a1'),
  b0: $<HTMLInputElement>('b0'),
  b1: $<HTMLInputElement>('b1'),
} satisfies Record<InputField, HTMLInputElement>;

const reducedMotion = window.matchMedia('(prefers-reduced-motion: reduce)').matches;
const state: AppState = decodeState(location.search);
if (reducedMotion && !new URLSearchParams(location.search).has('play')) state.playing = false;

let renderer: Renderer;
try {
  renderer = new Renderer(glCanvas);
} catch {
  $('fallback').hidden = false;
  throw new Error('WebGL2 not available');
}

let surface: Surface | null = null;
let t = 0;
let dirty = true;
let critical: CriticalPoint[] = [];
let criticalTime = -1;
/** Camera distance right after the last auto-fit; stored camera distances are relative to it. */
let fitDistance = 1;

// ---------------------------------------------------------------------------
// State <-> form

function rangesFor(mode: Mode): [[string, string], [string, string]] {
  return mode === 'explicit' ? [state.xr, state.yr] : [state.ur, state.vr];
}

function writeForm() {
  document.body.dataset.mode = state.mode;
  for (const b of $$<HTMLButtonElement>('.mode-switch button')) b.setAttribute('aria-checked', String(b.dataset.mode === state.mode));
  inputs.f.value = state.f;
  inputs.px.value = state.px;
  inputs.py.value = state.py;
  inputs.pz.value = state.pz;
  const [ar, br] = rangesFor(state.mode);
  [inputs.a0.value, inputs.a1.value] = ar;
  [inputs.b0.value, inputs.b1.value] = br;
  const [pa, pb] = PARAMS[state.mode];
  $('a-name').textContent = pa;
  $('b-name').textContent = pb;

  $<HTMLInputElement>('res').value = String(state.res);
  $('res-out').textContent = `${state.res} × ${state.res}`;
  $<HTMLInputElement>('speed').value = String(state.speed);
  $('speed-out').textContent = `${state.speed.toFixed(1)}×`;
  for (const box of $$<HTMLInputElement>('[data-show]')) box.checked = state.show[box.dataset.show as keyof AppState['show']];
  $<HTMLInputElement>('true-scale').checked = state.trueScale;
  $('proj').textContent = state.orthographic ? 'Orthographic' : 'Perspective';
}

function readForm() {
  state.f = inputs.f.value;
  state.px = inputs.px.value;
  state.py = inputs.py.value;
  state.pz = inputs.pz.value;
  const ar: [string, string] = [inputs.a0.value, inputs.a1.value];
  const br: [string, string] = [inputs.b0.value, inputs.b1.value];
  if (state.mode === 'explicit') {
    state.xr = ar;
    state.yr = br;
  } else {
    state.ur = ar;
    state.vr = br;
  }
}

function surfaceInput(): SurfaceInput {
  const [aRange, bRange] = rangesFor(state.mode);
  return { mode: state.mode, f: state.f, px: state.px, py: state.py, pz: state.pz, aRange, bRange };
}

// ---------------------------------------------------------------------------
// Building the surface

type Refit = 'reset' | 'keep-angles' | 'none';

function rebuild(refit: Refit) {
  for (const el of Object.values(inputs)) el.removeAttribute('aria-invalid');
  const result = buildSurface(surfaceInput());
  if (result.errors.length) {
    showErrors(result.errors);
    return;
  }
  const next = result.surface!;
  const frame = measure(next);
  if (!frame.hasValues) {
    showMessage('The formula has no real values anywhere in this range. Try a different range.');
    return;
  }
  try {
    renderer.trueScale = state.trueScale;
    renderer.setSurface(next, frame);
  } catch (e) {
    console.error(e instanceof ShaderError ? `${e.message}\n${e.source}` : e);
    showMessage('Your graphics card could not compile this formula. Try a shorter one.');
    return;
  }
  showMessage('');
  surface = next;
  if (refit !== 'none') renderer.inspected = null;
  if (refit === 'reset') {
    renderer.resetCamera();
    fitDistance = renderer.camera.distance;
  } else if (refit === 'keep-angles') {
    const { azimuth, elevation } = renderer.camera;
    renderer.resetCamera();
    fitDistance = renderer.camera.distance;
    renderer.camera.azimuth = azimuth;
    renderer.camera.elevation = elevation;
  }
  renderer.camera.orthographic = state.orthographic;
  criticalTime = -1;
  updateCaption();
  updateDerivatives();
  updateAnimationUI();
  updateScaleHint();
  updateInspector();
  updateCritical(true);
  invalidate();
}

function showErrors(errors: FieldError[]) {
  const e = errors[0];
  const input = inputs[e.field];
  for (const err of errors) inputs[err.field].setAttribute('aria-invalid', 'true');
  const near = e.length > 0 ? input.value.slice(e.pos, e.pos + e.length) : '';
  const box = $('formula-error');
  box.textContent = e.message;
  if (near && !e.message.includes(`"${near}"`)) {
    box.append(' near ');
    const code = document.createElement('code');
    code.textContent = near;
    box.append(code);
  }
}

function showMessage(text: string) {
  $('formula-error').textContent = text;
}

// ---------------------------------------------------------------------------
// Panels

const truncate = (html: string, node: Node) => (size(node) > 90 ? `<span class="hint">a long expression with ${size(node)} terms</span>` : html);

function updateCaption() {
  const cap = $('caption');
  if (!surface) return;
  const long = (surface.mode === 'explicit' ? toText(surface.P[2]) : surface.P.map(toText).join('')).length > 48;
  if (surface.mode === 'explicit') {
    cap.className = long ? 'caption long' : 'caption';
    cap.innerHTML = `<span class="line"><span class="lhs">z</span> = ${toHTML(surface.P[2])}</span>`;
  } else {
    cap.className = long ? 'caption parametric long' : 'caption parametric';
    cap.innerHTML = (['x', 'y', 'z'] as const)
      .map((n, i) => `<span class="line"><span class="lhs">${n}</span> = ${toHTML(surface!.P[i])}</span>`)
      .join('');
  }
}

function updateDerivatives() {
  const dl = $('derivs');
  if (!surface) return;
  if (surface.mode === 'explicit') {
    dl.innerHTML =
      `<dl class="derivs"><dt>∂f/∂x =</dt><dd>${truncate(toHTML(surface.Pa[2]), surface.Pa[2])}</dd>` +
      `<dt>∂f/∂y =</dt><dd>${truncate(toHTML(surface.Pb[2]), surface.Pb[2])}</dd></dl>`;
  } else {
    // Parametric derivatives are long, so they start folded away.
    const vec = (c: Node[]) => `(${c.map((n) => truncate(toHTML(n), n)).join(', ')})`;
    dl.innerHTML = `<details class="derivs-fold"><summary>Partial derivatives used for the normals</summary>
      <dl class="derivs"><dt>∂P/∂u =</dt><dd>${vec(surface.Pa)}</dd><dt>∂P/∂v =</dt><dd>${vec(surface.Pb)}</dd></dl></details>`;
  }
}

function updateAnimationUI() {
  const play = $<HTMLButtonElement>('play');
  const uses = !!surface?.usesTime;
  play.disabled = !uses;
  play.textContent = state.playing && uses ? 'Pause' : 'Play';
  $<HTMLInputElement>('speed').disabled = !uses;
  $('t-out').textContent = uses ? `t = ${t.toFixed(2)}` : '';
  $('anim-hint').innerHTML = uses
    ? ''
    : `Add <i>t</i> to the formula to animate it, for example <code>${state.mode === 'explicit' ? 'sin(x + t)' : 'cos(u + t)'}</code>.`;
}

function updateScaleHint() {
  const z = renderer.zScale;
  $('scale-hint').textContent =
    surface?.mode === 'explicit' && !state.trueScale && z < 0.999 ? `Height is shown at ×${formatNumber(Number(z.toPrecision(2)))} so the surface fits.` : '';
}

const fmt = (v: number) => (Number.isFinite(v) ? (Math.abs(v) < 1e-12 ? '0' : formatNumber(Number(v.toPrecision(4)))) : 'undefined');

function updateInspector() {
  const box = $('inspector');
  const ip = renderer.inspected;
  if (!surface || !ip) {
    box.innerHTML = 'Click the surface to see the tangent plane and derivatives at that point.';
    return;
  }
  const s = newSample();
  surface.evaluate(ip.a, ip.b, t, s);
  let rows: [string, string][];
  if (surface.mode === 'explicit') {
    const fx = s.pa[2];
    const fy = s.pb[2];
    rows = [
      ['<i>x</i>, <i>y</i>', `${fmt(s.p[0])}, ${fmt(s.p[1])}`],
      ['<i>z</i>', fmt(s.p[2])],
      ['∂f/∂x', fmt(fx)],
      ['∂f/∂y', fmt(fy)],
      ['|∇f|', fmt(Math.hypot(fx, fy))],
    ];
  } else {
    const n = renderer.worldNormal(s);
    rows = [
      ['<i>u</i>, <i>v</i>', `${fmt(ip.a)}, ${fmt(ip.b)}`],
      ['point', s.p.map(fmt).join(', ')],
      ['normal', n ? n.map(fmt).join(', ') : 'undefined'],
    ];
  }
  box.innerHTML = `Inspected point<table>${rows.map(([k, v]) => `<tr><td>${k}</td><td>${v}</td></tr>`).join('')}</table>
    <button type="button" class="button" id="clear-point">Clear point</button>`;
  $('clear-point').addEventListener('click', clearInspected);
}

function clearInspected() {
  renderer.inspected = null;
  updateInspector();
  invalidate();
}

function updateCritical(force = false) {
  const list = $('critical-list');
  const enabled = $<HTMLInputElement>('critical').checked && surface?.mode === 'explicit';
  if (!enabled || !surface) {
    critical = [];
    list.innerHTML = '';
    return;
  }
  if (!force && Math.abs(t - criticalTime) < 1e-9) return;
  criticalTime = t;
  critical = findCriticalPoints(surface, t).filter((c) => renderer.isVisible([c.x, c.y, c.z]));
  if (!critical.length) {
    list.innerHTML = '<p class="hint">No points where the gradient is zero in this range.</p>';
    return;
  }
  const shown = critical.slice(0, 8);
  list.innerHTML =
    `<ul>${shown
      .map((c) => `<li>${markerSVG(c.kind)}<span>${CRITICAL_STYLE[c.kind].label}</span><span class="coords">(${fmt(c.x)}, ${fmt(c.y)}, ${fmt(c.z)})</span></li>`)
      .join('')}</ul>` + (critical.length > shown.length ? `<p class="hint">and ${critical.length - shown.length} more</p>` : '');
}

// ---------------------------------------------------------------------------
// Rendering loop

function invalidate() {
  dirty = true;
}

let lastFrame = performance.now();
let lastPanelUpdate = 0;

function tick(now: number) {
  const dt = Math.min(0.1, (now - lastFrame) / 1000);
  lastFrame = now;
  const animating = state.playing && !!surface?.usesTime;
  if (animating) {
    t += dt * state.speed;
    dirty = true;
  }
  if (dirty) {
    dirty = false;
    drawFrame();
    if (animating && now - lastPanelUpdate > 120) {
      lastPanelUpdate = now;
      $('t-out').textContent = `t = ${t.toFixed(2)}`;
      if (renderer.inspected) updateInspector();
    }
  }
  requestAnimationFrame(tick);
}

let lastCriticalUpdate = 0;
function drawFrame() {
  renderer.render(t);
  if (state.playing && surface?.usesTime && $<HTMLInputElement>('critical').checked && performance.now() - lastCriticalUpdate > 300) {
    lastCriticalUpdate = performance.now();
    updateCritical();
  }
  drawOverlay(overlay, { renderer, t, showAxes: state.show.axes, critical });
}

function resize() {
  const dpr = Math.min(window.devicePixelRatio || 1, 2);
  const w = Math.max(1, Math.round(stage.clientWidth * dpr));
  const h = Math.max(1, Math.round(stage.clientHeight * dpr));
  for (const c of [glCanvas, overlayCanvas]) {
    if (c.width !== w || c.height !== h) {
      c.width = w;
      c.height = h;
    }
  }
  invalidate();
}

// ---------------------------------------------------------------------------
// URL

let urlTimer = 0;
function scheduleUrlUpdate() {
  clearTimeout(urlTimer);
  urlTimer = window.setTimeout(updateUrl, 400);
}

function updateUrl() {
  const c = renderer.camera;
  state.camera = [c.azimuth, c.elevation, c.distance / fitDistance];
  history.replaceState(null, '', `${location.pathname}?${encodeState(state)}`);
}

// ---------------------------------------------------------------------------
// Toast

let toastTimer = 0;
function toast(text: string) {
  const el = $('toast');
  el.textContent = text;
  el.classList.add('show');
  clearTimeout(toastTimer);
  toastTimer = window.setTimeout(() => el.classList.remove('show'), 2600);
}

// ---------------------------------------------------------------------------
// Events: form

let inputTimer = 0;
for (const el of Object.values(inputs)) {
  el.addEventListener('input', () => {
    clearTimeout(inputTimer);
    inputTimer = window.setTimeout(() => {
      readForm();
      rebuild('keep-angles');
      scheduleUrlUpdate();
    }, 220);
  });
}

for (const b of $$<HTMLButtonElement>('.mode-switch button')) {
  b.addEventListener('click', () => {
    const mode = b.dataset.mode as Mode;
    if (mode === state.mode) return;
    readForm();
    state.mode = mode;
    writeForm();
    rebuild('reset');
    scheduleUrlUpdate();
  });
}

const presetSelect = $<HTMLSelectElement>('preset');
presetSelect.innerHTML =
  '<option value="">Choose an example</option>' +
  (['explicit', 'parametric'] as const)
    .map(
      (mode) =>
        `<optgroup label="${mode === 'explicit' ? 'z = f(x, y)' : 'Parametric'}">${PRESETS.map((p, i) => (p.mode === mode ? `<option value="${i}">${p.name}</option>` : '')).join('')}</optgroup>`,
    )
    .join('');
presetSelect.addEventListener('change', () => {
  const p = PRESETS[Number(presetSelect.value)];
  presetSelect.value = '';
  if (!p) return;
  state.mode = p.mode;
  if (p.mode === 'explicit') {
    state.f = p.f!;
    state.xr = [...p.a];
    state.yr = [...p.b];
  } else {
    state.px = p.px!;
    state.py = p.py!;
    state.pz = p.pz!;
    state.ur = [...p.a];
    state.vr = [...p.b];
  }
  writeForm();
  rebuild('reset');
  scheduleUrlUpdate();
});

$<HTMLInputElement>('res').addEventListener('input', (e) => {
  state.res = Number((e.target as HTMLInputElement).value);
  $('res-out').textContent = `${state.res} × ${state.res}`;
  renderer.setResolution(state.res);
  invalidate();
  scheduleUrlUpdate();
});

$<HTMLInputElement>('speed').addEventListener('input', (e) => {
  state.speed = Number((e.target as HTMLInputElement).value);
  $('speed-out').textContent = `${state.speed.toFixed(1)}×`;
  scheduleUrlUpdate();
});

function togglePlay() {
  if (!surface?.usesTime) return;
  state.playing = !state.playing;
  updateAnimationUI();
  if (!state.playing) updateCritical(true);
  scheduleUrlUpdate();
}
$('play').addEventListener('click', togglePlay);

function setShow(key: keyof AppState['show'], value: boolean) {
  state.show[key] = value;
  renderer.options = { ...state.show };
  if (key === 'axes' || key === 'grid') renderer.refreshGuides();
  const box = document.querySelector<HTMLInputElement>(`[data-show="${key}"]`);
  if (box) box.checked = value;
  invalidate();
  scheduleUrlUpdate();
}
for (const box of $$<HTMLInputElement>('[data-show]')) {
  box.addEventListener('change', () => setShow(box.dataset.show as keyof AppState['show'], box.checked));
}

$<HTMLInputElement>('true-scale').addEventListener('change', (e) => {
  state.trueScale = (e.target as HTMLInputElement).checked;
  renderer.trueScale = state.trueScale;
  renderer.updateTransform();
  const { azimuth, elevation } = renderer.camera;
  renderer.resetCamera();
  fitDistance = renderer.camera.distance;
  renderer.camera.azimuth = azimuth;
  renderer.camera.elevation = elevation;
  updateScaleHint();
  updateCritical(true);
  invalidate();
  scheduleUrlUpdate();
});

$<HTMLInputElement>('critical').addEventListener('change', () => {
  updateCritical(true);
  invalidate();
});

// ---------------------------------------------------------------------------
// Events: view

function setProjection(ortho: boolean) {
  state.orthographic = ortho;
  renderer.camera.orthographic = ortho;
  $('proj').textContent = ortho ? 'Orthographic' : 'Perspective';
  invalidate();
  scheduleUrlUpdate();
}
$('proj').addEventListener('click', () => setProjection(!state.orthographic));

function viewAlong(axis: 'x' | 'y' | 'z') {
  const c = renderer.camera;
  if (axis === 'x') [c.azimuth, c.elevation] = [0, 0];
  if (axis === 'y') [c.azimuth, c.elevation] = [Math.PI / 2, 0];
  if (axis === 'z') [c.azimuth, c.elevation] = [-Math.PI / 2, Math.PI / 2 - 0.001];
  invalidate();
  scheduleUrlUpdate();
}
for (const b of $$<HTMLButtonElement>('[data-view]')) b.addEventListener('click', () => viewAlong(b.dataset.view as 'x' | 'y' | 'z'));

function resetView() {
  renderer.resetCamera();
  fitDistance = renderer.camera.distance;
  invalidate();
  scheduleUrlUpdate();
}
$('reset-view').addEventListener('click', resetView);

// Pointer: drag to orbit, right/shift drag to pan, pinch to zoom, click to inspect.
const pointers = new Map<number, { x: number; y: number }>();
let dragMoved = 0;
let panning = false;
let pinchDistance = 0;

glCanvas.addEventListener('contextmenu', (e) => e.preventDefault());

glCanvas.addEventListener('pointerdown', (e) => {
  glCanvas.setPointerCapture(e.pointerId);
  pointers.set(e.pointerId, { x: e.clientX, y: e.clientY });
  dragMoved = 0;
  panning = e.button === 2 || e.shiftKey;
  if (pointers.size === 2) pinchDistance = pinchSpan();
  glCanvas.classList.add('dragging');
});

glCanvas.addEventListener('pointermove', (e) => {
  const prev = pointers.get(e.pointerId);
  if (!prev) return;
  const dx = e.clientX - prev.x;
  const dy = e.clientY - prev.y;
  const h = glCanvas.clientHeight || 1;
  if (pointers.size === 2) {
    const before = pinchMid();
    pointers.set(e.pointerId, { x: e.clientX, y: e.clientY });
    const span = pinchSpan();
    if (pinchDistance > 0 && span > 0) renderer.camera.zoom(pinchDistance / span);
    pinchDistance = span;
    const after = pinchMid();
    renderer.camera.pan((after.x - before.x) / h, (after.y - before.y) / h);
  } else {
    pointers.set(e.pointerId, { x: e.clientX, y: e.clientY });
    if (panning) renderer.camera.pan(dx / h, dy / h);
    else renderer.camera.orbit(-dx * 0.008, dy * 0.008);
  }
  dragMoved += Math.abs(dx) + Math.abs(dy);
  invalidate();
});

function endPointer(e: PointerEvent) {
  const wasSingle = pointers.size === 1;
  pointers.delete(e.pointerId);
  if (pointers.size === 0) glCanvas.classList.remove('dragging');
  if (wasSingle && dragMoved < 5 && e.type === 'pointerup' && e.button === 0 && !e.shiftKey) inspectAt(e);
  else if (dragMoved > 0) scheduleUrlUpdate();
}
glCanvas.addEventListener('pointerup', endPointer);
glCanvas.addEventListener('pointercancel', endPointer);

function pinchSpan() {
  const [a, b] = [...pointers.values()];
  return a && b ? Math.hypot(a.x - b.x, a.y - b.y) : 0;
}
function pinchMid() {
  const [a, b] = [...pointers.values()];
  return a && b ? { x: (a.x + b.x) / 2, y: (a.y + b.y) / 2 } : { x: 0, y: 0 };
}

glCanvas.addEventListener(
  'wheel',
  (e) => {
    e.preventDefault();
    const delta = e.deltaMode === 1 ? e.deltaY * 30 : e.deltaY;
    renderer.camera.zoom(Math.exp(delta * 0.0015));
    invalidate();
    scheduleUrlUpdate();
  },
  { passive: false },
);

function inspectAt(e: PointerEvent) {
  const rect = glCanvas.getBoundingClientRect();
  const nx = ((e.clientX - rect.left) / rect.width) * 2 - 1;
  const ny = 1 - ((e.clientY - rect.top) / rect.height) * 2;
  const hit = renderer.pick(nx, ny, t);
  if (!hit) return;
  renderer.inspected = hit;
  updateInspector();
  invalidate();
}

window.addEventListener('keydown', (e) => {
  const target = e.target as HTMLElement;
  if (e.ctrlKey || e.metaKey || e.altKey || target.matches('input:not([type=checkbox]):not([type=range]), select, textarea')) return;
  const key = e.key.toLowerCase();
  const toggles: Record<string, keyof AppState['show']> = { m: 'wireframe', c: 'contours', n: 'normals', o: 'axes', k: 'grid' };
  if (key in toggles) setShow(toggles[key], !state.show[toggles[key]]);
  else if (key === 'x' || key === 'y' || key === 'z') viewAlong(key);
  else if (key === 'r') resetView();
  else if (key === 'p') setProjection(!state.orthographic);
  else if (key === ' ' && target.tagName !== 'BUTTON') {
    e.preventDefault();
    togglePlay();
  } else if (key === 'escape') clearInspected();
  else return;
});

// ---------------------------------------------------------------------------
// Export and share

$('copy-link').addEventListener('click', async () => {
  clearTimeout(urlTimer);
  updateUrl();
  try {
    await navigator.clipboard.writeText(location.href);
    toast('Link copied');
  } catch {
    toast('Copy the link from the address bar');
  }
});

function download(blob: Blob, name: string) {
  const url = URL.createObjectURL(blob);
  const a = document.createElement('a');
  a.href = url;
  a.download = name;
  a.click();
  setTimeout(() => URL.revokeObjectURL(url), 1000);
}

$('save-png').addEventListener('click', () => {
  // Render and copy in the same task, so the WebGL drawing buffer is still intact.
  renderer.render(t);
  drawOverlay(overlay, { renderer, t, showAxes: state.show.axes, critical });
  const out = document.createElement('canvas');
  out.width = glCanvas.width;
  out.height = glCanvas.height;
  const ctx = out.getContext('2d')!;
  ctx.drawImage(glCanvas, 0, 0);
  ctx.drawImage(overlayCanvas, 0, 0);
  if (surface) {
    const dpr = out.width / Math.max(1, glCanvas.clientWidth);
    const lines =
      surface.mode === 'explicit' ? [`z = ${toText(surface.P[2])}`] : (['x', 'y', 'z'] as const).map((n, i) => `${n} = ${toText(surface!.P[i])}`);
    const fontSize = (surface.mode === 'explicit' ? 30 : 22) * dpr;
    ctx.font = `${fontSize}px "STIX Two Text", Georgia, serif`;
    ctx.fillStyle = '#1d2428';
    ctx.textBaseline = 'alphabetic';
    lines.forEach((line, i) => ctx.fillText(line, 36 * dpr, out.height - 30 * dpr - (lines.length - 1 - i) * fontSize * 1.25));
  }
  out.toBlob((blob) => {
    if (blob) {
      download(blob, 'math-functions-3d.png');
      toast('Image saved');
    }
  }, 'image/png');
});

$('save-stl').addEventListener('click', () => {
  if (!surface) return;
  const solid = $<HTMLInputElement>('stl-solid').checked;
  const result = exportSTL(
    surface,
    { resolution: Math.min(state.res, 300), t, scale: renderer.scale, solid, sizeMm: 100 },
    (a, b, time, s, n) => renderer.isVisibleAt(a, b, time, s, n),
  );
  if (result.triangles === 0) {
    toast('Nothing to export: the surface has no visible points');
    return;
  }
  download(new Blob([result.data], { type: 'model/stl' }), 'surface.stl');
  toast(
    result.solidSkipped
      ? 'Downloaded as an open surface, because some points are undefined'
      : `Downloaded surface.stl with ${result.triangles.toLocaleString('en')} triangles`,
  );
});

// ---------------------------------------------------------------------------
// Start

writeForm();
renderer.options = { ...state.show };
renderer.setResolution(state.res);
new ResizeObserver(resize).observe(stage);
resize();
rebuild('reset');
if (state.camera && surface) {
  const [az, el, rel] = state.camera;
  renderer.camera.azimuth = az;
  renderer.camera.elevation = el;
  renderer.camera.distance = rel * fitDistance;
}
// Fonts arrive after the first frame; redraw so canvas labels use them.
document.fonts?.ready.then(invalidate);
requestAnimationFrame(tick);

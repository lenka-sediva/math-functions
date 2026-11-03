import { newSample, poleNearby, type Frame, type Surface, type SurfaceSample, type Vec3 } from '../math/surface';
import { OrbitCamera } from './camera';
import { createProgram, hexToRgb, type Program } from './gl';
import { v3, type V3 } from './mat4';
import { LINE_FRAGMENT, LINE_VERTEX, SURFACE_FRAGMENT, surfaceVertexShader } from './shaders';

export interface DisplayOptions {
  surface: boolean;
  wireframe: boolean;
  contours: boolean;
  floor: boolean;
  axes: boolean;
  grid: boolean;
  normals: boolean;
}

export const COLORS = {
  paper: '#eef0ea',
  ink: '#1d2428',
  accent: '#2846c8',
  axisX: '#c8553d',
  axisY: '#3a8f5c',
  axisZ: '#2f5fb3',
};

type RGBA = [number, number, number, number];
const rgba = (hex: string, a = 1): RGBA => [...hexToRgb(hex), a];

/** A growable list of line or triangle vertices: x y z r g b a. */
class VertexList {
  data: number[] = [];
  add(p: V3, c: RGBA) {
    this.data.push(p[0], p[1], p[2], c[0], c[1], c[2], c[3]);
  }
  get count() {
    return this.data.length / 7;
  }
}

interface DynamicMesh {
  vao: WebGLVertexArrayObject;
  buffer: WebGLBuffer;
  count: number;
}

export class Renderer {
  readonly gl: WebGL2RenderingContext;
  readonly camera = new OrbitCamera();

  private surfaceProgram: Program | null = null;
  private readonly lineProgram: Program;

  private gridVao: WebGLVertexArrayObject;
  private gridBuffer: WebGLBuffer;
  private triIndex: WebGLBuffer;
  private wireIndex: WebGLBuffer;
  private triCount = 0;
  private wireCount = 0;
  resolution = 0;

  private guides: DynamicMesh; // axes + floor grid
  private normalLines: DynamicMesh;
  private planeTris: DynamicMesh;
  private planeLines: DynamicMesh;

  surface: Surface | null = null;
  frame: Frame | null = null;
  options: DisplayOptions = { surface: true, wireframe: false, contours: true, floor: true, axes: true, grid: true, normals: false };
  trueScale = false;

  /** Display transform: world = (p - center) * scale. */
  center: Vec3 = [0, 0, 0];
  scale: Vec3 = [1, 1, 1];
  radius = 1;
  floorZ = 0;
  contourStep = 1;
  /** Explicit mode: largest height change across one grid cell that still counts as continuous. */
  maxJump = 0;

  /** Point to highlight with a tangent plane, as surface parameters. */
  inspected: { a: number; b: number } | null = null;

  constructor(readonly canvas: HTMLCanvasElement) {
    const gl = canvas.getContext('webgl2', { antialias: true, alpha: false });
    if (!gl) throw new Error('WebGL2 is not available in this browser');
    this.gl = gl;

    this.lineProgram = createProgram(gl, LINE_VERTEX, LINE_FRAGMENT);

    this.gridVao = gl.createVertexArray()!;
    this.gridBuffer = gl.createBuffer()!;
    this.triIndex = gl.createBuffer()!;
    this.wireIndex = gl.createBuffer()!;

    this.guides = this.createDynamic();
    this.normalLines = this.createDynamic();
    this.planeTris = this.createDynamic();
    this.planeLines = this.createDynamic();
  }

  // ---------------------------------------------------------------------------
  // Setup

  /** Compiles the shader for a new formula. Throws ShaderError if the GPU rejects it. */
  setSurface(surface: Surface, frame: Frame) {
    const program = createProgram(this.gl, surfaceVertexShader(surface.glsl), SURFACE_FRAGMENT);
    if (this.surfaceProgram) this.gl.deleteProgram(this.surfaceProgram.program);
    this.surfaceProgram = program;
    this.surface = surface;
    this.frame = frame;
    this.bindGridAttribute();
    this.updateTransform();
  }

  /** Recomputes centre, scale and guides from the current frame (e.g. after toggling true scale). */
  updateTransform() {
    const frame = this.frame;
    const surface = this.surface;
    if (!frame || !surface) return;
    const span = v3.sub(frame.boxMax, frame.boxMin);
    this.center = v3.scale(v3.add(frame.boxMin, frame.boxMax), 0.5);

    let zScale = 1;
    if (surface.mode === 'explicit' && !this.trueScale) {
      // Keep tall surfaces (like x² over a wide range) at a readable height.
      const xy = Math.max(span[0], span[1]);
      if (span[2] > 0.8 * xy) zScale = (0.8 * xy) / span[2];
    }
    this.scale = [1, 1, zScale];
    const worldSpan = v3.mul(span, this.scale);
    this.radius = Math.max(v3.len(worldSpan) / 2, 1e-6);
    this.floorZ = (frame.boxMin[2] - this.center[2]) * zScale - 0.18 * this.radius;
    this.contourStep = niceStep(span[2] / 9);
    this.maxJump = surface.mode === 'explicit' ? 0.5 * span[2] : 0;
    this.buildGuides();
  }

  get zScale() {
    return this.scale[2];
  }

  setResolution(n: number) {
    if (n === this.resolution) return;
    this.resolution = n;
    const gl = this.gl;
    const verts = new Float32Array((n + 1) * (n + 1) * 2);
    let k = 0;
    for (let j = 0; j <= n; j++) {
      for (let i = 0; i <= n; i++) {
        verts[k++] = i / n;
        verts[k++] = j / n;
      }
    }
    const idx = (i: number, j: number) => j * (n + 1) + i;
    const tris = new Uint32Array(n * n * 6);
    k = 0;
    for (let j = 0; j < n; j++) {
      for (let i = 0; i < n; i++) {
        tris.set([idx(i, j), idx(i + 1, j), idx(i + 1, j + 1), idx(i, j), idx(i + 1, j + 1), idx(i, j + 1)], k);
        k += 6;
      }
    }
    // Dense meshes would turn the wireframe into a solid fill, so draw at most ~100 lines per direction.
    const every = Math.max(1, Math.ceil(n / 100));
    const rows: number[] = [];
    for (let k = 0; k < n; k += every) rows.push(k);
    rows.push(n);
    const wire: number[] = [];
    for (const j of rows) for (let i = 0; i < n; i++) wire.push(idx(i, j), idx(i + 1, j));
    for (const i of rows) for (let j = 0; j < n; j++) wire.push(idx(i, j), idx(i, j + 1));

    gl.bindBuffer(gl.ARRAY_BUFFER, this.gridBuffer);
    gl.bufferData(gl.ARRAY_BUFFER, verts, gl.STATIC_DRAW);
    gl.bindBuffer(gl.ELEMENT_ARRAY_BUFFER, this.triIndex);
    gl.bufferData(gl.ELEMENT_ARRAY_BUFFER, tris, gl.STATIC_DRAW);
    gl.bindBuffer(gl.ELEMENT_ARRAY_BUFFER, this.wireIndex);
    gl.bufferData(gl.ELEMENT_ARRAY_BUFFER, new Uint32Array(wire), gl.STATIC_DRAW);
    this.triCount = tris.length;
    this.wireCount = wire.length;
    this.bindGridAttribute();
  }

  private bindGridAttribute() {
    const gl = this.gl;
    const loc = this.surfaceProgram?.attribs.aST;
    if (loc === undefined || loc < 0) return;
    gl.bindVertexArray(this.gridVao);
    gl.bindBuffer(gl.ARRAY_BUFFER, this.gridBuffer);
    gl.enableVertexAttribArray(loc);
    gl.vertexAttribPointer(loc, 2, gl.FLOAT, false, 0, 0);
    gl.bindVertexArray(null);
  }

  private createDynamic(): DynamicMesh {
    const gl = this.gl;
    const vao = gl.createVertexArray()!;
    const buffer = gl.createBuffer()!;
    gl.bindVertexArray(vao);
    gl.bindBuffer(gl.ARRAY_BUFFER, buffer);
    const { aPos, aColor } = this.lineProgram.attribs;
    gl.enableVertexAttribArray(aPos);
    gl.vertexAttribPointer(aPos, 3, gl.FLOAT, false, 28, 0);
    gl.enableVertexAttribArray(aColor);
    gl.vertexAttribPointer(aColor, 4, gl.FLOAT, false, 28, 12);
    gl.bindVertexArray(null);
    return { vao, buffer, count: 0 };
  }

  private upload(mesh: DynamicMesh, list: VertexList) {
    const gl = this.gl;
    gl.bindBuffer(gl.ARRAY_BUFFER, mesh.buffer);
    gl.bufferData(gl.ARRAY_BUFFER, new Float32Array(list.data), gl.DYNAMIC_DRAW);
    mesh.count = list.count;
  }

  // ---------------------------------------------------------------------------
  // Coordinates

  toWorld(p: Vec3): V3 {
    return v3.mul(v3.sub(p, this.center), this.scale);
  }

  fromWorld(w: V3): Vec3 {
    return [w[0] / this.scale[0] + this.center[0], w[1] / this.scale[1] + this.center[1], w[2] / this.scale[2] + this.center[2]];
  }

  get aspect() {
    return this.canvas.width / Math.max(1, this.canvas.height);
  }

  /** Projects a math-space point to CSS pixels on the canvas; null if behind the camera. */
  project(p: Vec3): { x: number; y: number } | null {
    const m = this.camera.viewProjection(this.aspect);
    const w = this.toWorld(p);
    const cx = m[0] * w[0] + m[4] * w[1] + m[8] * w[2] + m[12];
    const cy = m[1] * w[0] + m[5] * w[1] + m[9] * w[2] + m[13];
    const cw = m[3] * w[0] + m[7] * w[1] + m[11] * w[2] + m[15];
    if (cw <= 0) return null;
    const rect = this.canvas.getBoundingClientRect();
    return { x: ((cx / cw + 1) / 2) * rect.width, y: ((1 - cy / cw) / 2) * rect.height };
  }

  resetCamera() {
    this.camera.azimuth = -Math.PI * 0.3;
    this.camera.elevation = 0.5;
    // Centre on the surface plus the floor below it.
    const top = this.frame ? (this.frame.boxMax[2] - this.center[2]) * this.scale[2] : 0;
    this.camera.fit([0, 0, (this.floorZ + top) / 2], this.radius * 1.12);
  }

  // ---------------------------------------------------------------------------
  // Guides: axes and floor grid

  private buildGuides() {
    const frame = this.frame!;
    const list = new VertexList();
    const { boxMin: mn, boxMax: mx } = frame;
    const floorMathZ = this.fromWorld([0, 0, this.floorZ])[2];

    if (this.options.grid) {
      const ink = rgba(COLORS.ink, 0.13);
      const step = niceStep(Math.max(mx[0] - mn[0], mx[1] - mn[1]) / 10);
      for (let x = Math.ceil(mn[0] / step) * step; x <= mx[0] + 1e-9; x += step) {
        list.add([x, mn[1], floorMathZ], ink);
        list.add([x, mx[1], floorMathZ], ink);
      }
      for (let y = Math.ceil(mn[1] / step) * step; y <= mx[1] + 1e-9; y += step) {
        list.add([mn[0], y, floorMathZ], ink);
        list.add([mx[0], y, floorMathZ], ink);
      }
    }

    if (this.options.axes) {
      const o = this.axisOrigin();
      const pad = v3.scale(v3.sub(mx, mn), 0.08);
      const ends = this.axisEnds();
      list.add([mn[0] - pad[0], o[1], o[2]], rgba(COLORS.axisX));
      list.add(ends.x, rgba(COLORS.axisX));
      list.add([o[0], mn[1] - pad[1], o[2]], rgba(COLORS.axisY));
      list.add(ends.y, rgba(COLORS.axisY));
      list.add([o[0], o[1], mn[2] - pad[2]], rgba(COLORS.axisZ));
      list.add(ends.z, rgba(COLORS.axisZ));
    }
    this.upload(this.guides, list);
  }

  /** Axes go through the origin, or the nearest point of the box if the origin is outside. */
  axisOrigin(): Vec3 {
    const { boxMin: mn, boxMax: mx } = this.frame!;
    return [0, 1, 2].map((k) => Math.min(mx[k], Math.max(mn[k], 0))) as Vec3;
  }

  axisEnds() {
    const { boxMin: mn, boxMax: mx } = this.frame!;
    const o = this.axisOrigin();
    const pad = v3.scale(v3.sub(mx, mn), 0.12);
    return {
      x: [mx[0] + pad[0], o[1], o[2]] as Vec3,
      y: [o[0], mx[1] + pad[1], o[2]] as Vec3,
      z: [o[0], o[1], mx[2] + pad[2]] as Vec3,
    };
  }

  // ---------------------------------------------------------------------------
  // Per-frame overlays computed on the CPU

  private updateNormals(t: number) {
    const list = new VertexList();
    const surface = this.surface;
    if (surface && this.options.normals) {
      const n = 22;
      const s = newSample();
      const color = rgba(COLORS.accent, 0.75);
      const len = this.radius * 0.07;
      const [a0, a1] = surface.aRange;
      const [b0, b1] = surface.bRange;
      for (let i = 0; i <= n; i++) {
        for (let j = 0; j <= n; j++) {
          const a = a0 + ((a1 - a0) * i) / n;
          const b = b0 + ((b1 - b0) * j) / n;
          if (!surface.evaluate(a, b, t, s) || !this.isVisibleAt(a, b, t, s)) continue;
          const nw = this.worldNormal(s);
          if (!nw) continue;
          const pw = this.toWorld(s.p);
          list.add(s.p, color);
          list.add(this.fromWorld(v3.add(pw, v3.scale(nw, len))), color);
        }
      }
    }
    this.upload(this.normalLines, list);
  }

  private updateTangentPlane(t: number) {
    const tris = new VertexList();
    const lines = new VertexList();
    const s = newSample();
    if (this.surface && this.inspected && this.surface.evaluate(this.inspected.a, this.inspected.b, t, s)) {
      const n = this.worldNormal(s);
      const e1 = v3.normalize(v3.mul(s.pa, this.scale));
      if (n && v3.len(e1) > 0) {
        const e2 = v3.cross(n, e1);
        const pw = this.toWorld(s.p);
        const r = this.radius * 0.17;
        const corner = (i: number, j: number) => this.fromWorld(v3.add(pw, v3.add(v3.scale(e1, i * r), v3.scale(e2, j * r))));
        const c = [corner(-1, -1), corner(1, -1), corner(1, 1), corner(-1, 1)];
        const fill = rgba(COLORS.accent, 0.2);
        for (const k of [0, 1, 2, 0, 2, 3]) tris.add(c[k], fill);
        const edge = rgba(COLORS.accent, 0.85);
        for (let k = 0; k < 4; k++) {
          lines.add(c[k], edge);
          lines.add(c[(k + 1) % 4], edge);
        }
      }
    }
    this.upload(this.planeTris, tris);
    this.upload(this.planeLines, lines);
  }

  /** Unit normal in world space (accounts for the z scale), or null where it is undefined. */
  worldNormal(s: SurfaceSample): V3 | null {
    const n = v3.cross(v3.mul(s.pa, this.scale), v3.mul(s.pb, this.scale));
    const l = v3.len(n);
    return l > 1e-20 && Number.isFinite(l) ? v3.scale(n, 1 / l) : null;
  }

  isVisible(p: Vec3): boolean {
    const f = this.frame;
    if (!f) return false;
    for (let k = 0; k < 3; k++) if (!(p[k] >= f.clipMin[k] && p[k] <= f.clipMax[k])) return false;
    return true;
  }

  /** Same test as the vertex shader: inside the clip box and not next to a pole. */
  isVisibleAt(a: number, b: number, t: number, s: SurfaceSample, resolution = this.resolution): boolean {
    if (!this.isVisible(s.p)) return false;
    if (this.maxJump <= 0 || !this.surface) return true;
    const [h, k] = this.cellSize(resolution);
    return !poleNearby(this.surface, a, b, t, s, h, k, this.maxJump);
  }

  cellSize(resolution: number): [number, number] {
    const s = this.surface;
    if (!s || resolution <= 0) return [0, 0];
    return [(s.aRange[1] - s.aRange[0]) / resolution, (s.bRange[1] - s.bRange[0]) / resolution];
  }

  // ---------------------------------------------------------------------------
  // Drawing

  render(t: number) {
    const gl = this.gl;
    const surface = this.surface;
    const frame = this.frame;
    const [pr, pg, pb] = hexToRgb(COLORS.paper);
    gl.viewport(0, 0, this.canvas.width, this.canvas.height);
    gl.clearColor(pr, pg, pb, 1);
    gl.clear(gl.COLOR_BUFFER_BIT | gl.DEPTH_BUFFER_BIT);
    if (!surface || !frame || !this.surfaceProgram) return;

    this.updateNormals(t);
    this.updateTangentPlane(t);

    const cam = this.camera;
    cam.sceneRadius = this.radius;
    const viewProj = cam.viewProjection(this.aspect);
    const { right, up, forward } = cam.basis();
    const keyDir = v3.normalize(v3.add(v3.add(v3.scale(right, -0.45), v3.scale(up, 0.75)), v3.scale(forward, -0.5)));
    const fillDir = v3.normalize(v3.add(v3.add(v3.scale(right, 0.8), v3.scale(up, -0.1)), v3.scale(forward, -0.4)));

    gl.enable(gl.DEPTH_TEST);
    gl.depthFunc(gl.LEQUAL);
    gl.enable(gl.BLEND);
    gl.blendFunc(gl.SRC_ALPHA, gl.ONE_MINUS_SRC_ALPHA);

    // Lines first (grid under the floor heatmap, axes inside the surface).
    this.drawLines(this.guides, gl.LINES, viewProj);

    const sp = this.surfaceProgram;
    const u = sp.uniforms;
    gl.useProgram(sp.program);
    gl.uniform4f(u.uRange, surface.aRange[0], surface.aRange[1], surface.bRange[0], surface.bRange[1]);
    gl.uniform1f(u.uT, t);
    gl.uniform3fv(u.uCenter, this.center);
    gl.uniform3fv(u.uScale, this.scale);
    gl.uniform3fv(u.uClipMin, frame.clipMin);
    gl.uniform3fv(u.uClipMax, frame.clipMax);
    gl.uniformMatrix4fv(u.uViewProj, false, viewProj);
    gl.uniform1f(u.uFloorZ, this.floorZ);
    gl.uniform2fv(u.uCell, this.cellSize(this.resolution));
    gl.uniform1f(u.uMaxJump, this.maxJump);
    gl.uniform3fv(u.uEye, cam.eye);
    gl.uniform3fv(u.uKeyDir, keyDir);
    gl.uniform3fv(u.uFillDir, fillDir);
    gl.uniform2f(u.uHeightRange, frame.boxMin[2], frame.boxMax[2]);
    gl.uniform1f(u.uContourStep, this.contourStep);
    gl.uniform3fv(u.uInk, hexToRgb(COLORS.ink));
    gl.uniform3fv(u.uPaper, hexToRgb(COLORS.paper));
    gl.bindVertexArray(this.gridVao);

    const drawTris = (floor: boolean) => {
      gl.uniform1f(u.uFloor, floor ? 1 : 0);
      gl.uniform1f(u.uWire, 0);
      gl.uniform1f(u.uContours, this.options.contours ? 1 : 0);
      gl.bindBuffer(gl.ELEMENT_ARRAY_BUFFER, this.triIndex);
      gl.drawElements(gl.TRIANGLES, this.triCount, gl.UNSIGNED_INT, 0);
    };

    if (this.options.floor && surface.mode === 'explicit') drawTris(true);

    if (this.options.surface) {
      gl.enable(gl.POLYGON_OFFSET_FILL);
      gl.polygonOffset(1, 1); // push the fill back so wireframe lines on top don't flicker
      drawTris(false);
      gl.disable(gl.POLYGON_OFFSET_FILL);
    }

    if (this.options.wireframe) {
      gl.uniform1f(u.uFloor, 0);
      gl.uniform1f(u.uWire, 1);
      gl.depthMask(this.options.surface ? false : true);
      gl.bindBuffer(gl.ELEMENT_ARRAY_BUFFER, this.wireIndex);
      gl.drawElements(gl.LINES, this.wireCount, gl.UNSIGNED_INT, 0);
      gl.depthMask(true);
    }
    gl.bindVertexArray(null);

    this.drawLines(this.normalLines, gl.LINES, viewProj);

    // Translucent tangent plane last, without writing depth.
    gl.depthMask(false);
    this.drawLines(this.planeTris, gl.TRIANGLES, viewProj);
    gl.depthMask(true);
    gl.disable(gl.DEPTH_TEST);
    this.drawLines(this.planeLines, gl.LINES, viewProj);
  }

  private drawLines(mesh: DynamicMesh, mode: number, viewProj: Float32Array) {
    if (!mesh.count) return;
    const gl = this.gl;
    const p = this.lineProgram;
    gl.useProgram(p.program);
    gl.uniform3fv(p.uniforms.uCenter, this.center);
    gl.uniform3fv(p.uniforms.uScale, this.scale);
    gl.uniformMatrix4fv(p.uniforms.uViewProj, false, viewProj);
    gl.bindVertexArray(mesh.vao);
    gl.drawArrays(mode, 0, mesh.count);
    gl.bindVertexArray(null);
  }

  /** Rebuild the guides after toggling axes or grid. */
  refreshGuides() {
    if (this.frame) this.buildGuides();
  }

  // ---------------------------------------------------------------------------
  // Picking

  /**
   * Finds the surface point under the cursor by casting a ray against a CPU copy of the mesh
   * (Möller–Trumbore ray/triangle test). Returns surface parameters, or null for a miss.
   */
  pick(ndcX: number, ndcY: number, t: number): { a: number; b: number } | null {
    const surface = this.surface;
    if (!surface) return null;
    const ray = this.camera.ray(ndcX, ndcY, this.aspect);
    if (!ray) return null;
    const n = 110;
    const [a0, a1] = surface.aRange;
    const [b0, b1] = surface.bRange;
    const pos: (V3 | null)[] = [];
    const s = newSample();
    for (let j = 0; j <= n; j++) {
      for (let i = 0; i <= n; i++) {
        const a = a0 + ((a1 - a0) * i) / n;
        const b = b0 + ((b1 - b0) * j) / n;
        const ok = surface.evaluate(a, b, t, s) && this.isVisibleAt(a, b, t, s, n);
        pos.push(ok ? this.toWorld(s.p) : null);
      }
    }
    let best = Infinity;
    let hit: { a: number; b: number } | null = null;
    const at = (i: number, j: number) => pos[j * (n + 1) + i];
    for (let j = 0; j < n; j++) {
      for (let i = 0; i < n; i++) {
        const tris: [number, number][][] = [
          [[i, j], [i + 1, j], [i + 1, j + 1]],
          [[i, j], [i + 1, j + 1], [i, j + 1]],
        ];
        for (const tri of tris) {
          const p0 = at(...tri[0]);
          const p1 = at(...tri[1]);
          const p2 = at(...tri[2]);
          if (!p0 || !p1 || !p2) continue;
          const r = rayTriangle(ray.origin, ray.dir, p0, p1, p2);
          if (r && r.t < best) {
            best = r.t;
            const w0 = 1 - r.u - r.v;
            const gi = w0 * tri[0][0] + r.u * tri[1][0] + r.v * tri[2][0];
            const gj = w0 * tri[0][1] + r.u * tri[1][1] + r.v * tri[2][1];
            hit = { a: a0 + ((a1 - a0) * gi) / n, b: b0 + ((b1 - b0) * gj) / n };
          }
        }
      }
    }
    return hit;
  }
}

function rayTriangle(o: V3, d: V3, p0: V3, p1: V3, p2: V3): { t: number; u: number; v: number } | null {
  const e1 = v3.sub(p1, p0);
  const e2 = v3.sub(p2, p0);
  const h = v3.cross(d, e2);
  const det = v3.dot(e1, h);
  if (Math.abs(det) < 1e-12) return null;
  const inv = 1 / det;
  const s = v3.sub(o, p0);
  const u = inv * v3.dot(s, h);
  if (u < 0 || u > 1) return null;
  const q = v3.cross(s, e1);
  const v = inv * v3.dot(d, q);
  if (v < 0 || u + v > 1) return null;
  const t = inv * v3.dot(e2, q);
  return t > 0 ? { t, u, v } : null;
}

/** Rounds to 1, 2 or 5 times a power of ten (nice grid and contour spacing). */
export function niceStep(raw: number): number {
  if (!(raw > 0) || !Number.isFinite(raw)) return 1;
  const p = Math.pow(10, Math.floor(Math.log10(raw)));
  const f = raw / p;
  return (f < 1.5 ? 1 : f < 3.5 ? 2 : f < 7.5 ? 5 : 10) * p;
}

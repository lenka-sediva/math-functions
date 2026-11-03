import { invert, lookAt, multiply, ortho, perspective, v3, type Mat4, type V3 } from './mat4';

/**
 * Orbit camera in spherical coordinates around a target point, with z pointing up
 * (the usual convention for z = f(x, y) plots).
 */
export class OrbitCamera {
  target: V3 = [0, 0, 0];
  distance = 10;
  /** Angle around the z axis, radians. */
  azimuth = -Math.PI * 0.3;
  /** Angle above the xy plane, radians. */
  elevation = 0.5;
  orthographic = false;
  readonly fovY = (40 * Math.PI) / 180;
  /** Radius of the scene, used for near/far planes. */
  sceneRadius = 5;

  get eye(): V3 {
    const ce = Math.cos(this.elevation);
    return v3.add(this.target, [
      this.distance * ce * Math.cos(this.azimuth),
      this.distance * ce * Math.sin(this.azimuth),
      this.distance * Math.sin(this.elevation),
    ]);
  }

  /** Places the camera so a sphere of the given radius around center fits the view. */
  fit(center: V3, radius: number) {
    this.target = [...center];
    this.sceneRadius = radius;
    this.distance = (radius / Math.sin(this.fovY / 2)) * 1.05;
  }

  orbit(dAzimuth: number, dElevation: number) {
    this.azimuth += dAzimuth;
    const limit = Math.PI / 2 - 0.001;
    this.elevation = Math.max(-limit, Math.min(limit, this.elevation + dElevation));
  }

  zoom(factor: number) {
    const r = this.sceneRadius;
    this.distance = Math.max(r * 0.05, Math.min(r * 40, this.distance * factor));
  }

  /** Moves the target in the screen plane; dx, dy are fractions of the viewport height. */
  pan(dx: number, dy: number) {
    const { right, up } = this.basis();
    const h = 2 * this.distance * Math.tan(this.fovY / 2);
    this.target = v3.add(this.target, v3.add(v3.scale(right, -dx * h), v3.scale(up, dy * h)));
  }

  basis() {
    const forward = v3.normalize(v3.sub(this.target, this.eye));
    const right = v3.normalize(v3.cross(forward, [0, 0, 1]));
    const up = v3.cross(right, forward);
    return { forward, right, up };
  }

  view(): Mat4 {
    return lookAt(this.eye, this.target, [0, 0, 1]);
  }

  /** Shifts the picture up by this fraction of the viewport, leaving room for the caption. */
  screenOffsetY = 0.07;

  projection(aspect: number): Mat4 {
    const near = Math.max(this.distance - this.sceneRadius * 3, this.distance * 0.01);
    const far = this.distance + this.sceneRadius * 4;
    let m: Mat4;
    if (this.orthographic) {
      const h = this.distance * Math.tan(this.fovY / 2);
      m = ortho(-h * aspect, h * aspect, -h, h, -far, far);
    } else {
      m = perspective(this.fovY, aspect, near, far);
    }
    // Translate in clip space: y' = y + offset * w (works for both projections).
    const off = 2 * this.screenOffsetY;
    for (let c = 0; c < 4; c++) m[c * 4 + 1] += off * m[c * 4 + 3];
    return m;
  }

  viewProjection(aspect: number): Mat4 {
    return multiply(this.projection(aspect), this.view());
  }

  /** World-space ray through a point in normalized device coordinates (-1..1). */
  ray(ndcX: number, ndcY: number, aspect: number): { origin: V3; dir: V3 } | null {
    const inv = invert(this.viewProjection(aspect));
    if (!inv) return null;
    const unproject = (z: number): V3 => {
      const x = inv[0] * ndcX + inv[4] * ndcY + inv[8] * z + inv[12];
      const y = inv[1] * ndcX + inv[5] * ndcY + inv[9] * z + inv[13];
      const zz = inv[2] * ndcX + inv[6] * ndcY + inv[10] * z + inv[14];
      const w = inv[3] * ndcX + inv[7] * ndcY + inv[11] * z + inv[15];
      return [x / w, y / w, zz / w];
    };
    const a = unproject(-1);
    const b = unproject(1);
    return { origin: a, dir: v3.normalize(v3.sub(b, a)) };
  }
}

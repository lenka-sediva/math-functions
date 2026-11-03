import { GLSL_HELPERS } from '../math/codegen';

/**
 * The surface vertex shader is generated per formula: the formula and its exact partial
 * derivatives are compiled to GLSL and pasted in. The mesh itself is just a flat grid of
 * (s, r) in [0, 1]²; the GPU computes position and normal for every vertex, every frame.
 * That's why animation with t costs nothing on the CPU.
 */
export function surfaceVertexShader(surfaceGLSL: string): string {
  return /* glsl */ `#version 300 es
precision highp float;

in vec2 aST;

uniform vec4 uRange;      // a0, a1, b0, b1
uniform float uT;         // time
uniform vec3 uCenter;     // centre of the bounding box (math coordinates)
uniform vec3 uScale;      // display scale per axis (z can be squashed)
uniform vec3 uClipMin;    // outlier bounds; vertices outside are hidden
uniform vec3 uClipMax;
uniform mat4 uViewProj;
uniform float uFloor;     // 1 = draw the flattened heatmap copy under the surface
uniform float uFloorZ;    // world z of the floor
uniform vec2 uCell;       // size of one grid cell in parameter units
uniform float uMaxJump;   // explicit mode: a height change this big across one cell may be a pole

out vec3 vWorld;
out vec3 vNormal;
out float vHeight;        // math z, used for colours and contour lines
out float vValid;

${GLSL_HELPERS}

${surfaceGLSL}

bool jumpsAgainstSlope(float z, float zn, float slopeStep) {
  float d = zn - z;
  return abs(d) > uMaxJump && d * slopeStep < 0.0;
}

bool poleNearby(float a, float b, float z, float za, float zb) {
  float h = uCell.x;
  float k = uCell.y;
  return jumpsAgainstSlope(z, surfP(a + h, b, uT).z, za * h)
      || jumpsAgainstSlope(z, surfP(a - h, b, uT).z, -za * h)
      || jumpsAgainstSlope(z, surfP(a, b + k, uT).z, zb * k)
      || jumpsAgainstSlope(z, surfP(a, b - k, uT).z, -zb * k);
}

void main() {
  float a = mix(uRange.x, uRange.y, aST.x);
  float b = mix(uRange.z, uRange.w, aST.y);

  vec3 p = surfP(a, b, uT);
  vec3 pa = surfPa(a, b, uT);
  vec3 pb = surfPb(a, b, uT);

  // NaN fails every comparison, so undefined points (sqrt(-1), 0/0) are rejected here too.
  bool ok = all(greaterThanEqual(p, uClipMin)) && all(lessThanEqual(p, uClipMax));
  // Next to a pole (tan, 1/x) the mesh would draw a false "wall" from +∞ to -∞.
  // Detect it: a big jump to a neighbouring vertex that goes against the local slope.
  if (ok && uMaxJump > 0.0 && poleNearby(a, b, p.z, pa.z, pb.z)) ok = false;
  vValid = ok ? 1.0 : 0.0;
  if (!ok) p = uCenter;

  // Exact normal from the symbolic partial derivatives. Tangents are scaled like the
  // geometry, so the normal stays correct when z is squashed.
  vec3 n = cross(pa * uScale, pb * uScale);
  float len = length(n);
  vNormal = (len > 1e-20 && len < 1e30) ? n / len : vec3(0.0, 0.0, 1.0);

  vHeight = p.z;
  vec3 w = (p - uCenter) * uScale;
  if (uFloor > 0.5) w.z = uFloorZ;
  vWorld = w;
  gl_Position = uViewProj * vec4(w, 1.0);
}
`;
}

export const SURFACE_FRAGMENT = /* glsl */ `#version 300 es
precision highp float;

in vec3 vWorld;
in vec3 vNormal;
in float vHeight;
in float vValid;

uniform vec3 uEye;
uniform vec3 uKeyDir;     // light directions, attached to the camera
uniform vec3 uFillDir;
uniform vec2 uHeightRange;
uniform float uContourStep;
uniform float uContours;  // 0/1
uniform float uFloor;     // 0/1
uniform float uWire;      // 0/1: draw as wireframe lines
uniform vec3 uInk;
uniform vec3 uPaper;

out vec4 outColor;

// Colour map from deep indigo through teal and lime to vermilion.
vec3 colormap(float t) {
  const vec3 c0 = vec3(0.153, 0.200, 0.420);
  const vec3 c1 = vec3(0.184, 0.435, 0.690);
  const vec3 c2 = vec3(0.227, 0.639, 0.608);
  const vec3 c3 = vec3(0.725, 0.827, 0.424);
  const vec3 c4 = vec3(0.941, 0.722, 0.290);
  const vec3 c5 = vec3(0.878, 0.376, 0.247);
  t = clamp(t, 0.0, 1.0) * 5.0;
  if (t < 1.0) return mix(c0, c1, t);
  if (t < 2.0) return mix(c1, c2, t - 1.0);
  if (t < 3.0) return mix(c2, c3, t - 2.0);
  if (t < 4.0) return mix(c3, c4, t - 3.0);
  return mix(c4, c5, t - 4.0);
}

void main() {
  // A triangle touching an undefined vertex has vValid < 1 everywhere inside it.
  if (vValid < 0.999) discard;

  if (uWire > 0.5) {
    outColor = vec4(uInk, 0.28);
    return;
  }

  float h = (vHeight - uHeightRange.x) / max(uHeightRange.y - uHeightRange.x, 1e-9);
  vec3 base = colormap(h);
  vec3 col;

  if (uFloor > 0.5) {
    col = mix(base, uPaper, 0.35);
  } else {
    vec3 N = normalize(vNormal);
    vec3 V = normalize(uEye - vWorld);
    bool back = dot(N, V) < 0.0;
    if (back) N = -N; // light both sides (parametric surfaces show their inside)

    float key = max(dot(N, uKeyDir), 0.0);
    float fill = max(dot(N, uFillDir), 0.0);
    vec3 H = normalize(uKeyDir + V);
    float spec = pow(max(dot(N, H), 0.0), 48.0);

    vec3 warm = vec3(1.0, 0.97, 0.92);
    vec3 cool = vec3(0.80, 0.87, 1.0);
    col = base * (0.38 + 0.58 * key * warm + 0.26 * fill * cool) + 0.22 * spec * warm;
    if (back) col *= 0.88;
  }

  if (uContours > 0.5) {
    // Anti-aliased lines at every multiple of the contour step, ~1 px wide on screen.
    float c = vHeight / uContourStep;
    float w = max(fwidth(c), 1e-6);
    float d = abs(fract(c - 0.5) - 0.5) / w;
    float line = 1.0 - smoothstep(0.5, 1.5, d);
    // Where the surface is almost flat (e.g. the tails of exp(-x²) near a level), the level is
    // "nearly crossed" over a wide area and a 1 px line would smear into a band. Fade those out.
    line *= smoothstep(0.0004, 0.002, w);
    col = mix(col, uInk, line * (uFloor > 0.5 ? 0.32 : 0.42));
  }

  outColor = vec4(col, 1.0);
}
`;

/** Lines and translucent triangles (axes, grid, normals, tangent plane). Positions in math coordinates. */
export const LINE_VERTEX = /* glsl */ `#version 300 es
precision highp float;
in vec3 aPos;
in vec4 aColor;
uniform vec3 uCenter;
uniform vec3 uScale;
uniform mat4 uViewProj;
out vec4 vColor;
void main() {
  vColor = aColor;
  gl_Position = uViewProj * vec4((aPos - uCenter) * uScale, 1.0);
}
`;

export const LINE_FRAGMENT = /* glsl */ `#version 300 es
precision highp float;
in vec4 vColor;
out vec4 outColor;
void main() { outColor = vColor; }
`;

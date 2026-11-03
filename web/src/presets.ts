import type { Mode } from './math/surface';

export interface Preset {
  name: string;
  mode: Mode;
  f?: string;
  px?: string;
  py?: string;
  pz?: string;
  a: [string, string];
  b: [string, string];
}

const XY: [string, string] = ['-6', '6'];

/** The ten presets of the desktop app, plus parametric shapes that only the web version can draw. */
export const PRESETS: Preset[] = [
  { name: 'Sinc', mode: 'explicit', f: 'sin(sqrt(x*x+y*y)) / (sqrt(x*x+y*y)+0.01)', a: XY, b: XY },
  { name: 'Saddle', mode: 'explicit', f: 'x*x - y*y', a: XY, b: XY },
  { name: 'Egg crate', mode: 'explicit', f: 'sin(x) * cos(y)', a: XY, b: XY },
  { name: 'Cone', mode: 'explicit', f: 'sqrt(x*x+y*y)', a: XY, b: XY },
  { name: 'Ripple', mode: 'explicit', f: 'sin(x*x+y*y)', a: ['-3', '3'], b: ['-3', '3'] },
  { name: 'Mexican hat', mode: 'explicit', f: '(1-(x*x+y*y)/4)*exp(-(x*x+y*y)/4)', a: XY, b: XY },
  { name: 'Travelling wave', mode: 'explicit', f: 'sin(x+t)*cos(y+t)', a: XY, b: XY },
  { name: 'Manta', mode: 'explicit', f: 'sin(x)+cos(y)+sin(x*y)', a: ['-3', '3'], b: ['-3', '3'] },
  { name: 'Spiral', mode: 'explicit', f: 'sin(atan2(y,x)*3+sqrt(x*x+y*y)-t*2)', a: XY, b: XY },
  { name: 'Star', mode: 'explicit', f: 'sqrt(x*x+y*y)+3*cos(sqrt(x*x+y*y))-3.9', a: XY, b: XY },
  { name: 'Two hills and a pit', mode: 'explicit', f: '3exp(-((x-1.5)^2+y^2)) + 2exp(-((x+1.5)^2+(y-1)^2)) - 2.5exp(-(x^2+(y+1.5)^2))', a: ['-4', '4'], b: ['-4', '4'] },
  { name: 'Monkey saddle', mode: 'explicit', f: 'x^3 - 3x*y^2', a: ['-2', '2'], b: ['-2', '2'] },

  { name: 'Torus', mode: 'parametric', px: '(2 + 0.7cos(v)) * cos(u)', py: '(2 + 0.7cos(v)) * sin(u)', pz: '0.7sin(v)', a: ['0', '2pi'], b: ['0', '2pi'] },
  { name: 'Breathing torus', mode: 'parametric', px: '(2 + (0.6 + 0.3sin(t))cos(v)) * cos(u)', py: '(2 + (0.6 + 0.3sin(t))cos(v)) * sin(u)', pz: '(0.6 + 0.3sin(t))sin(v)', a: ['0', '2pi'], b: ['0', '2pi'] },
  { name: 'Sphere', mode: 'parametric', px: 'cos(u) * sin(v)', py: 'sin(u) * sin(v)', pz: 'cos(v)', a: ['0', '2pi'], b: ['0', 'pi'] },
  { name: 'Möbius strip', mode: 'parametric', px: '(1 + v/2 * cos(u/2)) * cos(u)', py: '(1 + v/2 * cos(u/2)) * sin(u)', pz: 'v/2 * sin(u/2)', a: ['0', '2pi'], b: ['-1', '1'] },
  { name: 'Klein bottle', mode: 'parametric', px: '(2 + cos(u/2)sin(v) - sin(u/2)sin(2v)) * cos(u)', py: '(2 + cos(u/2)sin(v) - sin(u/2)sin(2v)) * sin(u)', pz: 'sin(u/2)sin(v) + cos(u/2)sin(2v)', a: ['0', '2pi'], b: ['0', '2pi'] },
  { name: 'Helicoid', mode: 'parametric', px: 'v * cos(u)', py: 'v * sin(u)', pz: '0.4u', a: ['-2pi', '2pi'], b: ['-2', '2'] },
  { name: 'Seashell', mode: 'parametric', px: '2(1 - exp(u/(6pi))) * cos(u) * cos(v/2)^2', py: '2(-1 + exp(u/(6pi))) * sin(u) * cos(v/2)^2', pz: '1 - exp(u/(3pi)) - sin(v) + exp(u/(6pi))sin(v)', a: ['0', '6pi'], b: ['0', '2pi'] },
];

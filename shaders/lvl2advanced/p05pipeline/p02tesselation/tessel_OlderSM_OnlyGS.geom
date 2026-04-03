#version 150

//layout(lines) in;
layout(triangles) in;
layout(triangle_strip, max_vertices = 100) out;


in vec3 vsColor[];
out vec3 gsColor;

void emit (vec4 v){
 float off=0.02;
	gl_Position = v + vec4(off, -off, 0.0, 0.0);
    EmitVertex();

	gl_Position = v + vec4(-off, -off, 0.0, 0.0);
    EmitVertex();

	gl_Position = v + vec4(off, off, 0.0, 0.0);
    EmitVertex();

	gl_Position = v + vec4(-off, off, 0.0, 0.0);
    EmitVertex();

	EndPrimitive();
}

void main() {

	gsColor = vsColor[0];
	emit(gl_in[0].gl_Position);
	gsColor = vsColor[1];
	emit(gl_in[1].gl_Position);
	gsColor = vsColor[2];
	emit(gl_in[2].gl_Position);

}

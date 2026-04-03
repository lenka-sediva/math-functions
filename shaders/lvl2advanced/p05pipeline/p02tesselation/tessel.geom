#version 440

//layout(lines) in;
layout(triangles) in;
layout(triangle_strip, max_vertices = 100) out;

layout(location = 1) in vec3 inColor[];

layout(location = 1) out vec3 outColor;
layout(location = 2) out flat int elementID; 

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

	outColor = inColor[0];
	emit(gl_in[0].gl_Position);
	outColor = inColor[1];
	emit(gl_in[1].gl_Position);
	outColor = inColor[2];
	emit(gl_in[2].gl_Position);
	
	elementID = gl_InvocationID;
}

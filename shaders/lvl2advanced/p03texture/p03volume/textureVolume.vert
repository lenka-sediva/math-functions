#version 330
in vec3 inPosition;
in vec3 inNormal;
out vec3 vertColor;
out vec3 vertPosition;
uniform mat4 mat;
uniform float scale;
void main() {
	float k = (sin(scale * 2 * 3.14) + 1.0)/4.0 + 0.5; 
	vertPosition = k*(inPosition.xyz-0.5)+0.5;
	gl_Position = mat * vec4(k*inPosition, 1.0);
	vertColor = inNormal * 0.5 + 0.5;
} 

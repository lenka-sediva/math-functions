#version 330
in vec3 inPosition;
in vec3 inNormal;
out vec3 vertColor;
out vec2 texCoord;
uniform mat4 mat;
void main() {
	gl_Position = mat * vec4(inPosition, 1.0);
	vertColor = inNormal * 0.5 + 0.5;
	int aux = int(dot(abs(inNormal) * vec3(0, 1, 2), vec3(1, 1, 1)));
	texCoord = vec2(inPosition[(aux + 1) % 3], inPosition[(aux + 2) % 3]);
} 

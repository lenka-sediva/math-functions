#version 150
in vec3 inPosition;
in vec3 inNormal;
in vec2 inTextureCoordinates;
out vec3 vertColor;
out vec2 texCoord;
uniform mat4 mat;
void main() {
	gl_Position = mat * vec4(inPosition, 1.0);
	vertColor = inNormal * 0.5 + 0.5;
	texCoord = inTextureCoordinates;
} 

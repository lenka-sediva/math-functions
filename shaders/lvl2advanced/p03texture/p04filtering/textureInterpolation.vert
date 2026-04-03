#version 330
in vec3 inPosition;
in vec3 inNormal;
in vec2 inTextureCoordinates;
out vec3 vertColor;
out vec2 texCoord;
smooth out vec2 texCoord1;
flat out vec2 texCoord2;
noperspective out vec2 texCoord3;
uniform mat4 mat;
void main() {
	gl_Position = mat * vec4(inPosition, 1.0);
	vertColor = inNormal * 0.5 + 0.5;
	texCoord = inTextureCoordinates;
	texCoord1 = inTextureCoordinates;
	texCoord2 = inTextureCoordinates;
	texCoord3 = inTextureCoordinates;
} 

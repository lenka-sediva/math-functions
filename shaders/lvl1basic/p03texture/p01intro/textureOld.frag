#version 150
in vec3 vertColor;
in vec2 texCoord;
out vec4 outColor;
uniform sampler2D textureID;
void main() {
	outColor = texture2D(textureID, texCoord); 
} 

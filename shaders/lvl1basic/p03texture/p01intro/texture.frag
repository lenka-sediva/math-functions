#version 330
in vec3 vertColor;
in vec2 texCoord;
out vec4 outColor;
uniform sampler2D textureID;
void main() {
	outColor = texture(textureID, texCoord); 
} 

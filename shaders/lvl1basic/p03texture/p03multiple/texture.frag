#version 330
in vec3 vertColor;
in vec2 texCoord;
out vec4 outColor;
uniform float height;
uniform sampler2D textureID1;
uniform sampler2D textureID2;
void main() {
	if (gl_FragCoord.y<height/2)
		outColor = texture(textureID1, texCoord); 
	else
		outColor = texture(textureID2, texCoord); 
} 

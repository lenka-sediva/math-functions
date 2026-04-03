#version 330
in vec2 texCoord;
out vec4 outColor;
uniform sampler2D textureID;
void main() {
	outColor = texture(textureID, texCoord); 
	outColor = texture(textureID, fract(texCoord+2.0/512.0)); 
	//outColor = vec4(texCoord, 1.0,1.0); 
	//outColor.r = 1.0; 
} 

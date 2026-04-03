#version 330
in vec3 vertColor;
in vec2 texCoord;
out vec4 outColor;
uniform sampler2D textureID;
void main() {
	outColor = texture(textureID, texCoord); 
	//outColor = texture(textureID, vec2(3*texCoord.x, 3*texCoord.y )); 
	//outColor = texture(textureID, vec2(3*texCoord.x-floor(3*texCoord.x),3*texCoord.y-floor(3*texCoord.y)));
	//outColor.r = 1.0; 
} 
#version 330
in vec3 vertColor;
in vec2 texCoord;
out vec4 outColor;
uniform sampler2D textureID;
void main() {
	outColor = texture(textureID, texCoord); 
	//if (length(texCoord.y)<0.1) //x axis
	//	outColor = vec4(1.0, 0.0, 0.0, 1.0); 
	//if (length(texCoord.x)<0.1) //y axis 
	//	outColor = vec4(0.0, 1.0, 0.0, 1.0); 
	if (length(texCoord)<0.3) 
		outColor = vec4(1.0); 
	if (length(texCoord)<0.2){
		outColor = texture(textureID, vec2(0.0, 0.0)); 
	} 
} 

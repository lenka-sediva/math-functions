#version 330
in vec3 vertColor;
in vec2 texCoord;
layout (location=0) out vec4 outColor0;
layout (location=1) out vec4 outColor1;
uniform sampler2D textureID;
void main() {
	vec3 outColor; 
	//outColor = texture(textureID, vec2(3*texCoord.x, 3*texCoord.y )); 
	//outColor = texture(textureID, vec2(3*texCoord.x-floor(3*texCoord.x),3*texCoord.y-floor(3*texCoord.y)));
	
	outColor = texture(textureID, texCoord).rgb;
	outColor.r = 1.0; 
	outColor0 = vec4(outColor,1.0);
	
	outColor = texture(textureID, texCoord).rgb;
	outColor.g = 1.0; 
	
	outColor1 = vec4(outColor,1.0);
	
//	outColor = texture(textureID, texCoord).rgb; 
//	outColor.r = 1.0; 
//	gl_FragData[0] = vec4(outColor,1.0);
	
//	outColor = texture(textureID, texCoord).rgb; 
//	outColor.g = 1.0; 
//	gl_FragData[1] = vec4(outColor,1.0);
	
} 
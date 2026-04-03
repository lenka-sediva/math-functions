#version 330
in vec3 vertColor;
in vec2 texCoord;
smooth in vec2 texCoord1;
flat in vec2 texCoord2;
noperspective in vec2 texCoord3;

out vec4 outColor;
uniform sampler2D textureID;
uniform int mode;
uniform int level;

void main() {
	vec2 textureCoord;
	
	textureCoord = texCoord ; 
	if (mode == 1)
		textureCoord = texCoord1 ; 
	if (mode == 2)
		textureCoord = texCoord2 ; 
	if (mode == 3)
		textureCoord = texCoord3 ; 
		
	outColor = texture(textureID, textureCoord * 2.0); 
	if (level > 0)
	    outColor = textureLod(textureID, textureCoord, level);
} 

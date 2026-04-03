#version 430
in vec2 inPosition; // vstup z vertex bufferu
in vec2 inTexCoord; // vstup z vertex bufferu
out vec2 outTexCoord; // vystup do FS
void main() {
	gl_Position = vec4(inPosition, 0.0, 1.0); 
	outTexCoord = inTexCoord;
} 

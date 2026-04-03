#version 430
in vec2 inPosition; // vstup z vertex bufferu
in vec3 inColor; // vstup z vertex bufferu
layout(location = 1) out vec3 outColor; // vystup do dalsich casti retezce
void main() {
	vec2 position = inPosition;
	gl_Position = vec4(position, 0.0, 1.0); 
	outColor = inColor;
} 

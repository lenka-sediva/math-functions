#version 150
in vec3 fColor; // vstup z predchozi casti retezce
out vec4 outColor; // vystup z fragment shaderu
void main() {
	outColor = vec4(fColor, 1.0); 
} 

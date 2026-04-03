#version 150
in vec3 gsColor; // vstup z predchozi casti retezce
out vec4 outColor; // vystup z fragment shaderu
void main() {
	//outColor = vec4(1.0); 
	outColor = vec4(gsColor,1.0); 
} 

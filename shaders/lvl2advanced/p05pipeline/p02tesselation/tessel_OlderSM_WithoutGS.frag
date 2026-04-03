#version 150
//layout(location = 1) in vec3 inColor; // vstup z predchozi casti retezce
in vec3 vsColor; // vstup z predchozi casti retezce
out vec4 outColor; // vystup z fragment shaderu
void main() {
	//outColor = vec4(1.0); 
	outColor = vec4(vsColor,1.0); 
} 

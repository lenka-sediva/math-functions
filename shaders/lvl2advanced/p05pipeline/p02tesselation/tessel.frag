#version 440
layout(location = 1) in vec3 inColor; // vstup z predchozi casti retezce
layout(location = 2) in flat int elementID; 
out vec4 outColor; // vystup z fragment shaderu
void main() {
	//elementID = gl_PrimitiveID;
	
	//outColor = vec4(vec3(float(elementID%10000)/10000.),1.0); 
	outColor = vec4(inColor, 1.0); 
} 

#version 150
in vec3 vertColor; // input from the previous pipeline stage
out vec4 outColor; // output from the fragment shader
void main() {
	outColor = vec4(vertColor, 1.0);
	//outColor = vec4(1.0); 
} 



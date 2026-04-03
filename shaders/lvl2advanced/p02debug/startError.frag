#version 150
in vec3 vertColor; // input from the previous pipeline stage

//wrong name of output variable
//out vec4 outColour; // output from the fragment shader
//correct name of output variable
out vec4 outColor; // output from the fragment shader

void main() {
	//wrong name of constructor
	//outColor = vec3(vertColor, 1.0); 
	//correct name of constructor
	outColor = vec4(vertColor, 1.0); 
} 

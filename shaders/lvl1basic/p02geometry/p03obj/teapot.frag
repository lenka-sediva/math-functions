#version 150
in vec3 vertColor; // input from the previous pipeline stage
out vec4 outColor; // output from the fragment shader
void main() {
	//outColor = vec4(vec3(position.x,position.y,position.z),0.0);
 	outColor = vec4(vertColor, 1.0);
	//outColor = vec4(1.0); 
} 



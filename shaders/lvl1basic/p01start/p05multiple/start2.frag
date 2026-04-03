#version 150
in vec3 vertColor; // input from the previous pipeline stage
out vec4 outColor; // output from the fragment shader
void main() {
//	outColor = vec4(vertColor, 1.0); //coloring by set color	
	outColor = vec4(vec3(gl_FragCoord.z), 1.0); //coloring by the depth coordinate
//	outColor = vec4(gl_FragCoord.xy / 1000.0, gl_FragCoord.z, 1.0); // coloring by the pixel position 
} 

#version 150
in vec2 inPosition; // input from the vertex buffer
in vec3 inColor; // input from the vertex buffer
out vec3 vertColor; // output from this shader to the next pipeline stage
uniform float time; // variable constant for all vertices in a single draw
void main() {
	vec3 position = vec3(inPosition, 0);
	position.x += 0.1;
	position.y += cos(position.x + time);
	position.z += sin(position.x + time);
	gl_Position = vec4(position, 1.0); 
	vertColor = inColor;
} 

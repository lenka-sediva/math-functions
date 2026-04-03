#version 150
in vec2 inPosition; // input from the vertex buffer
in vec3 inColor; // input from the vertex buffer
out vec3 vertColor; // output from this shader to the next pipeline stage
void main() {
	vec2 position = inPosition;
	position.x += 0.1;
	gl_Position = vec4(position, 0.0, 1.0); 
	vertColor = inColor;
} 

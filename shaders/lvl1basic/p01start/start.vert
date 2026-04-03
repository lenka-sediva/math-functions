#version 150
in vec2 inPosition; // input from the vertex buffer
void main() {
	vec2 position = inPosition;
	position.x += 0.1;
	gl_Position = vec4(position, 0.0, 1.0); 
} 

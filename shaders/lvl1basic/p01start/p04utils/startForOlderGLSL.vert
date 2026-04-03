#version 120
attribute vec2 inPosition; // input from the vertex buffer
attribute vec3 inColor; // input from the vertex buffer
varying vec3 vertColor; // output from this shader to the next pipleline stage
uniform float time; // variable constant for all vertices in a single draw
void main() {
	vec2 position = inPosition;
	position.x += 0.1;
	position.y += cos(position.x + time);
	gl_Position = vec4(position, 0.0, 1.0); 
	vertColor = inColor;
} 

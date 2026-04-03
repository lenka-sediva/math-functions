#version 330

in vec2 inPosition;
in vec3 inColor;

out vec3 vertColor;

void main() {
	vertColor = vec3(inColor.rgb);
	gl_Position = vec4(inPosition, 0., 1.); 
} 

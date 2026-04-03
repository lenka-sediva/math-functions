#version 400
in vec2 inPosition;
in vec3 inColor;
out vec3 color;

void main() {
	gl_Position = vec4(inPosition.xy + vec2(0.2, 0.0),0.0,1.0); 
	color = vec3(1.0, inColor.gb); 
} 

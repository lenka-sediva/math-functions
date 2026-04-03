#version 450
in vec2 inPosition;
in vec3 inColor;
layout(location = 1) out vec3 outColor;

void main() {
	gl_Position = vec4(inPosition.xy + vec2(0.2, 0.0),0.0,1.0); 
	outColor = vec3(1.0, inColor.gb); 
} 

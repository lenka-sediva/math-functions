#version 400
in vec2 inPosition;
in vec3 inColor;
out outValue {
	vec2 pos;
	float r;
	float g;
	float b;
} outData;

void main() {
	outData.pos.x   = inPosition.x+0.5; 
	outData.pos.y   = inPosition.y; 
	outData.r   = 1.0; 
	outData.g   = inColor.g; 
	outData.b   = inColor.b; 
} 

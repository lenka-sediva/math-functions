#version 450
in vec2 inPosition;
in vec3 inColor;
layout(xfb_buffer = 0) out outValue {
	layout(xfb_offset = 0) vec2 pos;
	layout(xfb_offset = 8) float r;
	layout(xfb_offset = 12) float g;
	layout(xfb_offset = 16) float b;
} outData;

void main() {
	outData.pos.x   = inPosition.x+0.5; 
	outData.pos.y   = inPosition.y; 
	outData.r   = 1.0; 
	outData.g   = inColor.g; 
	outData.b   = inColor.b; 
} 

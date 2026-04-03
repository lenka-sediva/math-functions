#version 430
uniform int mode;

in vec2 inPosition; // vstup z vertex bufferu
in vec2 inTexCoord; // vstup z vertex bufferu
out vec2 outTexCoord; // vystup do FS
out float count;
layout (rgba32f, binding = 0) uniform image2D imageIn;

layout(binding = 1, offset = 4) uniform atomic_uint acVS;
layout(binding = 1, offset = 8) uniform atomic_uint acS;

void main() {
	count = float(atomicCounterIncrement(acVS));
	atomicCounterIncrement(acS);
	//count = float(atomicCounter(acVS));
	 
	gl_Position = vec4(inPosition, 0.0, 1.0); 
	outTexCoord = inTexCoord;
} 

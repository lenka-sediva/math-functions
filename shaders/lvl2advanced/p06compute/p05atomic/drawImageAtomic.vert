#version 430
uniform int mode;
in vec2 inPosition; // vstup z vertex bufferu
in vec2 inTexCoord; // vstup z vertex bufferu
out vec2 outTexCoord; // vystup do FS

layout(binding = 0, offset = 4) uniform atomic_uint acVS;
layout(binding = 0, offset = 8) uniform atomic_uint acS;

void main() {
	atomicCounterIncrement(acVS);
	atomicCounterIncrement(acS);
	memoryBarrierAtomicCounter()
	gl_Position = vec4(inPosition, 0.0, 1.0); 
	outTexCoord = inTexCoord;
} 

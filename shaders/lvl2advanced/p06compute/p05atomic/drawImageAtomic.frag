#version 430
uniform int mode;
uniform int numPrimitives;
uniform int numSamples;
in vec3 vertColor; // vstup z predchozi casti retezce
in vec2 outTexCoord; // vystup do FS
out vec4 outColor; // vystup z fragment shaderu

layout(binding = 0, offset = 0) uniform atomic_uint acFS;
layout(binding = 0, offset = 4) uniform atomic_uint acVS;
layout(binding = 0, offset = 8) uniform atomic_uint acS;

void main() {
	int size;
	atomicCounterIncrement(acFS);
	atomicCounterIncrement(acS);
	
	size = numSamples  / 4 ; //samples per fragment
	float valueR = (int(atomicCounter(acFS))%(size + 1))/float(size + 1);
	
	size = numPrimitives + 1; 
	float valueG = atomicCounter(acVS)%size/float(size);
	
	size = numPrimitives + 1 + numSamples/4 ; //samples per fragment
	float valueB = atomicCounter(acS)%(size+1)/float(size);
	
	//uint counter = atomicCounterIncrement(ac);
	//float value = counter/555/555f;
	memoryBarrierAtomicCounter()
	
	switch(mode%4){
		case 0:	
			outColor = vec4(valueR, valueG, valueB, 1.0);
			break; 
		case 1:	
			outColor = vec4(valueR, 0, 0, 1.0); 
			break; 
		case 2:	
			outColor = vec4(0, valueG, 0, 1.0); 
			break; 
		case 3:	
			outColor = vec4(0, 0, valueB, 1.0); 
			break;
		} 
} 

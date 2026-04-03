#version 430
in vec3 vertColor; // vstup z predchozi casti retezce
in vec2 outTexCoord; // vystup do FS
out vec4 outColor; // vystup z fragment shaderu
layout (rgba32f, binding = 0) uniform image2D imageIn;

void main() {
	ivec2 coord = ivec2( int(outTexCoord.x*256), int(outTexCoord.y*256));
	vec4 tex = imageLoad(imageIn, coord);

	outColor = vec4(tex.rgb, 1.0); 
} 

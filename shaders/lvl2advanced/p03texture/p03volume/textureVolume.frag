#version 330
in vec3 vertColor;
in vec3 vertPosition;
out vec4 outColor;
uniform sampler3D textureVol;
void main() {
//	outColor = vec4(textureLod(textureVol, vertPosition, 0f).rgb,1.0);
	outColor = vec4(texture(textureVol, vertPosition).rgb,1.0);
//	outColor = vec4(1.0);
}

	 

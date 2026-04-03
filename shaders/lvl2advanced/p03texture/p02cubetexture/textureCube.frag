#version 150
in vec3 vertColor;
in vec3 vertPosition;
out vec4 outColor;
uniform samplerCube textureID;
void main() {
	//outColor = vec4(textureLod(textureID, vertPosition,0f).rgb,1.0);
	outColor = vec4(texture(textureID, vertPosition).rgb,1.0);
}

	 

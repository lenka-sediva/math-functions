#version 450
layout(location = 2) in vec3 inColor; // vstup z vertex bufferu
layout(location = 1) in vec2 inPosition; // vstup z vertex bufferu

layout(location = 1) out vec3 vsColor; // vystup do dalsich casti retezce
layout(location = 2) out flat int elementID; 
uniform float time;
void main() {
	vec2 position = inPosition;
	//position.y += 0.1;
	//position.x += cos(position.y + time);
	gl_Position = vec4(position, 0.0, 1.0); 
	vsColor = inColor;
	//vsColor = inColor*(gl_VertexID);
	
	
} 

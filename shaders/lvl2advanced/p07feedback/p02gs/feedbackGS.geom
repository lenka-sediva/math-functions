#version 400
//layout(points, location = 0) in vec2 position[];
layout(points) in;
in vec3 color[];

layout(points, max_vertices = 4) out;


out outValue {
	vec2 pos;
	vec3 col;
} outData;


void main() {
	 float dist = 0.02;
      outData.col = color[0];
      outData.pos = gl_in[0].gl_Position.xy + vec2(-dist,dist);
      EmitVertex();
      
      outData.pos = gl_in[0].gl_Position.xy + vec2(dist,dist);
      EmitVertex();
      
      outData.pos = gl_in[0].gl_Position.xy + vec2(0.0,-dist);
      EmitVertex();
      
      EndPrimitive();
}
#version 450
//layout(points, location = 0) in vec2 position[];
layout(points) in;
layout(location = 1) in vec3 color[];

layout(points, max_vertices = 4) out;


layout(xfb_buffer = 0) out outValue {
	layout(xfb_offset = 0) vec2 pos;
	layout(xfb_offset = 8) vec3 col;
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
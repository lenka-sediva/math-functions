#version 150

layout(lines_adjacency) in;
layout(triangle_strip, max_vertices = 60) out;


in vec3 vColor[];
out vec3 fColor;

void main() {
	vec2 dir1= normalize(gl_in[0].gl_Position.xy-gl_in[2].gl_Position.xy);
	vec2 dir2= normalize(gl_in[3].gl_Position.xy-gl_in[1].gl_Position.xy);
	//vec2 dir3= normalize(gl_in[2].gl_Position.xy-gl_in[3].gl_Position.xy);
	
    //spojujici usecka
    float k=0.04;
	fColor = vColor[1];
	gl_Position = gl_in[1].gl_Position +vec4(k*dir1.y,k*dir1.x,0,0);
    EmitVertex();
	gl_Position = gl_in[1].gl_Position -vec4(k*dir1.y,k*dir1.x,0,0);
    EmitVertex();
    fColor = vColor[2];
	gl_Position = gl_in[2].gl_Position -vec4(k*dir2.y,k*dir2.x,0,0);
    EmitVertex();
    
	EndPrimitive();
    
    fColor = vColor[1];
	gl_Position = gl_in[1].gl_Position +vec4(k*dir1.y,k*dir1.x,0,0);
    EmitVertex();
    fColor = vColor[2];
	gl_Position = gl_in[2].gl_Position -vec4(k*dir2.y,k*dir2.x,0,0);;
    EmitVertex();
	gl_Position = gl_in[2].gl_Position +vec4(k*dir2.y,k*dir2.x,0,0);;
    EmitVertex();
   
	EndPrimitive();

}

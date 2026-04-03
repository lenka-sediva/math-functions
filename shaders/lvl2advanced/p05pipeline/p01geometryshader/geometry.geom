#version 430

//input geometry
layout(lines_adjacency) in;
//output geometry
layout(triangle_strip, max_vertices = 60) out;

//input attribute 
layout(location = 1) in vec3 vColor[];
//output attribute
layout(location = 1) out vec3 fColor;

void main() {
	// input geometry: gl_in[0]..gl_in[3] line adjacency
	
	//directions
	vec2 dir1= normalize(gl_in[1].gl_Position.xy - gl_in[0].gl_Position.xy);
	vec2 dir2= normalize(gl_in[2].gl_Position.xy - gl_in[1].gl_Position.xy);
	vec2 dir3= normalize(gl_in[3].gl_Position.xy - gl_in[2].gl_Position.xy);
	
	//normal vectors
	dir1= normalize(vec2(-1.0, 1.0)*dir1.yx);
	dir2= normalize(vec2(-1.0, 1.0)*dir2.yx);
	dir3= normalize(vec2(-1.0, 1.0)*dir3.yx);
	
	//vectors of junction
	vec2 d1= normalize(dir1 + dir2);
	vec2 d2= normalize(dir2 + dir3);
	 
	//width of new bold line
	float k1 = 0.05/abs(dot(d1,dir2));
	float k2 = 0.05/abs(dot(d2,dir2));

	//first triangle of the first trianglestrip
	fColor = vColor[1];
	gl_Position = gl_in[1].gl_Position + vec4(k1 * d1.xy, .0, .0);
    EmitVertex();
	gl_Position = gl_in[1].gl_Position - vec4(k1 * d1.xy, .0, .0);
    EmitVertex();
    fColor = vColor[2];
	gl_Position = gl_in[2].gl_Position + vec4(k2 * d2.xy, .0, .0);
    EmitVertex();
    
	EndPrimitive();
    
    //first triangle of the second trianglestrip
	fColor = vColor[1];
	gl_Position = gl_in[1].gl_Position - vec4(k1 * d1.xy, .0, .0);
    EmitVertex();
    fColor = vColor[2];
	gl_Position = gl_in[2].gl_Position + vec4(k2 * d2.xy, .0, .0);
    EmitVertex();
	gl_Position = gl_in[2].gl_Position - vec4(k2 * d2.xy, .0, .0);
    EmitVertex();
   
	EndPrimitive();
}

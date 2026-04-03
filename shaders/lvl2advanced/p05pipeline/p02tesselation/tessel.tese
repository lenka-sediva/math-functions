#version 430 core

//layout (triangles, fractional_odd_spacing, ccw) in; 
//layout (triangles, fractional_even_spacing, ccw) in; 
layout (triangles, equal_spacing, ccw) in; 

//layout (quads, equal_spacing, cw) in;
//layout (isolines, equal_spacing, cw) in;

layout(location = 1) in vec3 inColor[];

layout(location = 1) out vec3 outColor;

void main(void){
    gl_Position = (gl_TessCoord.x * gl_in[0].gl_Position +
    gl_TessCoord.y * gl_in[1].gl_Position +
    gl_TessCoord.z * gl_in[2].gl_Position);

    //gl_Position = vec4(gl_TessCoord.x-0.1 , gl_TessCoord.y-0.1, gl_TessCoord.z, 1);

    
    outColor = gl_TessCoord.x * vec3(1.0, 0.0, 0.0) + gl_TessCoord.y * vec3(0.0,1.0,0.0) +
    gl_TessCoord.z * vec3(1.0,1.0,0.0);
    
    outColor = inColor[0];
 	
 	outColor = gl_TessCoord.x * inColor[0] +
    gl_TessCoord.y * inColor[1] +
    gl_TessCoord.z * inColor[2];

  }
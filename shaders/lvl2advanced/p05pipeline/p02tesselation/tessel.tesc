#version 430 core

layout (vertices = 3) out;

layout(location = 1) in vec3 inColor[];
layout(location = 1) out vec3 outColor[];

uniform float time;
void main(void){

    if (gl_InvocationID == 0){
        gl_TessLevelInner[0] = time;
        //gl_TessLevelInner[1] = 2.0;
        gl_TessLevelOuter[0] = 4.0;
        gl_TessLevelOuter[1] = 2.0;
        gl_TessLevelOuter[2] = 3.0;
        //gl_TessLevelOuter[3] = 5.0;
    }
  
   //if (gl_InvocationID == 0)
   //     gl_out[gl_InvocationID].gl_Position = vec4(0,0,0,1);
   //else     
   
   gl_out[gl_InvocationID].gl_Position = gl_in[gl_InvocationID].gl_Position;

   outColor[gl_InvocationID] = inColor[gl_InvocationID];
}